package org.fxt.freeplsql.appsvc.connection;

import com.zaxxer.hikari.HikariConfig;

/**
 * Uses the JDBC URL verbatim as supplied by the user — supports full Oracle
 * TNS-style descriptors like
 * {@code jdbc:oracle:thin:@(DESCRIPTION=(ADDRESS=(PROTOCOL=TCP)(HOST=…)(PORT=…))(CONNECT_DATA=(SERVICE_NAME=…)))}.
 * Credentials are applied through Hikari, not encoded into the URL.
 */
public final class CustomUrlAuth implements OracleAuthStrategy {

    @Override
    public String jdbcUrl(ConnectionProfile p) {
        return p.customJdbcUrl();
    }

    @Override
    public void applyConfig(HikariConfig config, ConnectionProfile p) {
        if (p.username() != null && !p.username().isBlank()) {
            config.setUsername(p.username());
        }
        if (p.password() != null && !p.password().isBlank()) {
            config.setPassword(p.password());
        }
    }
}
