package org.fxt.freeplsql.appsvc.search;

public record SearchHit(String profileId,
                        String connectionName,
                        String owner,
                        String objectName,
                        String objectType,
                        int line,
                        String snippet) {
}
