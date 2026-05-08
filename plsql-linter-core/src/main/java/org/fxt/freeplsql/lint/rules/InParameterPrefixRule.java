package org.fxt.freeplsql.lint.rules;

import org.fxt.freeplsql.lint.AbstractListenerRule;
import org.fxt.freeplsql.lint.Severity;
import org.fxt.freeplsql.parser.gen.PlSqlParser;

public final class InParameterPrefixRule extends AbstractListenerRule {

    public static final String ID = "F-13";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String name() {
        return "in-parameter-prefix";
    }

    @Override
    public Severity defaultSeverity() {
        return Severity.WARNING;
    }

    @Override
    public void enterParameter(PlSqlParser.ParameterContext ctx) {
        boolean hasOut = (ctx.OUT() != null && !ctx.OUT().isEmpty())
                || (ctx.INOUT() != null && !ctx.INOUT().isEmpty());
        if (hasOut) {
            return;
        }
        if (ctx.parameter_name() == null) {
            return;
        }
        String name = ctx.parameter_name().getText();
        String unquoted = name.startsWith("\"") && name.endsWith("\"") && name.length() >= 2
                ? name.substring(1, name.length() - 1)
                : name;
        if (!unquoted.toLowerCase().startsWith("p_")) {
            report(ctx.parameter_name().getStart(),
                    "IN parameter '" + name + "' should start with 'p_' prefix",
                    name);
        }
    }
}
