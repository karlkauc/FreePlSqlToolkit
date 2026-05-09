package org.fxt.freeplsql.appsvc.invalid;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads compile errors from {@code DBA_ERRORS}, falling back to {@code USER_ERRORS}
 * when the user lacks the {@code SELECT_CATALOG_ROLE} privilege.
 */
public final class InvalidObjectsService {

    public List<InvalidEntry> listErrors(Connection conn, String schema) throws SQLException {
        try {
            return queryDbaErrors(conn, schema);
        } catch (SQLException dbaFailed) {
            return queryUserErrors(conn);
        }
    }

    private List<InvalidEntry> queryDbaErrors(Connection conn, String schema) throws SQLException {
        var sql = "SELECT owner, name, type, line, position, text, attribute "
                + "FROM dba_errors WHERE owner = ? ORDER BY name, line";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, schema.toUpperCase());
            return readRows(ps);
        }
    }

    private List<InvalidEntry> queryUserErrors(Connection conn) throws SQLException {
        var sql = "SELECT user AS owner, name, type, line, position, text, attribute "
                + "FROM user_errors ORDER BY name, line";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            return readRows(ps);
        }
    }

    private static List<InvalidEntry> readRows(PreparedStatement ps) throws SQLException {
        var out = new ArrayList<InvalidEntry>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                out.add(new InvalidEntry(
                        rs.getString("owner"),
                        rs.getString("name"),
                        rs.getString("type"),
                        rs.getInt("line"),
                        rs.getInt("position"),
                        rs.getString("text"),
                        rs.getString("attribute")));
            }
        }
        return out;
    }
}
