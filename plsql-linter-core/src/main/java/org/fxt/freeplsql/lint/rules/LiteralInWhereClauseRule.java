package org.fxt.freeplsql.lint.rules;

import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.fxt.freeplsql.lint.AbstractListenerRule;
import org.fxt.freeplsql.lint.Severity;
import org.fxt.freeplsql.parser.gen.PlSqlParser;

public final class LiteralInWhereClauseRule extends AbstractListenerRule {

    public static final String ID = "F-4";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String name() {
        return "literal-in-where-clause";
    }

    @Override
    public Severity defaultSeverity() {
        return Severity.INFO;
    }

    @Override
    public void enterWhere_clause(PlSqlParser.Where_clauseContext ctx) {
        scan(ctx);
    }

    private void scan(ParseTree node) {
        if (node instanceof TerminalNode terminal) {
            Token token = terminal.getSymbol();
            int type = token.getType();
            if (type == PlSqlParser.UNSIGNED_INTEGER
                    || type == PlSqlParser.APPROXIMATE_NUM_LIT
                    || type == PlSqlParser.CHAR_STRING) {
                report(token,
                        "Hardcoded literal '" + token.getText() + "' in WHERE clause; consider a bind variable",
                        token.getText());
            }
            return;
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            scan(node.getChild(i));
        }
    }
}
