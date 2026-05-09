package org.fxt.freeplsql.appsvc.lint;

import org.fxt.freeplsql.lint.Issue;
import org.fxt.freeplsql.sync.DbObject;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record LintReport(
        String connectionName,
        String schemaName,
        Instant runAt,
        Map<DbObject, List<Issue>> issuesByObject) {

    public int totalObjects() {
        return issuesByObject.size();
    }

    public int totalIssues() {
        return issuesByObject.values().stream().mapToInt(List::size).sum();
    }

    /** Flat list of every issue, in object iteration order. */
    public List<Map.Entry<DbObject, Issue>> flatten() {
        var out = new ArrayList<Map.Entry<DbObject, Issue>>();
        for (var e : issuesByObject.entrySet()) {
            for (Issue i : e.getValue()) {
                out.add(Map.entry(e.getKey(), i));
            }
        }
        return out;
    }

    public static LintReport empty(String connectionName, String schemaName) {
        return new LintReport(connectionName, schemaName, Instant.now(), new LinkedHashMap<>());
    }
}
