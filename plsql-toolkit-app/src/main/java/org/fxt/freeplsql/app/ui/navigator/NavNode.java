package org.fxt.freeplsql.app.ui.navigator;

import org.fxt.freeplsql.sync.DbObject;

/**
 * Sealed model for the schema navigator tree. Four concrete variants:
 * connection root, schema, object-type folder, and a single DB object.
 * The {@code Loading} variant is a transient placeholder while children load.
 */
public sealed interface NavNode
        permits NavNode.Conn, NavNode.Schema, NavNode.Type, NavNode.Obj, NavNode.Loading {

    String label();

    record Conn(String profileId, String profileName) implements NavNode {
        @Override public String label() { return profileName; }
    }

    record Schema(String profileId, String schemaName) implements NavNode {
        @Override public String label() { return schemaName; }
    }

    /** Folder grouping all objects of one type (PACKAGE, PROCEDURE, …). */
    record Type(String profileId, String schemaName, String objectType, String displayLabel)
            implements NavNode {
        @Override public String label() { return displayLabel; }
    }

    record Obj(String profileId, DbObject dbObject) implements NavNode {
        @Override public String label() { return dbObject.name(); }
    }

    record Loading(String text) implements NavNode {
        @Override public String label() { return text; }
    }
}
