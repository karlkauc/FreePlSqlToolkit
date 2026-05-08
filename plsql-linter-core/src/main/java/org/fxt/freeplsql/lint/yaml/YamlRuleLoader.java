package org.fxt.freeplsql.lint.yaml;

import org.fxt.freeplsql.lint.LintRule;
import org.fxt.freeplsql.lint.Severity;
import org.fxt.freeplsql.lint.rules.ForbiddenKeywordRule;
import org.fxt.freeplsql.lint.rules.RegexRule;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public final class YamlRuleLoader {

    private YamlRuleLoader() {
    }

    public static List<LintRule> load(Path yamlFile) throws IOException {
        try (Reader reader = Files.newBufferedReader(yamlFile)) {
            return parse(reader);
        }
    }

    @SuppressWarnings("unchecked")
    public static List<LintRule> parse(Reader reader) {
        Object root = new Yaml().load(reader);
        if (!(root instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException("Expected a YAML object at the top level");
        }
        Object rulesNode = map.get("rules");
        if (rulesNode == null) {
            return List.of();
        }
        if (!(rulesNode instanceof List<?> rulesList)) {
            throw new IllegalArgumentException("'rules' must be a list");
        }

        var rules = new ArrayList<LintRule>();
        int index = 0;
        for (Object item : rulesList) {
            index++;
            if (!(item instanceof Map<?, ?> ruleMap)) {
                throw new IllegalArgumentException("rules[" + index + "] must be an object");
            }
            rules.add(buildRule((Map<String, Object>) ruleMap, index));
        }
        return rules;
    }

    @SuppressWarnings("unchecked")
    private static LintRule buildRule(Map<String, Object> spec, int index) {
        String id = require(spec, "id", index);
        String name = require(spec, "name", index);
        String message = require(spec, "message", index);
        Severity severity = parseSeverity(spec.get("severity"));

        Object patternObj = spec.get("pattern");
        if (!(patternObj instanceof Map<?, ?> patternMap)) {
            throw new IllegalArgumentException("rules[" + index + "].pattern must be an object");
        }
        Map<String, Object> pattern = (Map<String, Object>) patternMap;
        String type = require(pattern, "type", index);

        return switch (type) {
            case "regex" -> {
                Object regex = pattern.get("regex");
                if (!(regex instanceof String s)) {
                    throw new IllegalArgumentException("rules[" + index + "].pattern.regex must be a string");
                }
                yield new RegexRule(id, name, severity, message, Pattern.compile(s));
            }
            case "forbidden-keyword" -> {
                Object values = pattern.get("values");
                if (!(values instanceof List<?> list)) {
                    throw new IllegalArgumentException("rules[" + index + "].pattern.values must be a list");
                }
                var strings = new ArrayList<String>();
                for (Object v : list) {
                    strings.add(String.valueOf(v));
                }
                yield new ForbiddenKeywordRule(id, name, severity, message, strings);
            }
            default -> throw new IllegalArgumentException("rules[" + index + "].pattern.type unknown: " + type);
        };
    }

    private static String require(Map<String, Object> map, String key, int index) {
        Object v = map.get(key);
        if (!(v instanceof String s) || s.isBlank()) {
            throw new IllegalArgumentException("rules[" + index + "]." + key + " is required (string)");
        }
        return s;
    }

    private static Severity parseSeverity(Object node) {
        if (node == null) {
            return Severity.WARNING;
        }
        if (node instanceof String s) {
            return Severity.valueOf(s.toUpperCase());
        }
        throw new IllegalArgumentException("severity must be a string (INFO, WARNING, ERROR)");
    }
}
