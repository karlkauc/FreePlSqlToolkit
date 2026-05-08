package org.fxt.freeplsql.lint;

import org.fxt.freeplsql.parser.ParseResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class RuleContext {

    private final ParseResult parseResult;
    private final String sourcePath;
    private final List<Issue> issues = new ArrayList<>();

    public RuleContext(ParseResult parseResult, String sourcePath) {
        this.parseResult = parseResult;
        this.sourcePath = sourcePath;
    }

    public ParseResult parseResult() {
        return parseResult;
    }

    public String sourcePath() {
        return sourcePath;
    }

    public void report(Issue issue) {
        issues.add(issue);
    }

    public List<Issue> issues() {
        return Collections.unmodifiableList(issues);
    }
}
