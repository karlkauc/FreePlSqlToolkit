package org.fxt.freeplsql.appsvc.diff;

import org.fxt.freeplsql.sync.DbObject;

import java.util.List;

/**
 * Comparison of two schemas. {@code added} lives in B but not A; {@code removed}
 * in A but not B; {@code modified} exists in both with non-equal DDL.
 */
public record DiffResult(String labelA,
                         String labelB,
                         List<DbObject> added,
                         List<DbObject> removed,
                         List<ModifiedObject> modified) {

    public record ModifiedObject(DbObject objectA,
                                 DbObject objectB,
                                 String ddlA,
                                 String ddlB) {
        public String key() {
            return objectA.type() + "/" + objectA.name();
        }
    }

    public int totalChanges() {
        return added.size() + removed.size() + modified.size();
    }
}
