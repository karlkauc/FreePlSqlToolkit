package org.fxt.freeplsql.lint;

import org.fxt.freeplsql.lint.rules.CommitInTriggerRule;
import org.fxt.freeplsql.lint.rules.ExplicitCursorRule;
import org.fxt.freeplsql.lint.rules.GenericRaiseApplicationErrorRule;
import org.fxt.freeplsql.lint.rules.IdentifierLengthRule;
import org.fxt.freeplsql.lint.rules.IfExitInsteadOfExitWhenRule;
import org.fxt.freeplsql.lint.rules.InParameterPrefixRule;
import org.fxt.freeplsql.lint.rules.InsertWithoutColumnListRule;
import org.fxt.freeplsql.lint.rules.LiteralInWhereClauseRule;
import org.fxt.freeplsql.lint.rules.LocalVarPrefixRule;
import org.fxt.freeplsql.lint.rules.PackageGlobalVariableRule;
import org.fxt.freeplsql.lint.rules.PackageNamingRule;
import org.fxt.freeplsql.lint.rules.ReservedWordIdentifierRule;
import org.fxt.freeplsql.lint.rules.SelectStarRule;
import org.fxt.freeplsql.lint.rules.UpdateDeleteWithoutWhereRule;
import org.fxt.freeplsql.lint.rules.WhenOthersWithoutRaiseRule;

import java.util.List;

public final class DefaultRules {

    private DefaultRules() {
    }

    public static List<LintRule> all() {
        return List.of(
                new IdentifierLengthRule(),
                new ReservedWordIdentifierRule(),
                new PackageGlobalVariableRule(),
                new LiteralInWhereClauseRule(),
                new SelectStarRule(),
                new InsertWithoutColumnListRule(),
                new UpdateDeleteWithoutWhereRule(),
                new IfExitInsteadOfExitWhenRule(),
                new WhenOthersWithoutRaiseRule(),
                new GenericRaiseApplicationErrorRule(),
                new PackageNamingRule(),
                new LocalVarPrefixRule(),
                new InParameterPrefixRule(),
                new CommitInTriggerRule(),
                new ExplicitCursorRule()
        );
    }
}
