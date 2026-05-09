package org.fxt.freeplsql.appsvc.connection;

import com.zaxxer.hikari.HikariConfig;

public final class EasyConnectAuth implements OracleAuthStrategy {

    @Override
    public String jdbcUrl(ConnectionProfile p) {
        return "jdbc:oracle:thin:@//" + p.host() + ":" + p.port() + "/" + p.service();
    }

    @Override
    public void applyConfig(HikariConfig config, ConnectionProfile p) {
        config.setUsername(p.username());
        config.setPassword(p.password());
    }
}
