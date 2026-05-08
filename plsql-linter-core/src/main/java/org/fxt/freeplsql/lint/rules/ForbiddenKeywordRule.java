package org.fxt.freeplsql.lint.rules;

import org.antlr.v4.runtime.Token;
import org.fxt.freeplsql.lint.Issue;
import org.fxt.freeplsql.lint.LintRule;
import org.fxt.freeplsql.lint.Position;
import org.fxt.freeplsql.lint.RuleContext;
import org.fxt.freeplsql.lint.Severity;

import java.util.LinkedHashSet;
import java.util.Set;

public final class ForbiddenKeywordRule implements LintRule {

    private final String id;
    private final String name;
    private final Severity severity;
    private final String message;
    private final Set<String> forbidden;

    public ForbiddenKeywordRule(String id, String name, Severity severity, String message, Iterable<String> values) {
        this.id = id;
        this.name = name;
        this.severity = severity;
        this.message = message;
        var upper = new LinkedHashSet<String>();
        for (String v : values) {
            upper.add(v.toUpperCase());
        }
        this.forbidden = Set.copyOf(upper);
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Severity defaultSeverity() {
        return severity;
    }

    @Override
    public void check(RuleContext context) {
        var tokens = context.parseResult().tokens();
        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            if (token.getChannel() != Token.DEFAULT_CHANNEL) {
                continue;
            }
            String text = token.getText();
            if (text == null) {
                continue;
            }
            if (forbidden.contains(text.toUpperCase())) {
                int length = token.getStopIndex() - token.getStartIndex() + 1;
                context.report(new Issue(
                        id, name, severity,
                        Position.at(token.getLine(), token.getCharPositionInLine(), length),
                        message + " (found: " + text + ")",
                        text
                ));
            }
        }
    }
}
