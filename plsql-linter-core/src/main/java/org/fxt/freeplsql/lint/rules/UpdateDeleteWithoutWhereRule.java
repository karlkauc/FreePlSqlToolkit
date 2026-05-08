package org.fxt.freeplsql.lint.rules;

import org.fxt.freeplsql.lint.AbstractListenerRule;
import org.fxt.freeplsql.lint.Severity;
import org.fxt.freeplsql.parser.gen.PlSqlParser;

public final class UpdateDeleteWithoutWhereRule extends AbstractListenerRule {

    public static final String ID = "F-7";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String name() {
        return "update-delete-without-where";
    }

    @Override
    public Severity defaultSeverity() {
        return Severity.ERROR;
    }

    @Override
    public void enterUpdate_statement(PlSqlParser.Update_statementContext ctx) {
        if (ctx.where_clause() == null) {
            report(ctx.getStart(),
                    "UPDATE without WHERE clause: confirm this affects the entire table on purpose",
                    "UPDATE " + ctx.general_table_ref().getText());
        }
    }

    @Override
    public void enterDelete_statement(PlSqlParser.Delete_statementContext ctx) {
        if (ctx.where_clause() == null) {
            report(ctx.getStart(),
                    "DELETE without WHERE clause: confirm this affects the entire table on purpose",
                    "DELETE FROM " + ctx.general_table_ref().getText());
        }
    }
}
