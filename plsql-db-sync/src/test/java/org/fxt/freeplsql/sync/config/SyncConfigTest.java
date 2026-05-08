package org.fxt.freeplsql.sync.config;

import org.junit.jupiter.api.Test;

import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SyncConfigTest {

    @Test
    void parsesMinimalConfig() {
        var yaml = """
                connection:
                  url: jdbc:oracle:thin:@host:1521/XEPDB1
                  user: scott
                  password: tiger
                schemas: [HR, PAYROLL]
                output:
                  repo: ./plsql-source
                  branch: develop
                  commitAuthor: "Sync Bot <bot@example.com>"
                  push: true
                schedule:
                  intervalMinutes: 15
                """;
        var cfg = SyncConfig.parse(new StringReader(yaml));

        assertEquals("jdbc:oracle:thin:@host:1521/XEPDB1", cfg.connection().url());
        assertEquals("scott", cfg.connection().user());
        assertEquals("tiger", cfg.connection().password());
        assertEquals(2, cfg.schemas().size());
        assertEquals("HR", cfg.schemas().get(0));
        assertEquals("develop", cfg.output().branch());
        assertTrue(cfg.output().push());
        assertEquals(15, cfg.schedule().intervalMinutes());
    }

    @Test
    void appliesDefaults() {
        var yaml = """
                connection:
                  url: jdbc:oracle:thin:@host:1521/XEPDB1
                  user: u
                  password: p
                schemas: [HR]
                output:
                  repo: /tmp/repo
                """;
        var cfg = SyncConfig.parse(new StringReader(yaml));

        assertEquals("main", cfg.output().branch());
        assertFalse(cfg.output().push());
        assertEquals(5, cfg.schedule().intervalMinutes());
    }

    @Test
    void expandsEnvironmentVariables() {
        // Empty unknown env var → empty string (rather than literal ${VAR})
        var expanded = SyncConfig.expandEnv("user-${NO_SUCH_VAR_PLEASE}-end");
        assertEquals("user--end", expanded);
    }
}
