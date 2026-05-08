package org.fxt.freeplsql.lint.rules;

import org.fxt.freeplsql.lint.AbstractListenerRule;
import org.fxt.freeplsql.lint.Severity;
import org.fxt.freeplsql.parser.gen.PlSqlParser;

public final class PackageGlobalVariableRule extends AbstractListenerRule {

    public static final String ID = "F-3";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String name() {
        return "package-global-variable";
    }

    @Override
    public Severity defaultSeverity() {
        return Severity.WARNING;
    }

    @Override
    public void enterVariable_declaration(PlSqlParser.Variable_declarationContext ctx) {
        if (ctx.CONSTANT() != null) {
            return;
        }
        var parent = ctx.getParent();
        boolean atPackageScope = parent instanceof PlSqlParser.Package_obj_specContext
                || parent instanceof PlSqlParser.Package_obj_bodyContext;
        if (atPackageScope) {
            String name = ctx.identifier() != null ? ctx.identifier().getText() : "<anonymous>";
            report(ctx.getStart(),
                    "Package-level variable '" + name + "' should be declared CONSTANT or hidden inside the package body",
                    name);
        }
    }
}
