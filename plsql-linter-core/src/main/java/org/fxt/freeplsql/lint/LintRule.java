package org.fxt.freeplsql.lint;

public interface LintRule {

    String id();

    String name();

    Severity defaultSeverity();

    void check(RuleContext context);
}
