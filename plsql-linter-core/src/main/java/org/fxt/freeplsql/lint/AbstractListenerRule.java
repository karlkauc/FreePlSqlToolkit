package org.fxt.freeplsql.lint;

import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.fxt.freeplsql.parser.gen.PlSqlParserBaseListener;

public abstract class AbstractListenerRule extends PlSqlParserBaseListener implements LintRule {

    private RuleContext context;

    @Override
    public final void check(RuleContext context) {
        this.context = context;
        try {
            ParseTreeWalker.DEFAULT.walk(this, context.parseResult().tree());
        } finally {
            this.context = null;
        }
    }

    protected final void report(Token token, String message, String snippet) {
        int length = token.getStopIndex() >= token.getStartIndex()
                ? token.getStopIndex() - token.getStartIndex() + 1
                : 1;
        context.report(new Issue(
                id(),
                name(),
                defaultSeverity(),
                Position.at(token.getLine(), token.getCharPositionInLine(), length),
                message,
                snippet
        ));
    }
}
