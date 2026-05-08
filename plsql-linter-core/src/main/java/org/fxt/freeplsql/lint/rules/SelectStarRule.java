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

public final class SelectStarRule implements LintRule {

    public static final String ID = "F-5";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String name() {
        return "select-star";
    }

    @Override
    public Severity defaultSeverity() {
        return Severity.WARNING;
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
        public void enterSelected_list(PlSqlParser.Selected_listContext ctx) {
            Token start = ctx.getStart();
            if (start.getType() == PlSqlParser.ASTERISK) {
                context.report(new Issue(
                        ID,
                        name(),
                        defaultSeverity(),
                        Position.at(start.getLine(), start.getCharPositionInLine(), 1),
                        "Avoid SELECT *: list explicit columns instead",
                        "*"
                ));
            }
        }

        @Override
        public void enterSelect_list_elements(PlSqlParser.Select_list_elementsContext ctx) {
            if (ctx.ASTERISK() != null) {
                Token tok = ctx.ASTERISK().getSymbol();
                context.report(new Issue(
                        ID,
                        name(),
                        defaultSeverity(),
                        Position.at(tok.getLine(), tok.getCharPositionInLine(), 1),
                        "Avoid <table>.*: list explicit columns instead",
                        ctx.getText()
                ));
            }
        }
    }
}
