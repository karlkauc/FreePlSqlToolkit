package org.fxt.freeplsql.sync.config;

import org.yaml.snakeyaml.Yaml;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public record SyncConfig(
        ConnectionConfig connection,
        List<String> schemas,
        OutputConfig output,
        ScheduleConfig schedule
) {

    public record ConnectionConfig(String url, String user, String password) {}

    public record OutputConfig(Path repo, String branch, String commitAuthor, boolean push) {}

    public record ScheduleConfig(int intervalMinutes) {}

    public static SyncConfig load(Path file) {
        try (Reader reader = Files.newBufferedReader(file)) {
            return parse(reader);
        } catch (java.io.IOException e) {
            throw new RuntimeException("Failed to read config: " + file, e);
        }
    }

    @SuppressWarnings("unchecked")
    public static SyncConfig parse(Reader reader) {
        Object root = new Yaml().load(reader);
        if (!(root instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException("Top-level YAML must be an object");
        }

        var conn = (Map<String, Object>) requireMap(map, "connection");
        var connection = new ConnectionConfig(
                requireString(conn, "url"),
                expandEnv(requireString(conn, "user")),
                expandEnv(requireString(conn, "password"))
        );

        Object schemasObj = map.get("schemas");
        List<String> schemas;
        if (schemasObj instanceof List<?> list) {
            schemas = list.stream().map(String::valueOf).toList();
        } else {
            throw new IllegalArgumentException("schemas must be a list");
        }

        var out = (Map<String, Object>) requireMap(map, "output");
        var output = new OutputConfig(
                Path.of(requireString(out, "repo")),
                String.valueOf(out.getOrDefault("branch", "main")),
                String.valueOf(out.getOrDefault("commitAuthor", "PLSQLSync <plsqlsync@example.com>")),
                Boolean.TRUE.equals(out.get("push"))
        );

        Object schedNode = map.get("schedule");
        ScheduleConfig schedule;
        if (schedNode instanceof Map<?, ?> sched) {
            Object iv = sched.get("intervalMinutes");
            int minutes = iv instanceof Number n ? n.intValue() : 5;
            schedule = new ScheduleConfig(minutes);
        } else {
            schedule = new ScheduleConfig(5);
        }

        return new SyncConfig(connection, schemas, output, schedule);
    }

    private static Object requireMap(Map<?, ?> map, String key) {
        Object v = map.get(key);
        if (!(v instanceof Map<?, ?>)) {
            throw new IllegalArgumentException("'" + key + "' must be an object");
        }
        return v;
    }

    private static String requireString(Map<String, Object> map, String key) {
        Object v = map.get(key);
        if (!(v instanceof String s) || s.isBlank()) {
            throw new IllegalArgumentException("'" + key + "' is required (string)");
        }
        return s;
    }

    /** Expands {@code ${ENV_VAR}} placeholders. Unknown vars resolve to empty. */
    static String expandEnv(String value) {
        if (value == null) return null;
        var sb = new StringBuilder();
        int i = 0;
        while (i < value.length()) {
            int dollar = value.indexOf("${", i);
            if (dollar < 0) {
                sb.append(value, i, value.length());
                break;
            }
            sb.append(value, i, dollar);
            int close = value.indexOf('}', dollar + 2);
            if (close < 0) {
                sb.append(value.substring(dollar));
                break;
            }
            String name = value.substring(dollar + 2, close);
            String env = System.getenv(name);
            sb.append(env == null ? "" : env);
            i = close + 1;
        }
        return sb.toString();
    }
}
