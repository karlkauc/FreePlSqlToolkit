package org.fxt.freeplsql.lint;

import org.fxt.freeplsql.parser.ParseResult;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class LintEngine {

    private final List<LintRule> rules;

    public LintEngine(Collection<LintRule> rules) {
        this.rules = List.copyOf(rules);
    }

    public List<LintRule> rules() {
        return rules;
    }

    public List<Issue> run(ParseResult parseResult, String sourcePath) {
        var allIssues = new ArrayList<Issue>();
        for (LintRule rule : rules) {
            var ctx = new RuleContext(parseResult, sourcePath);
            rule.check(ctx);
            allIssues.addAll(ctx.issues());
        }
        return allIssues;
    }
}
