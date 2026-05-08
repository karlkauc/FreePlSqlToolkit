package org.fxt.freeplsql.lint.rules;

import org.fxt.freeplsql.lint.AbstractListenerRule;
import org.fxt.freeplsql.lint.Severity;
import org.fxt.freeplsql.parser.gen.PlSqlParser;

public final class PackageNamingRule extends AbstractListenerRule {

    public static final String ID = "F-11";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String name() {
        return "package-naming";
    }

    @Override
    public Severity defaultSeverity() {
        return Severity.INFO;
    }

    @Override
    public void enterCreate_package(PlSqlParser.Create_packageContext ctx) {
        if (ctx.package_name() == null || ctx.package_name().isEmpty()) {
            return;
        }
        var nameCtx = ctx.package_name(0);
        check(nameCtx.getStart().getText(), nameCtx.getStart());
    }

    @Override
    public void enterCreate_package_body(PlSqlParser.Create_package_bodyContext ctx) {
        if (ctx.package_name() == null || ctx.package_name().isEmpty()) {
            return;
        }
        var nameCtx = ctx.package_name(0);
        check(nameCtx.getStart().getText(), nameCtx.getStart());
    }

    private void check(String name, org.antlr.v4.runtime.Token at) {
        String unquoted = name.startsWith("\"") && name.endsWith("\"") && name.length() >= 2
                ? name.substring(1, name.length() - 1)
                : name;
        if (!unquoted.toLowerCase().startsWith("pkg_")) {
            report(at,
                    "Package '" + name + "' should start with 'pkg_' prefix",
                    name);
        }
    }
}
