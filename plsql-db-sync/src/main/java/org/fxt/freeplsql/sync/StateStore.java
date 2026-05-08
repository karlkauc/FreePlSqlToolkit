package org.fxt.freeplsql.sync;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class StateStore {

    public Map<String, Instant> load(Path stateFile) throws IOException {
        if (!Files.exists(stateFile)) {
            return new LinkedHashMap<>();
        }
        try (Reader r = Files.newBufferedReader(stateFile)) {
            return parse(slurp(r));
        }
    }

    public void save(Path stateFile, Map<String, Instant> state) throws IOException {
        if (stateFile.getParent() != null) {
            Files.createDirectories(stateFile.getParent());
        }
        try (Writer w = Files.newBufferedWriter(stateFile)) {
            w.write(serialize(state));
        }
    }

    static String serialize(Map<String, Instant> state) {
        var sb = new StringBuilder();
        sb.append("{\n");
        boolean first = true;
        for (var e : state.entrySet()) {
            if (!first) sb.append(",\n");
            first = false;
            sb.append("  ").append(jsonString(e.getKey()))
                    .append(": ").append(jsonString(e.getValue().toString()));
        }
        sb.append("\n}\n");
        return sb.toString();
    }

    static Map<String, Instant> parse(String json) {
        var map = new LinkedHashMap<String, Instant>();
        if (json == null || json.isBlank()) return map;
        int i = json.indexOf('{');
        int end = json.lastIndexOf('}');
        if (i < 0 || end < 0 || end <= i) return map;
        String body = json.substring(i + 1, end).trim();
        if (body.isEmpty()) return map;
        // tolerant comma split — assumes no commas inside keys/values (fine for our keys)
        for (String entry : body.split(",")) {
            String pair = entry.trim();
            if (pair.isEmpty()) continue;
            int colon = pair.indexOf(':');
            if (colon < 0) continue;
            String k = unquote(pair.substring(0, colon).trim());
            String v = unquote(pair.substring(colon + 1).trim());
            map.put(k, Instant.parse(v));
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(map));
    }

    private static String slurp(Reader r) throws IOException {
        var sb = new StringBuilder();
        var buf = new char[4096];
        int n;
        while ((n = r.read(buf)) > 0) sb.append(buf, 0, n);
        return sb.toString();
    }

    private static String unquote(String s) {
        if (s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"")) {
            return s.substring(1, s.length() - 1)
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\");
        }
        return s;
    }

    private static String jsonString(String s) {
        var sb = new StringBuilder(s.length() + 2);
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                default -> sb.append(c);
            }
        }
        sb.append('"');
        return sb.toString();
    }
}
