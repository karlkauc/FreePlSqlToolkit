package org.fxt.freeplsql.appsvc.snapshot;

import org.fxt.freeplsql.appsvc.connection.ConnectionProfile;
import org.fxt.freeplsql.appsvc.connection.OracleAuthStrategy;
import org.fxt.freeplsql.sync.SyncResult;
import org.fxt.freeplsql.sync.SyncService;
import org.fxt.freeplsql.sync.config.SyncConfig;

import java.nio.file.Path;
import java.util.List;

/**
 * Runs a one-shot DB → Git snapshot for the given profile + repo path + schemas.
 *
 * <p>Uses {@link SyncService}, which goes through {@code DriverManager}. That
 * works for Easy Connect, TNS-Names, and Wallet profiles. Kerberos profiles
 * need {@code oracle.net.authentication_services} as JDBC properties, which
 * the underlying SyncService does not pass — those will fail until SyncService
 * gains that hook (v0.3).
 */
public final class SnapshotRunner {

    private final SyncService syncService = new SyncService();

    public SyncResult run(ConnectionProfile profile, Path repoPath, List<String> schemas)
            throws Exception {
        var connection = buildConnectionConfig(profile);
        var output = new SyncConfig.OutputConfig(repoPath, "main",
                "FreePlSqlToolkit <noreply@example.com>", false);
        var schedule = new SyncConfig.ScheduleConfig(0);
        var config = new SyncConfig(connection, schemas, output, schedule);
        return syncService.syncOnce(config);
    }

    private static SyncConfig.ConnectionConfig buildConnectionConfig(ConnectionProfile profile) {
        OracleAuthStrategy strategy = OracleAuthStrategy.forType(profile.authType());
        String url = strategy.jdbcUrl(profile);
        String user = profile.username() == null ? "" : profile.username();
        String password = profile.password() == null ? "" : profile.password();
        return new SyncConfig.ConnectionConfig(url, user, password);
    }
}
