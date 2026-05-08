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

public final class LocalVarPrefixRule implements LintRule {

    public static final String ID = "F-12";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String name() {
        return "local-var-prefix";
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
        public void enterVariable_declaration(PlSqlParser.Variable_declarationContext ctx) {
            if (ctx.CONSTANT() != null) {
                return;
            }
            if (ctx.identifier() == null) {
                return;
            }
            String name = ctx.identifier().getText();
            String unquoted = name.startsWith("\"") && name.endsWith("\"") && name.length() >= 2
                    ? name.substring(1, name.length() - 1)
                    : name;
            if (!unquoted.toLowerCase().startsWith("l_")) {
                Token start = ctx.identifier().getStart();
                context.report(new Issue(
                        ID,
                        name(),
                        defaultSeverity(),
                        Position.at(start.getLine(), start.getCharPositionInLine(), name.length()),
                        "Local variable '" + name + "' should start with 'l_' prefix",
                        name
                ));
            }
        }
    }
}
