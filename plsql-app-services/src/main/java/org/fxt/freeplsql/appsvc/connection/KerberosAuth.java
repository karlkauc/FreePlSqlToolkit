package org.fxt.freeplsql.appsvc.connection;

import com.zaxxer.hikari.HikariConfig;

/**
 * Connects with a Kerberos ticket (kinit). Requires {@code krb5.conf} on the
 * default location and a valid TGT in the ticket cache. Username/password are
 * not used; the principal flows from the OS ticket.
 */
public final class KerberosAuth implements OracleAuthStrategy {

    @Override
    public String jdbcUrl(ConnectionProfile p) {
        return "jdbc:oracle:thin:@//" + p.host() + ":" + p.port() + "/" + p.service();
    }

    @Override
    public void applyConfig(HikariConfig config, ConnectionProfile p) {
        config.addDataSourceProperty("oracle.net.authentication_services", "(KERBEROS5)");
        config.addDataSourceProperty("oracle.net.kerberos5_mutual_authentication", "true");
        // No username/password — auth flows from the kinit ticket.
    }
}
