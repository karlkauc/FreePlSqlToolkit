package org.fxt.freeplsql.appsvc.diff;

import org.fxt.freeplsql.appsvc.lint.ProgressSink;
import org.fxt.freeplsql.appsvc.metadata.SchemaMetadataService;
import org.fxt.freeplsql.sync.DbObject;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Compares two Oracle schemas across two connections and returns a
 * {@link DiffResult} with added / removed / modified objects.
 *
 * <p>"Modified" means the DDL differs after whitespace normalisation —
 * insensitive to indentation but sensitive to comments and identifier case.
 */
public final class SchemaDiffService {

    private final SchemaMetadataService metadata = new SchemaMetadataService();

    public DiffResult diff(Connection connA, String labelA, String schemaA,
                           Connection connB, String labelB, String schemaB,
                           ProgressSink progress) throws SQLException {
        Map<String, DbObject> mapA = byKey(metadata.listAllObjects(connA, schemaA));
        Map<String, DbObject> mapB = byKey(metadata.listAllObjects(connB, schemaB));

        List<DbObject> added = new ArrayList<>();
        List<DbObject> removed = new ArrayList<>();
        List<DiffResult.ModifiedObject> modified = new ArrayList<>();

        for (var e : mapA.entrySet()) {
            if (!mapB.containsKey(e.getKey())) removed.add(e.getValue());
        }
        for (var e : mapB.entrySet()) {
            if (!mapA.containsKey(e.getKey())) added.add(e.getValue());
        }

        var common = new ArrayList<String>(mapA.keySet());
        common.retainAll(mapB.keySet());
        for (int i = 0; i < common.size(); i++) {
            if (progress.isCancelled()) break;
            String key = common.get(i);
            progress.update(i + 1, common.size(), key);
            DbObject objA = mapA.get(key);
            DbObject objB = mapB.get(key);
            String ddlA = metadata.getDdl(connA, objA);
            String ddlB = metadata.getDdl(connB, objB);
            if (!normalize(ddlA).equals(normalize(ddlB))) {
                modified.add(new DiffResult.ModifiedObject(objA, objB,
                        ddlA == null ? "" : ddlA,
                        ddlB == null ? "" : ddlB));
            }
        }

        return new DiffResult(labelA, labelB, added, removed, modified);
    }

    private static Map<String, DbObject> byKey(List<DbObject> objects) {
        Map<String, DbObject> out = new LinkedHashMap<>();
        for (DbObject o : objects) {
            out.put(o.type() + "/" + o.name(), o);
        }
        return out;
    }

    private static String normalize(String ddl) {
        if (ddl == null) return "";
        return ddl.replaceAll("\\s+", " ").trim();
    }
}
