package org.fxt.freeplsql.lint;

public record Issue(String ruleId,
                    String ruleName,
                    Severity severity,
                    Position position,
                    String message,
                    String snippet) {
}
