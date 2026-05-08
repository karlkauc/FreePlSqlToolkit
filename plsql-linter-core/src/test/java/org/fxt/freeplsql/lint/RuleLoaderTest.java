package org.fxt.freeplsql.lint;

import org.fxt.freeplsql.lint.yaml.YamlRuleLoader;
import org.fxt.freeplsql.parser.PlSqlParserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuleLoaderTest {

    private final PlSqlParserService parser = new PlSqlParserService();

    @Test
    void yamlRegexRuleDetectsTodoComment() {
        var yaml = """
                rules:
                  - id: C-100
                    name: no-todo
                    severity: WARNING
                    message: "TODO comment must be tracked"
                    pattern:
                      type: regex
                      regex: "(?i)--\\\\s*TODO\\\\b"
                """;
        List<LintRule> rules = YamlRuleLoader.parse(new StringReader(yaml));
        var engine = new LintEngine(rules);

        var src = """
                BEGIN
                    -- TODO: implement this
                    NULL;
                END;
                /
                """;

        var issues = engine.run(parser.parse(src), "todo.sql");
        assertEquals(1, issues.size(), () -> issues.toString());
        assertEquals("C-100", issues.get(0).ruleId());
    }

    @Test
    void yamlForbiddenKeywordRuleDetectsDbmsOutput() {
        var yaml = """
                rules:
                  - id: C-200
                    name: no-dbms-output
                    severity: WARNING
                    message: "DBMS_OUTPUT not allowed in production"
                    pattern:
                      type: forbidden-keyword
                      values: [DBMS_OUTPUT]
                """;
        List<LintRule> rules = YamlRuleLoader.parse(new StringReader(yaml));
        var engine = new LintEngine(rules);

        var src = """
                BEGIN
                    DBMS_OUTPUT.put_line('hello');
                END;
                /
                """;

        var issues = engine.run(parser.parse(src), "dbms.sql");
        assertTrue(issues.stream().anyMatch(i -> "C-200".equals(i.ruleId())),
                () -> issues.toString());
    }

    @Test
    void ruleLoaderCombinesBuiltinAndYamlAndSpi(@TempDir Path tempDir) throws IOException {
        Path yamlFile = tempDir.resolve("rules.yaml");
        Files.writeString(yamlFile, """
                rules:
                  - id: C-300
                    name: no-print
                    severity: INFO
                    message: "no print please"
                    pattern:
                      type: forbidden-keyword
                      values: [PRINT_LINE_CUSTOM]
                """);

        List<LintRule> rules = RuleLoader.loadAll(yamlFile, getClass().getClassLoader());

        // Builtin rules must be there
        assertTrue(rules.stream().anyMatch(r -> "F-5".equals(r.id())), "Expected builtin F-5");
        // YAML rule must be there
        assertTrue(rules.stream().anyMatch(r -> "C-300".equals(r.id())), "Expected YAML C-300");
        // SPI rule (registered in test resources META-INF/services) must be there
        assertTrue(rules.stream().anyMatch(r -> "TEST-SPI-1".equals(r.id())),
                "Expected SPI TEST-SPI-1, got: " + rules.stream().map(LintRule::id).toList());
    }

    @Test
    void spiRuleActuallyChecksSource() throws IOException {
        List<LintRule> rules = RuleLoader.loadAll(null, getClass().getClassLoader());
        var engine = new LintEngine(rules);
        var src = """
                DECLARE
                    forbidden_via_spi NUMBER;
                BEGIN
                    forbidden_via_spi := 1;
                END;
                /
                """;

        var issues = engine.run(parser.parse(src), "spi.sql");
        assertTrue(issues.stream().anyMatch(i -> "TEST-SPI-1".equals(i.ruleId())),
                () -> "Expected TEST-SPI-1 issue, got: " + issues);
    }
}
