package org.fxt.freeplsql.appsvc.connection;

import com.zaxxer.hikari.HikariConfig;

/**
 * Builds the JDBC URL and HikariConfig properties for one specific Oracle
 * authentication mechanism. Stateless — pick the right impl via
 * {@link #forType(AuthType)}.
 */
public sealed interface OracleAuthStrategy
        permits EasyConnectAuth, TnsNamesAuth, WalletAuth, KerberosAuth {

    String jdbcUrl(ConnectionProfile profile);

    /** Sets username/password and any auth-specific data-source properties. */
    void applyConfig(HikariConfig config, ConnectionProfile profile);

    static OracleAuthStrategy forType(AuthType type) {
        return switch (type) {
            case EASY_CONNECT -> new EasyConnectAuth();
            case TNS_NAMES -> new TnsNamesAuth();
            case WALLET -> new WalletAuth();
            case KERBEROS -> new KerberosAuth();
        };
    }
}
