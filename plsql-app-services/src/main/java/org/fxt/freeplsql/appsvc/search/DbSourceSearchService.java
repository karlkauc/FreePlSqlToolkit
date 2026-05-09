package org.fxt.freeplsql.appsvc.search;

import org.fxt.freeplsql.appsvc.connection.ConnectionHandle;
import org.fxt.freeplsql.appsvc.lint.ProgressSink;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Searches {@code ALL_SOURCE} across one or more active Oracle connections.
 * Regex is compiled client-side; literal search uses Oracle's {@code INSTR}.
 */
public final class DbSourceSearchService {

    private static final int MAX_HITS_PER_CONNECTION = 5_000;

    public List<SearchHit> search(List<ConnectionHandle> handles,
                                  SearchQuery query,
                                  ProgressSink progress) {
        var allHits = new ArrayList<SearchHit>();
        for (int i = 0; i < handles.size(); i++) {
            if (progress.isCancelled()) break;
            ConnectionHandle handle = handles.get(i);
            progress.update(i + 1, handles.size(), handle.profile().name());
            try {
                allHits.addAll(searchOne(handle, query));
            } catch (SQLException ignored) {
                // skip connections we can't read — likely missing privileges
            }
        }
        return allHits;
    }

    private List<SearchHit> searchOne(ConnectionHandle handle, SearchQuery query) throws SQLException {
        if (query.pattern() == null || query.pattern().isEmpty()) return List.of();

        if (query.regex()) {
            return regexSearch(handle, query);
        }
        return literalSearch(handle, query);
    }

    private List<SearchHit> literalSearch(ConnectionHandle handle, SearchQuery query) throws SQLException {
        String sql = query.caseSensitive()
                ? "SELECT owner, name, type, line, text FROM all_source WHERE INSTR(text, ?) > 0 AND ROWNUM <= ?"
                : "SELECT owner, name, type, line, text FROM all_source WHERE INSTR(UPPER(text), UPPER(?)) > 0 AND ROWNUM <= ?";
        var hits = new ArrayList<SearchHit>();
        try (Connection conn = handle.borrow();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, query.pattern());
            ps.setInt(2, MAX_HITS_PER_CONNECTION);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    hits.add(toHit(handle, rs));
                }
            }
        }
        return hits;
    }

    private List<SearchHit> regexSearch(ConnectionHandle handle, SearchQuery query) throws SQLException {
        // Pull all source for the connection's accessible schemas, filter client-side.
        // Bounded by ROWNUM to keep memory in check.
        String sql = "SELECT owner, name, type, line, text FROM all_source WHERE ROWNUM <= ?";
        Pattern pattern = Pattern.compile(query.pattern(),
                query.caseSensitive() ? 0 : Pattern.CASE_INSENSITIVE);
        var hits = new ArrayList<SearchHit>();
        try (Connection conn = handle.borrow();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, MAX_HITS_PER_CONNECTION);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String text = rs.getString("text");
                    if (text != null && pattern.matcher(text).find()) {
                        hits.add(toHit(handle, rs, text));
                    }
                }
            }
        }
        return hits;
    }

    private static SearchHit toHit(ConnectionHandle handle, ResultSet rs) throws SQLException {
        return toHit(handle, rs, rs.getString("text"));
    }

    private static SearchHit toHit(ConnectionHandle handle, ResultSet rs, String text) throws SQLException {
        return new SearchHit(
                handle.profile().id(),
                handle.profile().name(),
                rs.getString("owner"),
                rs.getString("name"),
                rs.getString("type"),
                rs.getInt("line"),
                text == null ? "" : text.stripTrailing());
    }
}
