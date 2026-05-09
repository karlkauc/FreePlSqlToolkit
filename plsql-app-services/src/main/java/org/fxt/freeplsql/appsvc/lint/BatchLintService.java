package org.fxt.freeplsql.appsvc.lint;

import org.fxt.freeplsql.appsvc.metadata.SchemaMetadataService;
import org.fxt.freeplsql.lint.Issue;
import org.fxt.freeplsql.lint.LintEngine;
import org.fxt.freeplsql.parser.ParseResult;
import org.fxt.freeplsql.parser.PlSqlParserService;
import org.fxt.freeplsql.sync.DbObject;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Lints every PL/SQL object in a schema (or a sub-selection). Reports progress
 * after each object and respects {@link ProgressSink#isCancelled()} between
 * objects.
 */
public final class BatchLintService {

    private final LintEngine engine;
    private final PlSqlParserService parser = new PlSqlParserService();
    private final SchemaMetadataService metadata = new SchemaMetadataService();

    public BatchLintService(LintEngine engine) {
        this.engine = engine;
    }

    public LintReport lint(Connection conn,
                           String connectionName,
                           String schemaName,
                           List<DbObject> objects,
                           ProgressSink progress) throws SQLException {
        var result = new LinkedHashMap<DbObject, List<Issue>>();
        int total = objects.size();
        for (int i = 0; i < total; i++) {
            if (progress.isCancelled()) break;
            DbObject obj = objects.get(i);
            progress.update(i + 1, total, obj.name());
            String ddl = metadata.getDdl(conn, obj);
            if (ddl == null || ddl.isBlank()) {
                continue;
            }
            ParseResult parsed;
            try {
                parsed = parser.parse(ddl);
            } catch (RuntimeException e) {
                continue;
            }
            List<Issue> issues = engine.run(parsed, obj.relativePath());
            if (!issues.isEmpty()) {
                result.put(obj, issues);
            }
        }
        return new LintReport(connectionName, schemaName, Instant.now(), result);
    }
}
