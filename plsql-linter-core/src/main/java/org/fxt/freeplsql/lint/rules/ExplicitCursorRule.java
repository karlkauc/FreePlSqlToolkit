package org.fxt.freeplsql.lint.rules;

import org.fxt.freeplsql.lint.AbstractListenerRule;
import org.fxt.freeplsql.lint.Severity;
import org.fxt.freeplsql.parser.gen.PlSqlParser;

public final class ExplicitCursorRule extends AbstractListenerRule {

    public static final String ID = "F-15";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String name() {
        return "explicit-cursor";
    }

    @Override
    public Severity defaultSeverity() {
        return Severity.INFO;
    }

    @Override
    public void enterOpen_statement(PlSqlParser.Open_statementContext ctx) {
        String cursor = ctx.cursor_name() != null ? ctx.cursor_name().getText() : "<cursor>";
        report(ctx.getStart(),
                "Prefer a 'FOR <record> IN <cursor> LOOP' over explicit OPEN/FETCH/CLOSE on '" + cursor + "'",
                "OPEN " + cursor);
    }
}
