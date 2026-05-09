package org.fxt.freeplsql.appsvc.metadata;

import org.fxt.freeplsql.sync.DbObject;
import org.fxt.freeplsql.sync.MetadataExtractor;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Read-only metadata access on top of the existing {@link MetadataExtractor}.
 * Adds schema enumeration, per-object-type filtering, and DDL fetching with
 * graceful empty-CLOB handling for low-privilege users.
 */
public final class SchemaMetadataService {

    private final MetadataExtractor extractor = new MetadataExtractor();

    public List<String> listSchemas(Connection conn) throws SQLException {
        var schemas = new ArrayList<String>();
        try (Statement st = conn.createStatement();
             var rs = st.executeQuery("SELECT username FROM all_users ORDER BY username")) {
            while (rs.next()) {
                schemas.add(rs.getString(1));
            }
        }
        return schemas;
    }

    public List<DbObject> listAllObjects(Connection conn, String schema) throws SQLException {
        return extractor.listObjects(conn, schema);
    }

    public List<DbObject> listObjectsByType(Connection conn, String schema, String objectType) throws SQLException {
        return extractor.listObjects(conn, schema).stream()
                .filter(o -> o.type().equals(objectType))
                .toList();
    }

    public String getDdl(Connection conn, DbObject obj) throws SQLException {
        return extractor.getDdl(conn, obj);
    }
}
