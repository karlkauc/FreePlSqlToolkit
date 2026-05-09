package org.fxt.freeplsql.appsvc.dependency;

import org.fxt.freeplsql.sync.DbObject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads cross-references from {@code ALL_DEPENDENCIES}. Two directions:
 * what does this object call ({@link #referencesOf}) and who calls this object
 * ({@link #referencedBy}).
 */
public final class DependencyService {

    public List<DependencyEdge> referencesOf(Connection conn, DbObject object) throws SQLException {
        var sql = "SELECT owner, name, type, referenced_owner, referenced_name, referenced_type "
                + "FROM all_dependencies "
                + "WHERE owner = ? AND name = ? AND type = ? "
                + "ORDER BY referenced_owner, referenced_name";
        return query(conn, sql, object.schema(), object.name(), object.type());
    }

    public List<DependencyEdge> referencedBy(Connection conn, DbObject object) throws SQLException {
        var sql = "SELECT owner, name, type, referenced_owner, referenced_name, referenced_type "
                + "FROM all_dependencies "
                + "WHERE referenced_owner = ? AND referenced_name = ? AND referenced_type = ? "
                + "ORDER BY owner, name";
        return query(conn, sql, object.schema(), object.name(), object.type());
    }

    private static List<DependencyEdge> query(Connection conn, String sql,
                                              String owner, String name, String type) throws SQLException {
        var out = new ArrayList<DependencyEdge>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, owner);
            ps.setString(2, name);
            ps.setString(3, type);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new DependencyEdge(
                            rs.getString("owner"),
                            rs.getString("name"),
                            rs.getString("type"),
                            rs.getString("referenced_owner"),
                            rs.getString("referenced_name"),
                            rs.getString("referenced_type")));
                }
            }
        }
        return out;
    }
}
