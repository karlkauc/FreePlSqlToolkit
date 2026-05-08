package org.fxt.freeplsql.sync;

import java.time.Instant;

public record DbObject(String schema, String name, String type, Instant lastDdlTime) {

    /**
     * Maps an {@code all_objects.object_type} value to the form expected by
     * {@code DBMS_METADATA.GET_DDL}: spaces become underscores.
     */
    public String metadataType() {
        return type.replace(' ', '_');
    }

    /** Stable key used in the state file: {@code SCHEMA/TYPE/NAME}. */
    public String key() {
        return schema + "/" + type + "/" + name;
    }

    /** Filesystem-safe relative path: {@code schema/type-folder/name.sql}. */
    public String relativePath() {
        String typeFolder = type.toLowerCase().replace(' ', '_');
        return schema.toLowerCase() + "/" + typeFolder + "/" + name.toLowerCase() + ".sql";
    }
}
