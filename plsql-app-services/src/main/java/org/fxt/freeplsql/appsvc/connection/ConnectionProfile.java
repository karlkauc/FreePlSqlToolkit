package org.fxt.freeplsql.appsvc.connection;

/**
 * Persisted connection profile. Stored inside the encrypted profile file —
 * the {@link #password} field is therefore on disk in clear-text inside the
 * AES-GCM ciphertext, not in the JSON layer. Auth-type specific fields are
 * nullable; unused ones serialize as {@code null}.
 */
public record ConnectionProfile(
        String id,
        String name,
        AuthType authType,
        String host,
        Integer port,
        String service,
        String tnsAlias,
        String walletPath,
        String kerberosPrincipal,
        String username,
        String password,
        int poolSize
) {

    public static ConnectionProfile easyConnect(
            String id, String name,
            String host, int port, String service,
            String username, String password, int poolSize) {
        return new ConnectionProfile(id, name, AuthType.EASY_CONNECT,
                host, port, service,
                null, null, null,
                username, password, poolSize);
    }

    public static ConnectionProfile tnsNames(
            String id, String name, String tnsAlias,
            String username, String password, int poolSize) {
        return new ConnectionProfile(id, name, AuthType.TNS_NAMES,
                null, null, null,
                tnsAlias, null, null,
                username, password, poolSize);
    }

    public static ConnectionProfile wallet(
            String id, String name, String tnsAlias, String walletPath,
            String username, String password, int poolSize) {
        return new ConnectionProfile(id, name, AuthType.WALLET,
                null, null, null,
                tnsAlias, walletPath, null,
                username, password, poolSize);
    }

    public static ConnectionProfile kerberos(
            String id, String name,
            String host, int port, String service,
            String kerberosPrincipal, int poolSize) {
        return new ConnectionProfile(id, name, AuthType.KERBEROS,
                host, port, service,
                null, null, kerberosPrincipal,
                null, null, poolSize);
    }
}
