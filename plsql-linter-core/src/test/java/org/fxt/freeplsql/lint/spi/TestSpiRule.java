package org.fxt.freeplsql.lint.spi;

import org.fxt.freeplsql.lint.AbstractListenerRule;
import org.fxt.freeplsql.lint.Severity;
import org.fxt.freeplsql.parser.gen.PlSqlParser;

public final class TestSpiRule extends AbstractListenerRule {

    public static final String ID = "TEST-SPI-1";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String name() {
        return "test-spi-rule";
    }

    @Override
    public Severity defaultSeverity() {
        return Severity.INFO;
    }

    @Override
    public void enterIdentifier(PlSqlParser.IdentifierContext ctx) {
        if ("forbidden_via_spi".equalsIgnoreCase(ctx.getText())) {
            report(ctx.getStart(),
                    "Identifier 'forbidden_via_spi' is forbidden by SPI plugin",
                    ctx.getText());
        }
    }
}
