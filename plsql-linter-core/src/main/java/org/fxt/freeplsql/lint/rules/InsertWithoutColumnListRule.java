package org.fxt.freeplsql.lint.rules;

import org.fxt.freeplsql.lint.AbstractListenerRule;
import org.fxt.freeplsql.lint.Severity;
import org.fxt.freeplsql.parser.gen.PlSqlParser;

public final class InsertWithoutColumnListRule extends AbstractListenerRule {

    public static final String ID = "F-6";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String name() {
        return "insert-without-column-list";
    }

    @Override
    public Severity defaultSeverity() {
        return Severity.WARNING;
    }

    @Override
    public void enterInsert_into_clause(PlSqlParser.Insert_into_clauseContext ctx) {
        if (ctx.paren_column_list() == null) {
            report(ctx.getStart(),
                    "INSERT without explicit column list; specify columns to be insertion-order independent",
                    ctx.getText());
        }
    }
}
