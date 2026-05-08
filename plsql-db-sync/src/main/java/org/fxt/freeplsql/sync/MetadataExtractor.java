package org.fxt.freeplsql.sync;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class MetadataExtractor {

    public static final Set<String> SUPPORTED_TYPES = Set.of(
            "PACKAGE", "PACKAGE BODY",
            "PROCEDURE", "FUNCTION",
            "TRIGGER", "VIEW",
            "TYPE", "TYPE BODY"
    );

    /**
     * Lists all supported PL/SQL objects in the given schema.
     */
    public List<DbObject> listObjects(Connection conn, String schema) throws SQLException {
        var sql = """
                SELECT object_name, object_type, last_ddl_time
                  FROM all_objects
                 WHERE owner = ?
                   AND object_type IN (
                       'PACKAGE','PACKAGE BODY','PROCEDURE','FUNCTION',
                       'TRIGGER','VIEW','TYPE','TYPE BODY'
                   )
                 ORDER BY object_type, object_name
                """;
        var result = new ArrayList<DbObject>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, schema.toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    var ts = rs.getTimestamp("last_ddl_time");
                    Instant lastDdl = ts != null ? ts.toInstant() : Instant.EPOCH;
                    result.add(new DbObject(
                            schema.toUpperCase(),
                            rs.getString("object_name"),
                            rs.getString("object_type"),
                            lastDdl
                    ));
                }
            }
        }
        return result;
    }

    /**
     * Calls {@code DBMS_METADATA.GET_DDL} and returns the source as a String.
     */
    public String getDdl(Connection conn, DbObject obj) throws SQLException {
        var call = "{ ? = call DBMS_METADATA.GET_DDL(?, ?, ?) }";
        try (CallableStatement cs = conn.prepareCall(call)) {
            cs.registerOutParameter(1, Types.CLOB);
            cs.setString(2, obj.metadataType());
            cs.setString(3, obj.name());
            cs.setString(4, obj.schema());
            cs.execute();
            var clob = cs.getClob(1);
            if (clob == null) {
                return "";
            }
            try {
                return clob.getSubString(1L, (int) clob.length());
            } finally {
                clob.free();
            }
        }
    }
}
