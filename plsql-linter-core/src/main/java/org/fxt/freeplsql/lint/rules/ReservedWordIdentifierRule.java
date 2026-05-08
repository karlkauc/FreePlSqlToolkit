package org.fxt.freeplsql.lint.rules;

import org.fxt.freeplsql.lint.AbstractListenerRule;
import org.fxt.freeplsql.lint.Severity;
import org.fxt.freeplsql.parser.gen.PlSqlParser;

import java.util.Set;

public final class ReservedWordIdentifierRule extends AbstractListenerRule {

    public static final String ID = "F-2";

    private static final Set<String> AVOID = Set.of(
            "NUMBER", "DATE", "TIMESTAMP", "VARCHAR", "VARCHAR2", "CHAR", "CLOB", "BLOB",
            "ROWID", "LEVEL", "USER", "SESSION", "TYPE", "ROW", "TABLE", "COLUMN",
            "VALUE", "NAME", "COMMENT", "FILE", "PASSWORD", "INDEX"
    );

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String name() {
        return "reserved-word-identifier";
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
        if (AVOID.contains(unquoted.toUpperCase())) {
            report(ctx.getStart(),
                    "Identifier '" + text + "' shadows a reserved word or built-in type",
                    text);
        }
    }
}
