package org.fxt.freeplsql.appsvc.connection;

import com.zaxxer.hikari.HikariConfig;

/**
 * Resolves the alias from {@code tnsnames.ora} on {@code $TNS_ADMIN}. The user
 * is responsible for setting that environment variable / system property before
 * the JVM starts; the strategy does not override it.
 */
public final class TnsNamesAuth implements OracleAuthStrategy {

    @Override
    public String jdbcUrl(ConnectionProfile p) {
        return "jdbc:oracle:thin:@" + p.tnsAlias();
    }

    @Override
    public void applyConfig(HikariConfig config, ConnectionProfile p) {
        config.setUsername(p.username());
        config.setPassword(p.password());
    }
}
