package org.fxt.freeplsql.appsvc.connection;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProfileStoreTest {

    @Test
    void initializeCreatesEmptyEncryptedFile(@TempDir Path tmp)
            throws IOException, WrongPasswordException {
        var store = new ProfileStore(new CryptoStore(tmp.resolve("profiles.enc")));
        store.initialize("pw1234567".toCharArray());

        assertTrue(store.exists());
        assertTrue(new ProfileStore(new CryptoStore(tmp.resolve("profiles.enc")))
                .unlock("pw1234567".toCharArray()).isEmpty());
    }

    @Test
    void saveThenUnlockRoundtripsAllAuthTypes(@TempDir Path tmp)
            throws IOException, WrongPasswordException {
        var path = tmp.resolve("profiles.enc");
        var store = new ProfileStore(new CryptoStore(path));
        store.initialize("master!!!".toCharArray());
        // Re-unlock to set up the cached key for write
        store.unlock("master!!!".toCharArray());

        var profiles = List.of(
                ConnectionProfile.easyConnect("id-1", "Local XE",
                        "localhost", 1521, "XEPDB1", "scott", "tiger", 2),
                ConnectionProfile.tnsNames("id-2", "Prod via TNS",
                        "PROD_RW", "app", "secret", 4),
                ConnectionProfile.wallet("id-3", "Autonomous",
                        "MYDB_HIGH", "/opt/wallets/adb", "appuser", "wpw", 2),
                ConnectionProfile.kerberos("id-4", "Kerberos cluster",
                        "kdc.host", 1521, "ORCL", "svc/host@REALM", 2)
        );
        store.save(profiles);

        var fresh = new ProfileStore(new CryptoStore(path));
        List<ConnectionProfile> roundtrip = fresh.unlock("master!!!".toCharArray());

        assertEquals(profiles, roundtrip);
    }

    @Test
    void unlockWithWrongPasswordThrows(@TempDir Path tmp)
            throws IOException, WrongPasswordException {
        var path = tmp.resolve("profiles.enc");
        var store = new ProfileStore(new CryptoStore(path));
        store.initialize("right".toCharArray());

        assertThrows(WrongPasswordException.class,
                () -> new ProfileStore(new CryptoStore(path))
                        .unlock("wrong".toCharArray()));
    }

    @Test
    void existsReportsCorrectState(@TempDir Path tmp) throws IOException {
        var path = tmp.resolve("profiles.enc");
        var store = new ProfileStore(new CryptoStore(path));
        assertTrue(!store.exists());
        store.initialize("pw".toCharArray());
        assertTrue(store.exists());
    }
}
