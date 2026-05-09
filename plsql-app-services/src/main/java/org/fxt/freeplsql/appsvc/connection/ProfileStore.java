package org.fxt.freeplsql.appsvc.connection;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Persists the list of {@link ConnectionProfile}s as JSON inside the encrypted
 * blob managed by {@link CryptoStore}. Stateless codec — the in-memory profile
 * list is owned by the caller (AppContext).
 */
public final class ProfileStore {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<List<ConnectionProfile>> LIST_TYPE =
            new TypeReference<>() {};

    private final CryptoStore crypto;

    public ProfileStore(CryptoStore crypto) {
        this.crypto = crypto;
    }

    public boolean exists() {
        return crypto.exists();
    }

    /** Creates a fresh, empty encrypted profile file under the given master password. */
    public void initialize(char[] masterPassword) throws IOException {
        byte[] payload = MAPPER.writeValueAsBytes(List.<ConnectionProfile>of());
        crypto.create(masterPassword, payload);
    }

    /** Decrypts the file with the given master password and returns the profile list. */
    public List<ConnectionProfile> unlock(char[] masterPassword)
            throws IOException, WrongPasswordException {
        byte[] payload = crypto.unlock(masterPassword);
        if (payload.length == 0) {
            return List.of();
        }
        return MAPPER.readValue(new String(payload, StandardCharsets.UTF_8), LIST_TYPE);
    }

    /** Re-encrypts and persists the given list. CryptoStore must be unlocked. */
    public void save(List<ConnectionProfile> profiles) throws IOException {
        byte[] payload = MAPPER.writeValueAsBytes(profiles);
        crypto.write(payload);
    }
}
