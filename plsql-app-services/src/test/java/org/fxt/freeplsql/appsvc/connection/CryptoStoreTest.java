package org.fxt.freeplsql.appsvc.connection;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CryptoStoreTest {

    private static final byte[] PAYLOAD = "the quick brown fox".getBytes();

    @Test
    void createThenUnlockRoundtrips(@TempDir Path tmp)
            throws IOException, WrongPasswordException {
        var file = tmp.resolve("profiles.enc");
        var store = new CryptoStore(file);
        store.create("hunter2!".toCharArray(), PAYLOAD);

        // Re-open from disk in a fresh instance to prove no in-memory cheating.
        var fresh = new CryptoStore(file);
        byte[] decrypted = fresh.unlock("hunter2!".toCharArray());

        assertArrayEquals(PAYLOAD, decrypted);
        assertTrue(fresh.isUnlocked());
    }

    @Test
    void unlockWithWrongPasswordThrows(@TempDir Path tmp) throws IOException {
        var file = tmp.resolve("profiles.enc");
        new CryptoStore(file).create("right".toCharArray(), PAYLOAD);

        var fresh = new CryptoStore(file);
        assertThrows(WrongPasswordException.class,
                () -> fresh.unlock("wrong".toCharArray()));
        assertFalse(fresh.isUnlocked());
    }

    @Test
    void writeAfterUnlockChangesIvButRoundtrips(@TempDir Path tmp)
            throws IOException, WrongPasswordException {
        var file = tmp.resolve("profiles.enc");
        var store = new CryptoStore(file);
        store.create("pw1234567".toCharArray(), PAYLOAD);

        byte[] firstFile = Files.readAllBytes(file);
        store.write("updated payload".getBytes());
        byte[] secondFile = Files.readAllBytes(file);

        // IV is at offset 1+16=17, length 12 — must differ between writes.
        byte[] iv1 = java.util.Arrays.copyOfRange(firstFile, 17, 29);
        byte[] iv2 = java.util.Arrays.copyOfRange(secondFile, 17, 29);
        assertFalse(java.util.Arrays.equals(iv1, iv2),
                "GCM IVs must never repeat for the same key");

        var fresh = new CryptoStore(file);
        assertArrayEquals("updated payload".getBytes(),
                fresh.unlock("pw1234567".toCharArray()));
    }

    @Test
    void truncatedFileFailsUnlock(@TempDir Path tmp) throws IOException {
        var file = tmp.resolve("profiles.enc");
        new CryptoStore(file).create("pw".toCharArray(), PAYLOAD);

        // Chop the file in half to simulate truncation.
        byte[] full = Files.readAllBytes(file);
        Files.write(file, java.util.Arrays.copyOf(full, full.length / 2));

        var fresh = new CryptoStore(file);
        assertThrows(Exception.class, () -> fresh.unlock("pw".toCharArray()));
    }

    @Test
    void mutatedCipherTriggersWrongPassword(@TempDir Path tmp) throws IOException {
        var file = tmp.resolve("profiles.enc");
        new CryptoStore(file).create("pw".toCharArray(), PAYLOAD);

        // Flip a byte deep inside the ciphertext — GCM auth tag must fail.
        byte[] raw = Files.readAllBytes(file);
        raw[raw.length - 4] ^= (byte) 0xff;
        Files.write(file, raw);

        var fresh = new CryptoStore(file);
        assertThrows(WrongPasswordException.class,
                () -> fresh.unlock("pw".toCharArray()));
    }

    @Test
    void createRefusesToOverwriteExistingFile(@TempDir Path tmp) throws IOException {
        var file = tmp.resolve("profiles.enc");
        new CryptoStore(file).create("pw".toCharArray(), PAYLOAD);

        assertThrows(IOException.class,
                () -> new CryptoStore(file).create("pw".toCharArray(), PAYLOAD));
    }

    @Test
    void writeBeforeUnlockThrows(@TempDir Path tmp) {
        var file = tmp.resolve("profiles.enc");
        var store = new CryptoStore(file);
        assertThrows(IllegalStateException.class, () -> store.write(PAYLOAD));
    }

    @Test
    void unknownVersionByteFailsUnlock(@TempDir Path tmp) throws IOException {
        var file = tmp.resolve("profiles.enc");
        new CryptoStore(file).create("pw".toCharArray(), PAYLOAD);

        byte[] raw = Files.readAllBytes(file);
        raw[0] = 99;
        Files.write(file, raw);

        var fresh = new CryptoStore(file);
        IOException ex = assertThrows(IOException.class,
                () -> fresh.unlock("pw".toCharArray()));
        assertTrue(ex.getMessage().contains("version"));
    }

    @Test
    void saltDiffersBetweenIndependentlyCreatedStores(@TempDir Path tmp) throws IOException {
        var file1 = tmp.resolve("a.enc");
        var file2 = tmp.resolve("b.enc");
        new CryptoStore(file1).create("pw".toCharArray(), PAYLOAD);
        new CryptoStore(file2).create("pw".toCharArray(), PAYLOAD);

        byte[] salt1 = java.util.Arrays.copyOfRange(Files.readAllBytes(file1), 1, 17);
        byte[] salt2 = java.util.Arrays.copyOfRange(Files.readAllBytes(file2), 1, 17);
        assertFalse(java.util.Arrays.equals(salt1, salt2),
                "every fresh store should pick its own salt");

        // Sanity: also yields different ciphertexts even with same payload+pw.
        assertNotEquals(
                java.util.Arrays.toString(Files.readAllBytes(file1)),
                java.util.Arrays.toString(Files.readAllBytes(file2)));
    }
}
