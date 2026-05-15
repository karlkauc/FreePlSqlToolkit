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
     * Returns the DDL for the given object.
     *
     * <p>Tries {@code DBMS_METADATA.GET_DDL} first. That call requires
     * {@code SELECT_CATALOG_ROLE} for objects the connected user does not own;
     * without it Oracle raises {@code ORA-31603} even though the object is
     * visible in {@code ALL_OBJECTS}. In that case the DDL is reconstructed
     * from {@code ALL_SOURCE} / {@code ALL_VIEWS}, which share the same
     * visibility as {@code ALL_OBJECTS}.
     */
    public String getDdl(Connection conn, DbObject obj) throws SQLException {
        try {
            return getDdlViaMetadata(conn, obj);
        } catch (SQLException e) {
            if (isMetadataPermissionError(e)) {
                return getDdlFromSource(conn, obj);
            }
            throw e;
        }
    }

    private String getDdlViaMetadata(Connection conn, DbObject obj) throws SQLException {
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

    /** Reconstructs DDL without DBMS_METADATA, for cross-schema sessions. */
    private String getDdlFromSource(Connection conn, DbObject obj) throws SQLException {
        if ("VIEW".equals(obj.type())) {
            var sql = "SELECT text FROM all_views WHERE owner = ? AND view_name = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, obj.schema());
                ps.setString(2, obj.name());
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        throw noAccessException(obj, "ALL_VIEWS");
                    }
                    return buildViewDdl(obj.schema(), obj.name(), rs.getString(1));
                }
            }
        }

        var sql = "SELECT text FROM all_source"
                + " WHERE owner = ? AND name = ? AND type = ? ORDER BY line";
        var lines = new ArrayList<String>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, obj.schema());
            ps.setString(2, obj.name());
            ps.setString(3, obj.type());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lines.add(rs.getString(1));
                }
            }
        }
        if (lines.isEmpty()) {
            throw noAccessException(obj, "ALL_SOURCE");
        }
        return buildSourceDdl(lines);
    }

    private static SQLException noAccessException(DbObject obj, String view) {
        return new SQLException(
                "Keine Berechtigung, DDL für " + obj.schema() + "." + obj.name()
                        + " zu lesen (DBMS_METADATA: ORA-31603; " + view
                        + " leer). Es fehlt vermutlich SELECT_CATALOG_ROLE.");
    }

    /** True for the Oracle errors that signal a cross-schema DBMS_METADATA denial. */
    static boolean isMetadataPermissionError(SQLException e) {
        int code = e.getErrorCode();
        return code == 31603 || code == 31604;
    }

    /**
     * Assembles a {@code CREATE OR REPLACE} statement from raw
     * {@code ALL_SOURCE.TEXT} lines (line 1 starts with e.g.
     * {@code PACKAGE BODY ...}, without the {@code CREATE OR REPLACE} prefix).
     */
    static String buildSourceDdl(List<String> sourceLines) {
        var body = new StringBuilder();
        for (String line : sourceLines) {
            String l = line == null ? "" : line;
            while (!l.isEmpty() && (l.charAt(l.length() - 1) == '\n' || l.charAt(l.length() - 1) == '\r')) {
                l = l.substring(0, l.length() - 1);
            }
            if (body.length() > 0) {
                body.append('\n');
            }
            body.append(l);
        }
        while (body.length() > 0 && Character.isWhitespace(body.charAt(body.length() - 1))) {
            body.setLength(body.length() - 1);
        }
        return "-- Reconstructed from ALL_SOURCE (DBMS_METADATA not permitted cross-schema)\n"
                + "CREATE OR REPLACE " + body + "\n/\n";
    }

    /** Wraps {@code ALL_VIEWS.TEXT} in a {@code CREATE OR REPLACE FORCE VIEW}. */
    static String buildViewDdl(String schema, String name, String viewText) {
        String text = viewText == null ? "" : viewText.strip();
        return "-- Reconstructed from ALL_VIEWS (DBMS_METADATA not permitted cross-schema)\n"
                + "CREATE OR REPLACE FORCE VIEW \"" + schema + "\".\"" + name + "\" AS\n"
                + text + ";\n";
    }
}
