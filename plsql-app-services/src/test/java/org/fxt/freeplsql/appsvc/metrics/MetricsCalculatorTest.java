package org.fxt.freeplsql.appsvc.metrics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MetricsCalculatorTest {

    private final MetricsCalculator calc = new MetricsCalculator();

    @Test
    void emptySourceMinimumValues() {
        assertEquals(0, calc.countLoc(""));
        assertEquals(0, calc.countSloc(""));
        assertEquals(1, calc.countCcn(""));
    }

    @Test
    void countsLines() {
        var src = "line1\nline2\nline3";
        assertEquals(3, calc.countLoc(src));
    }

    @Test
    void slocSkipsBlanksAndComments() {
        var src = """
                -- pure comment

                  -- indented comment
                BEGIN
                    NULL;
                END;
                /* block */
                """;
        // SLOC: BEGIN, NULL;, END;, (block comment alone) → 3
        assertEquals(3, calc.countSloc(src));
    }

    @Test
    void slocCountsBlockCommentTrailingCode() {
        var src = "/* hi */ x := 1;";
        assertEquals(1, calc.countSloc(src));
    }

    @Test
    void ccnStartsAtOneAddsBranchAndAndOr() {
        var src = """
                BEGIN
                    IF x > 0 AND y > 0 THEN
                        NULL;
                    ELSIF z OR w THEN
                        NULL;
                    END IF;
                END;
                """;
        // base 1 + IF + AND + ELSIF + OR = 5
        assertEquals(5, calc.countCcn(src));
    }

    @Test
    void ccnCountsCaseWhen() {
        var src = """
                BEGIN
                    CASE x
                        WHEN 1 THEN NULL;
                        WHEN 2 THEN NULL;
                        ELSE NULL;
                    END CASE;
                END;
                """;
        // 1 + CASE + WHEN + WHEN = 4
        assertEquals(4, calc.countCcn(src));
    }

    @Test
    void ccnCountsLoopWhile() {
        var src = """
                BEGIN
                    WHILE x > 0 LOOP
                        FOR i IN 1..10 LOOP
                            NULL;
                        END LOOP;
                    END LOOP;
                END;
                """;
        // 1 + WHILE + FOR = 3
        assertEquals(3, calc.countCcn(src));
    }
}
