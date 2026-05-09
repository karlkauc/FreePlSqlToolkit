package org.fxt.freeplsql.appsvc.metrics;

import org.fxt.freeplsql.appsvc.lint.ProgressSink;
import org.fxt.freeplsql.appsvc.metadata.SchemaMetadataService;
import org.fxt.freeplsql.lint.Issue;
import org.fxt.freeplsql.lint.LintEngine;
import org.fxt.freeplsql.parser.PlSqlParserService;
import org.fxt.freeplsql.sync.DbObject;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Computes {@link ObjectMetrics} for every PL/SQL object in a schema. */
public final class MetricsService {

    private final MetricsCalculator calculator = new MetricsCalculator();
    private final SchemaMetadataService metadata = new SchemaMetadataService();
    private final PlSqlParserService parser = new PlSqlParserService();
    private final LintEngine engine;

    public MetricsService(LintEngine engine) {
        this.engine = engine;
    }

    public List<ObjectMetrics> measure(Connection conn, String schema,
                                       List<DbObject> objects, ProgressSink progress) throws SQLException {
        var result = new ArrayList<ObjectMetrics>();
        for (int i = 0; i < objects.size(); i++) {
            if (progress.isCancelled()) break;
            DbObject obj = objects.get(i);
            progress.update(i + 1, objects.size(), obj.name());
            String ddl = metadata.getDdl(conn, obj);
            int issues = 0;
            try {
                List<Issue> found = engine.run(parser.parse(ddl), obj.relativePath());
                issues = found.size();
            } catch (RuntimeException ignored) {
                // un-parsable DDL → issue count stays 0
            }
            result.add(calculator.compute(obj, ddl, issues));
        }
        return result;
    }
}
