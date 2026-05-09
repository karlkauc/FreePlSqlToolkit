package org.fxt.freeplsql.appsvc.connection;

import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Arrays;
import java.util.Objects;

/**
 * Single-file encrypted blob storage.
 *
 * <p>File layout:
 * <pre>
 *   [1B version=1][16B salt][12B IV][ciphertext+16B GCM tag]
 * </pre>
 *
 * <p>Crypto: AES-256/GCM/NoPadding, key derived via PBKDF2-HMAC-SHA256 with
 * 600 000 iterations (OWASP 2023+). Salt is fixed for the lifetime of the file
 * (so the slow KDF runs once per session); IV is fresh per write, never
 * reused, drawn from {@link SecureRandom}.
 *
 * <p>Threading: not safe for concurrent use; serialise externally.
 */
public final class CryptoStore {

    private static final byte VERSION = 1;
    private static final int SALT_BYTES = 16;
    private static final int IV_BYTES = 12;
    private static final int KEY_BITS = 256;
    private static final int GCM_TAG_BITS = 128;
    private static final int PBKDF2_ITERATIONS = 600_000;
    private static final String KDF_ALG = "PBKDF2WithHmacSHA256";
    private static final String CIPHER_ALG = "AES/GCM/NoPadding";

    private static final SecureRandom RNG = new SecureRandom();

    private final Path file;
    private byte[] salt;
    private SecretKey key;

    public CryptoStore(Path file) {
        this.file = Objects.requireNonNull(file);
    }

    public Path file() {
        return file;
    }

    public boolean exists() {
        return Files.exists(file);
    }

    public boolean isUnlocked() {
        return key != null;
    }

    /**
     * First-time setup: writes the file with the given payload encrypted under
     * a key derived from the master password. The caller still owns the
     * {@code masterPassword} array and should zero it afterwards.
     */
    public void create(char[] masterPassword, byte[] payload) throws IOException {
        if (Files.exists(file)) {
            throw new IOException("File already exists: " + file);
        }
        byte[] freshSalt = randomBytes(SALT_BYTES);
        SecretKey derived = deriveKey(masterPassword, freshSalt);
        byte[] iv = randomBytes(IV_BYTES);
        byte[] ciphertext;
        try {
            ciphertext = encrypt(derived, iv, payload);
        } catch (GeneralSecurityException e) {
            throw new IOException("Encryption failed", e);
        }
        writeFile(freshSalt, iv, ciphertext);
        this.salt = freshSalt;
        this.key = derived;
    }

    /**
     * Reads the file, derives the key, and returns the decrypted payload. On
     * success, the key is cached in memory so subsequent {@link #write} calls
     * skip the slow KDF.
     */
    public byte[] unlock(char[] masterPassword) throws IOException, WrongPasswordException {
        byte[] raw = Files.readAllBytes(file);
        int minSize = 1 + SALT_BYTES + IV_BYTES + (GCM_TAG_BITS / 8);
        if (raw.length < minSize) {
            throw new IOException("Profile file truncated (size=" + raw.length + ")");
        }
        if (raw[0] != VERSION) {
            throw new IOException("Unknown profile-file version: " + (raw[0] & 0xff));
        }
        byte[] readSalt = Arrays.copyOfRange(raw, 1, 1 + SALT_BYTES);
        byte[] iv = Arrays.copyOfRange(raw, 1 + SALT_BYTES, 1 + SALT_BYTES + IV_BYTES);
        byte[] ciphertext = Arrays.copyOfRange(raw, 1 + SALT_BYTES + IV_BYTES, raw.length);
        SecretKey derived = deriveKey(masterPassword, readSalt);
        try {
            byte[] plaintext = decrypt(derived, iv, ciphertext);
            this.salt = readSalt;
            this.key = derived;
            return plaintext;
        } catch (AEADBadTagException e) {
            throw new WrongPasswordException();
        } catch (GeneralSecurityException e) {
            throw new IOException("Decryption failed", e);
        }
    }

    /**
     * Re-encrypts and persists the payload using the cached key. A fresh IV is
     * generated to avoid IV-reuse under the same key (which would break GCM).
     */
    public void write(byte[] payload) throws IOException {
        if (key == null || salt == null) {
            throw new IllegalStateException("CryptoStore is locked — call create() or unlock() first");
        }
        byte[] iv = randomBytes(IV_BYTES);
        byte[] ciphertext;
        try {
            ciphertext = encrypt(key, iv, payload);
        } catch (GeneralSecurityException e) {
            throw new IOException("Encryption failed", e);
        }
        writeFile(salt, iv, ciphertext);
    }

    public void lock() {
        this.key = null;
        this.salt = null;
    }

    private void writeFile(byte[] saltBytes, byte[] iv, byte[] ciphertext) throws IOException {
        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Path tmp = file.resolveSibling(file.getFileName().toString() + ".tmp");
        try (OutputStream out = Files.newOutputStream(tmp)) {
            out.write(VERSION);
            out.write(saltBytes);
            out.write(iv);
            out.write(ciphertext);
        }
        try {
            Files.move(tmp, file,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static SecretKey deriveKey(char[] pwd, byte[] salt) {
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance(KDF_ALG);
            KeySpec spec = new PBEKeySpec(pwd, salt, PBKDF2_ITERATIONS, KEY_BITS);
            byte[] keyBytes = factory.generateSecret(spec).getEncoded();
            return new SecretKeySpec(keyBytes, "AES");
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("PBKDF2 key derivation failed", e);
        }
    }

    private static byte[] encrypt(SecretKey key, byte[] iv, byte[] plaintext)
            throws GeneralSecurityException {
        Cipher c = Cipher.getInstance(CIPHER_ALG);
        c.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
        return c.doFinal(plaintext);
    }

    private static byte[] decrypt(SecretKey key, byte[] iv, byte[] ciphertext)
            throws GeneralSecurityException {
        Cipher c = Cipher.getInstance(CIPHER_ALG);
        c.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
        return c.doFinal(ciphertext);
    }

    private static byte[] randomBytes(int n) {
        byte[] b = new byte[n];
        RNG.nextBytes(b);
        return b;
    }
}
