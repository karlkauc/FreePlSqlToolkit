package org.fxt.freeplsql.appsvc.metrics;

import org.fxt.freeplsql.sync.DbObject;

public record ObjectMetrics(DbObject dbObject,
                            int loc,
                            int sloc,
                            int ccn,
                            int issueCount) {
}
