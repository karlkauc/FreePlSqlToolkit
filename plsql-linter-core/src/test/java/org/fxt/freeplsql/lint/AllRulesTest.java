package org.fxt.freeplsql.lint;

import org.fxt.freeplsql.parser.PlSqlParserService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AllRulesTest {

    private final PlSqlParserService parser = new PlSqlParserService();
    private final LintEngine engine = new LintEngine(DefaultRules.all());

    @Test
    void f1_identifierTooLong() {
        var src = """
                DECLARE
                    l_a_very_long_identifier_name_that_exceeds_thirty_chars NUMBER;
                BEGIN
                    NULL;
                END;
                /
                """;
        assertHas(src, "F-1");
    }

    @Test
    void f2_reservedWordAsIdentifier() {
        var src = """
                CREATE OR REPLACE PROCEDURE p_x IS
                    l_date NUMBER;
                    "NUMBER" NUMBER;
                BEGIN
                    NULL;
                END;
                /
                """;
        assertHas(src, "F-2");
    }

    @Test
    void f3_packageGlobalVariable() {
        var src = """
                CREATE OR REPLACE PACKAGE pkg_x AS
                    g_counter NUMBER;
                    c_max CONSTANT NUMBER := 10;
                END pkg_x;
                /
                """;
        var issues = lint(src);
        assertTrue(issues.stream().anyMatch(i -> "F-3".equals(i.ruleId())
                && i.message().contains("g_counter")),
                () -> issues.toString());
        assertFalse(issues.stream().anyMatch(i -> "F-3".equals(i.ruleId())
                && i.message().contains("c_max")),
                () -> "F-3 should skip CONSTANT, got: " + issues);
    }

    @Test
    void f4_literalInWhereClause() {
        var src = """
                CREATE OR REPLACE PROCEDURE p_x IS
                    l_n NUMBER;
                BEGIN
                    SELECT count(emp_id) INTO l_n FROM employees WHERE dept_id = 42;
                END;
                /
                """;
        assertHas(src, "F-4");
    }

    @Test
    void f6_insertWithoutColumnList() {
        var src = """
                BEGIN
                    INSERT INTO employees VALUES ('Alice', 1000);
                END;
                /
                """;
        assertHas(src, "F-6");
    }

    @Test
    void f7_updateWithoutWhere() {
        var src = """
                BEGIN
                    UPDATE employees SET salary = salary * 1.1;
                END;
                /
                """;
        assertHas(src, "F-7");
    }

    @Test
    void f7_deleteWithoutWhere() {
        var src = """
                BEGIN
                    DELETE FROM employees;
                END;
                /
                """;
        assertHas(src, "F-7");
    }

    @Test
    void f8_ifExitInsteadOfExitWhen() {
        var src = """
                BEGIN
                    LOOP
                        IF rownum > 10 THEN
                            EXIT;
                        END IF;
                    END LOOP;
                END;
                /
                """;
        assertHas(src, "F-8");
    }

    @Test
    void f10_genericRaiseApplicationError() {
        var src = """
                BEGIN
                    RAISE_APPLICATION_ERROR(-20000, 'something');
                END;
                /
                """;
        assertHas(src, "F-10");
    }

    @Test
    void f11_packageMissingPkgPrefix() {
        var src = """
                CREATE OR REPLACE PACKAGE employees_api AS
                    PROCEDURE hire(p_name IN VARCHAR2);
                END employees_api;
                /
                """;
        assertHas(src, "F-11");
    }

    @Test
    void f13_inParameterMissingPPrefix() {
        var src = """
                CREATE OR REPLACE PROCEDURE p_hire(name IN VARCHAR2, salary IN NUMBER) IS
                BEGIN
                    NULL;
                END;
                /
                """;
        assertHas(src, "F-13");
    }

    @Test
    void f14_commitInTrigger() {
        var src = """
                CREATE OR REPLACE TRIGGER trg_audit
                AFTER INSERT ON employees
                FOR EACH ROW
                BEGIN
                    INSERT INTO audit_log VALUES (sysdate);
                    COMMIT;
                END;
                /
                """;
        assertHas(src, "F-14");
    }

    @Test
    void f15_explicitCursor() {
        var src = """
                DECLARE
                    CURSOR c_emps IS SELECT emp_id FROM employees;
                    l_id NUMBER;
                BEGIN
                    OPEN c_emps;
                    FETCH c_emps INTO l_id;
                    CLOSE c_emps;
                END;
                /
                """;
        assertHas(src, "F-15");
    }

    private List<Issue> lint(String src) {
        return engine.run(parser.parse(src), "test.sql");
    }

    private void assertHas(String src, String ruleId) {
        var issues = lint(src);
        assertTrue(issues.stream().anyMatch(i -> ruleId.equals(i.ruleId())),
                () -> "Expected " + ruleId + " issue, got: " + issues);
    }
}
