package org.fxt.freeplsql.lint.rules;

import org.fxt.freeplsql.lint.AbstractListenerRule;
import org.fxt.freeplsql.lint.Severity;
import org.fxt.freeplsql.parser.gen.PlSqlParser;

public final class IfExitInsteadOfExitWhenRule extends AbstractListenerRule {

    public static final String ID = "F-8";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String name() {
        return "if-exit-instead-of-exit-when";
    }

    @Override
    public Severity defaultSeverity() {
        return Severity.INFO;
    }

    @Override
    public void enterIf_statement(PlSqlParser.If_statementContext ctx) {
        if (ctx.elsif_part() != null && !ctx.elsif_part().isEmpty()) {
            return;
        }
        if (ctx.else_part() != null) {
            return;
        }
        var seq = ctx.seq_of_statements();
        if (seq == null || seq.statement() == null || seq.statement().size() != 1) {
            return;
        }
        var stmt = seq.statement(0);
        if (stmt.exit_statement() == null) {
            return;
        }
        if (stmt.exit_statement().condition() != null) {
            return;
        }
        report(ctx.getStart(),
                "Use 'EXIT WHEN <condition>' instead of 'IF <condition> THEN EXIT; END IF'",
                "IF ... THEN EXIT; END IF");
    }
}
