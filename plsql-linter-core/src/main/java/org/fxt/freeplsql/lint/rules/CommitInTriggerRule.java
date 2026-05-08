package org.fxt.freeplsql.lint.rules;

import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.fxt.freeplsql.lint.AbstractListenerRule;
import org.fxt.freeplsql.lint.Severity;
import org.fxt.freeplsql.parser.gen.PlSqlParser;

public final class CommitInTriggerRule extends AbstractListenerRule {

    public static final String ID = "F-14";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String name() {
        return "commit-in-trigger";
    }

    @Override
    public Severity defaultSeverity() {
        return Severity.ERROR;
    }

    @Override
    public void enterTrigger_body(PlSqlParser.Trigger_bodyContext ctx) {
        scan(ctx);
    }

    private void scan(ParseTree node) {
        if (node instanceof TerminalNode terminal) {
            Token token = terminal.getSymbol();
            if (token.getType() == PlSqlParser.COMMIT) {
                report(token,
                        "COMMIT inside a trigger violates the transactional boundary of the calling statement",
                        token.getText());
            }
            return;
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            scan(node.getChild(i));
        }
    }
}
