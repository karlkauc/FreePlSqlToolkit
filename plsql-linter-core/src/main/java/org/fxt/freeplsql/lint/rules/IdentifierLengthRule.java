package org.fxt.freeplsql.lint.rules;

import org.fxt.freeplsql.lint.AbstractListenerRule;
import org.fxt.freeplsql.lint.Severity;
import org.fxt.freeplsql.parser.gen.PlSqlParser;

public final class IdentifierLengthRule extends AbstractListenerRule {

    public static final String ID = "F-1";
    private static final int MAX_LENGTH = 30;

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String name() {
        return "identifier-length";
    }

    @Override
    public Severity defaultSeverity() {
        return Severity.WARNING;
    }

    @Override
    public void enterIdentifier(PlSqlParser.IdentifierContext ctx) {
        String text = ctx.getText();
        String unquoted = text.startsWith("\"") && text.endsWith("\"") && text.length() >= 2
                ? text.substring(1, text.length() - 1)
                : text;
        if (unquoted.length() > MAX_LENGTH) {
            report(ctx.getStart(),
                    "Identifier '" + text + "' exceeds " + MAX_LENGTH + " characters (" + unquoted.length() + ")",
                    text);
        }
    }
}
