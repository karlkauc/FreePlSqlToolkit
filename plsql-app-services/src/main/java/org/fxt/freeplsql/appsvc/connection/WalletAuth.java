package org.fxt.freeplsql.appsvc.connection;

import com.zaxxer.hikari.HikariConfig;

/**
 * Connects via an Oracle Wallet (e.g. Autonomous Database). The wallet
 * directory is appended as the {@code TNS_ADMIN} URL parameter; user/password
 * are passed only if the profile provides them (Autonomous Wallets typically
 * embed the credentials).
 */
public final class WalletAuth implements OracleAuthStrategy {

    @Override
    public String jdbcUrl(ConnectionProfile p) {
        return "jdbc:oracle:thin:@" + p.tnsAlias()
                + "?TNS_ADMIN=" + p.walletPath();
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
