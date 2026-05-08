package org.fxt.freeplsql.lint;

import org.fxt.freeplsql.lint.rules.SelectStarRule;
import org.fxt.freeplsql.parser.PlSqlParserService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LintEngineTest {

    private final PlSqlParserService parser = new PlSqlParserService();
    private final LintEngine engine = new LintEngine(DefaultRules.all());

    @Test
    void cleanCodeProducesNoIssues() {
        var source = """
                CREATE OR REPLACE PROCEDURE p_clean IS
                    l_count NUMBER;
                BEGIN
                    SELECT count(*) INTO l_count FROM employees;
                EXCEPTION
                    WHEN OTHERS THEN
                        RAISE;
                END;
                /
                """;

        var issues = engine.run(parser.parse(source), "clean.sql");

        assertTrue(issues.isEmpty(), () -> "Unexpected issues: " + issues);
    }

    @Test
    void detectsSelectStar() {
        var source = """
                CREATE OR REPLACE PROCEDURE p_dirty IS
                    l_row employees%ROWTYPE;
                BEGIN
                    SELECT * INTO l_row FROM employees WHERE rownum = 1;
                END;
                /
                """;

        var issues = engine.run(parser.parse(source), "dirty.sql");

        assertTrue(issues.stream().anyMatch(i -> SelectStarRule.ID.equals(i.ruleId())),
                () -> "Expected F-5 issue, got: " + issues);
    }

    @Test
    void detectsWhenOthersWithoutRaise() {
        var source = """
                CREATE OR REPLACE PROCEDURE p_swallow IS
                    l_count NUMBER;
                BEGIN
                    SELECT count(emp_id) INTO l_count FROM employees;
                EXCEPTION
                    WHEN OTHERS THEN
                        NULL;
                END;
                /
                """;

        var issues = engine.run(parser.parse(source), "swallow.sql");

        assertTrue(issues.stream().anyMatch(i -> "F-9".equals(i.ruleId())),
                () -> "Expected F-9 issue, got: " + issues);
    }

    @Test
    void detectsLocalVarMissingPrefix() {
        var source = """
                CREATE OR REPLACE PROCEDURE p_bad_naming IS
                    counter NUMBER;
                BEGIN
                    counter := 0;
                END;
                /
                """;

        var issues = engine.run(parser.parse(source), "bad-naming.sql");

        assertFalse(issues.isEmpty());
        assertTrue(issues.stream().anyMatch(i -> "F-12".equals(i.ruleId())),
                () -> "Expected F-12 issue, got: " + issues);
    }

    @Test
    void localVarPrefixRuleSkipsConstants() {
        var source = """
                CREATE OR REPLACE PROCEDURE p_const IS
                    c_max CONSTANT NUMBER := 10;
                BEGIN
                    NULL;
                END;
                /
                """;

        var issues = engine.run(parser.parse(source), "const.sql");

        assertTrue(issues.stream().noneMatch(i -> "F-12".equals(i.ruleId())),
                () -> "F-12 should skip CONSTANT declarations, got: " + issues);
    }
}
