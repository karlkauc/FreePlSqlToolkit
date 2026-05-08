package org.fxt.freeplsql.sync;

import org.fxt.freeplsql.sync.config.SyncConfig;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Orchestrates a single sync run: list objects, detect changes via state file,
 * extract DDL via DBMS_METADATA, write to disk, commit to Git.
 */
public final class SyncService {

    public static final String STATE_FILE = ".plsqlsync-state.json";

    private final MetadataExtractor extractor;
    private final StateStore stateStore;

    public SyncService(MetadataExtractor extractor, StateStore stateStore) {
        this.extractor = extractor;
        this.stateStore = stateStore;
    }

    public SyncService() {
        this(new MetadataExtractor(), new StateStore());
    }

    public SyncResult syncOnce(SyncConfig config) throws SQLException, IOException, org.eclipse.jgit.api.errors.GitAPIException {
        Path repoRoot = config.output().repo();
        var git = new GitWriter(repoRoot);
        try (var jgit = git.initOrOpen()) {
            try (Connection conn = openConnection(config.connection())) {
                return runOneSchemaPass(conn, config, git);
            } finally {
                // jgit closed by try-with-resources
            }
        }
    }

    private SyncResult runOneSchemaPass(Connection conn, SyncConfig config, GitWriter git)
            throws SQLException, IOException, org.eclipse.jgit.api.errors.GitAPIException {
        Path stateFile = config.output().repo().resolve(STATE_FILE);
        Map<String, Instant> state = new LinkedHashMap<>(stateStore.load(stateFile));
        List<String> updated = new ArrayList<>();
        int totalScanned = 0;

        for (String schema : config.schemas()) {
            List<DbObject> objects = extractor.listObjects(conn, schema);
            totalScanned += objects.size();
            for (DbObject obj : objects) {
                Instant prev = state.get(obj.key());
                if (prev != null && !prev.isBefore(obj.lastDdlTime())) {
                    continue;
                }
                String ddl = extractor.getDdl(conn, obj);
                git.writeFile(obj.relativePath(), ddl);
                state.put(obj.key(), obj.lastDdlTime());
                updated.add(obj.relativePath());
            }
        }

        stateStore.save(stateFile, state);

        String commitHash = null;
        if (!updated.isEmpty()) {
            try (var jgit = org.eclipse.jgit.api.Git.open(config.output().repo().toFile())) {
                String message = String.format("[plsqlsync] sync %d object(s) from %s",
                        updated.size(), String.join(",", config.schemas()));
                commitHash = git.stageAndCommit(jgit,
                        message,
                        GitWriter.parseAuthor(config.output().commitAuthor()));
            }
        }
        return new SyncResult(totalScanned, List.copyOf(updated), commitHash);
    }

    static Connection openConnection(SyncConfig.ConnectionConfig conn) throws SQLException {
        return DriverManager.getConnection(conn.url(), conn.user(), conn.password());
    }
}
