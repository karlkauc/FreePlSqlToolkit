package org.fxt.freeplsql.appsvc.search;

import java.util.List;

/**
 * Pattern + flags + connection-id filter for {@link DbSourceSearchService}.
 * Empty {@link #profileIds} means "all currently active connections".
 */
public record SearchQuery(String pattern,
                          boolean regex,
                          boolean caseSensitive,
                          List<String> profileIds) {
    public SearchQuery {
        profileIds = profileIds == null ? List.of() : List.copyOf(profileIds);
    }
}
