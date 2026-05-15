package org.fxt.freeplsql.appsvc.connection;

import com.zaxxer.hikari.HikariConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OracleAuthStrategyTest {

    @Test
    void easyConnectBuildsThinUrl() {
        var p = ConnectionProfile.easyConnect("id", "n",
                "db.example.com", 1521, "XEPDB1", "scott", "tiger", 2);
        var s = OracleAuthStrategy.forType(AuthType.EASY_CONNECT);
        assertEquals("jdbc:oracle:thin:@//db.example.com:1521/XEPDB1", s.jdbcUrl(p));

        var config = new HikariConfig();
        config.setJdbcUrl(s.jdbcUrl(p));
        s.applyConfig(config, p);
        assertEquals("scott", config.getUsername());
        assertEquals("tiger", config.getPassword());
    }

    @Test
    void tnsNamesUsesAlias() {
        var p = ConnectionProfile.tnsNames("id", "n", "PROD_RW", "app", "secret", 4);
        var s = OracleAuthStrategy.forType(AuthType.TNS_NAMES);
        assertEquals("jdbc:oracle:thin:@PROD_RW", s.jdbcUrl(p));

        var config = new HikariConfig();
        config.setJdbcUrl(s.jdbcUrl(p));
        s.applyConfig(config, p);
        assertEquals("app", config.getUsername());
    }

    @Test
    void walletAppendsTnsAdminParam() {
        var p = ConnectionProfile.wallet("id", "n",
                "DB_HIGH", "/opt/wallets/adb", "appuser", "wpw", 2);
        var s = OracleAuthStrategy.forType(AuthType.WALLET);
        assertEquals("jdbc:oracle:thin:@DB_HIGH?TNS_ADMIN=/opt/wallets/adb", s.jdbcUrl(p));

        var config = new HikariConfig();
        config.setJdbcUrl(s.jdbcUrl(p));
        s.applyConfig(config, p);
        assertEquals("appuser", config.getUsername());
    }

    @Test
    void walletWithoutCredentialsLeavesUserNull() {
        var p = ConnectionProfile.wallet("id", "n", "DB_HIGH", "/wallet", "", "", 2);
        var s = OracleAuthStrategy.forType(AuthType.WALLET);

        var config = new HikariConfig();
        config.setJdbcUrl(s.jdbcUrl(p));
        s.applyConfig(config, p);
        assertNull(config.getUsername());
        assertNull(config.getPassword());
    }

    @Test
    void customUrlIsUsedVerbatim() {
        String descriptor = "jdbc:oracle:thin:@(DESCRIPTION=(ADDRESS=(PROTOCOL=TCP)"
                + "(HOST=db.example.com)(PORT=1521))(CONNECT_DATA=(SERVICE_NAME=ORCL)))";
        var p = ConnectionProfile.customUrl("id", "n", descriptor, "scott", "tiger", 2);
        var s = OracleAuthStrategy.forType(AuthType.CUSTOM_URL);
        assertEquals(descriptor, s.jdbcUrl(p));

        var config = new HikariConfig();
        config.setJdbcUrl(s.jdbcUrl(p));
        s.applyConfig(config, p);
        assertEquals("scott", config.getUsername());
        assertEquals("tiger", config.getPassword());
    }

    @Test
    void customUrlWithoutCredentialsLeavesUserNull() {
        var p = ConnectionProfile.customUrl("id", "n",
                "jdbc:oracle:thin:@//db.example.com:1521/ORCL", "", "", 2);
        var s = OracleAuthStrategy.forType(AuthType.CUSTOM_URL);

        var config = new HikariConfig();
        config.setJdbcUrl(s.jdbcUrl(p));
        s.applyConfig(config, p);
        assertNull(config.getUsername());
        assertNull(config.getPassword());
    }

    @Test
    void kerberosUsesEasyConnectAndAuthService() {
        var p = ConnectionProfile.kerberos("id", "n",
                "kdc.host", 1521, "ORCL", "svc/host@REALM", 2);
        var s = OracleAuthStrategy.forType(AuthType.KERBEROS);
        assertEquals("jdbc:oracle:thin:@//kdc.host:1521/ORCL", s.jdbcUrl(p));

        var config = new HikariConfig();
        config.setJdbcUrl(s.jdbcUrl(p));
        s.applyConfig(config, p);
        assertNull(config.getUsername(), "Kerberos uses kinit ticket, not user/pw");
        var props = config.getDataSourceProperties();
        assertTrue(props.toString().contains("KERBEROS5"));
    }
}
