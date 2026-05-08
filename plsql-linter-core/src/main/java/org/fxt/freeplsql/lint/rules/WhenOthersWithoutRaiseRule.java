package org.fxt.freeplsql.lint.rules;

import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.fxt.freeplsql.lint.Issue;
import org.fxt.freeplsql.lint.LintRule;
import org.fxt.freeplsql.lint.Position;
import org.fxt.freeplsql.lint.RuleContext;
import org.fxt.freeplsql.lint.Severity;
import org.fxt.freeplsql.parser.gen.PlSqlParser;
import org.fxt.freeplsql.parser.gen.PlSqlParserBaseListener;

public final class WhenOthersWithoutRaiseRule implements LintRule {

    public static final String ID = "F-9";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String name() {
        return "when-others-without-raise";
    }

    @Override
    public Severity defaultSeverity() {
        return Severity.ERROR;
    }

    @Override
    public void check(RuleContext context) {
        var listener = new Listener(context);
        ParseTreeWalker.DEFAULT.walk(listener, context.parseResult().tree());
    }

    private final class Listener extends PlSqlParserBaseListener {
        private final RuleContext context;

        Listener(RuleContext context) {
            this.context = context;
        }

        @Override
        public void enterException_handler(PlSqlParser.Exception_handlerContext ctx) {
            boolean handlesOthers = ctx.exception_name().stream()
                    .anyMatch(name -> "OTHERS".equalsIgnoreCase(name.getText()));
            if (!handlesOthers) {
                return;
            }
            String body = ctx.seq_of_statements() != null
                    ? ctx.seq_of_statements().getText().toUpperCase()
                    : "";
            boolean reraises = body.contains("RAISE")
                    || body.contains("RAISE_APPLICATION_ERROR");
            if (!reraises) {
                Token start = ctx.getStart();
                context.report(new Issue(
                        ID,
                        name(),
                        defaultSeverity(),
                        Position.at(start.getLine(), start.getCharPositionInLine(),
                                start.getStopIndex() - start.getStartIndex() + 1),
                        "WHEN OTHERS handler must re-raise (RAISE) or call RAISE_APPLICATION_ERROR",
                        ctx.getText()
                ));
            }
        }
    }
}
