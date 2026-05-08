package org.fxt.freeplsql.lint.rules;

import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.fxt.freeplsql.lint.AbstractListenerRule;
import org.fxt.freeplsql.lint.Issue;
import org.fxt.freeplsql.lint.LintRule;
import org.fxt.freeplsql.lint.Position;
import org.fxt.freeplsql.lint.RuleContext;
import org.fxt.freeplsql.lint.Severity;
import org.fxt.freeplsql.parser.gen.PlSqlParser;

public final class GenericRaiseApplicationErrorRule implements LintRule {

    public static final String ID = "F-10";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String name() {
        return "generic-raise-application-error";
    }

    @Override
    public Severity defaultSeverity() {
        return Severity.WARNING;
    }

    @Override
    public void check(RuleContext context) {
        CommonTokenStream tokens = context.parseResult().tokens();
        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            if (token.getType() != PlSqlParser.REGULAR_ID) {
                continue;
            }
            if (!"RAISE_APPLICATION_ERROR".equalsIgnoreCase(token.getText())) {
                continue;
            }
            int next = i + 1;
            while (next < tokens.size() && isHidden(tokens.get(next))) {
                next++;
            }
            if (next >= tokens.size() || tokens.get(next).getType() != PlSqlParser.LEFT_PAREN) {
                continue;
            }
            int firstArg = next + 1;
            while (firstArg < tokens.size() && isHidden(tokens.get(firstArg))) {
                firstArg++;
            }
            if (firstArg >= tokens.size() || tokens.get(firstArg).getType() != PlSqlParser.MINUS_SIGN) {
                continue;
            }
            int numIdx = firstArg + 1;
            while (numIdx < tokens.size() && isHidden(tokens.get(numIdx))) {
                numIdx++;
            }
            if (numIdx >= tokens.size()) {
                continue;
            }
            Token numToken = tokens.get(numIdx);
            if (numToken.getType() == PlSqlParser.UNSIGNED_INTEGER
                    && "20000".equals(numToken.getText())) {
                int length = token.getStopIndex() - token.getStartIndex() + 1;
                context.report(new Issue(
                        ID,
                        name(),
                        defaultSeverity(),
                        Position.at(token.getLine(), token.getCharPositionInLine(), length),
                        "Use a specific error code (-20001..-20999), not the generic -20000",
                        token.getText()
                ));
            }
        }
    }

    private static boolean isHidden(Token token) {
        return token.getChannel() != Token.DEFAULT_CHANNEL;
    }
}
