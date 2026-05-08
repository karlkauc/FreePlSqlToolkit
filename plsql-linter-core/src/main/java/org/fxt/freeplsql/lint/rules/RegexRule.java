package org.fxt.freeplsql.lint.rules;

import org.fxt.freeplsql.lint.Issue;
import org.fxt.freeplsql.lint.LintRule;
import org.fxt.freeplsql.lint.Position;
import org.fxt.freeplsql.lint.RuleContext;
import org.fxt.freeplsql.lint.Severity;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class RegexRule implements LintRule {

    private final String id;
    private final String name;
    private final Severity severity;
    private final String message;
    private final Pattern pattern;

    public RegexRule(String id, String name, Severity severity, String message, Pattern pattern) {
        this.id = id;
        this.name = name;
        this.severity = severity;
        this.message = message;
        this.pattern = pattern;
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
        String source = context.parseResult().source();
        if (source == null) {
            return;
        }
        Matcher matcher = pattern.matcher(source);
        while (matcher.find()) {
            int start = matcher.start();
            int line = lineNumberAt(source, start);
            int col = columnAt(source, start);
            int length = matcher.end() - matcher.start();
            String snippet = matcher.group();
            context.report(new Issue(
                    id, name, severity,
                    Position.at(line, col, length),
                    message,
                    snippet.length() > 80 ? snippet.substring(0, 80) + "..." : snippet
            ));
        }
    }

    private static int lineNumberAt(String source, int index) {
        int line = 1;
        for (int i = 0; i < index; i++) {
            if (source.charAt(i) == '\n') {
                line++;
            }
        }
        return line;
    }

    private static int columnAt(String source, int index) {
        int col = 0;
        for (int i = index - 1; i >= 0; i--) {
            if (source.charAt(i) == '\n') {
                break;
            }
            col++;
        }
        return col;
    }
}
