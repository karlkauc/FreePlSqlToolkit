package org.fxt.freeplsql.cli.output;

import org.fxt.freeplsql.lint.Issue;

import java.io.PrintStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public final class TextReportWriter implements ReportWriter {

    @Override
    public void write(Map<Path, List<Issue>> resultsByFile, PrintStream out) {
        int totalIssues = 0;
        for (var entry : resultsByFile.entrySet()) {
            Path file = entry.getKey();
            List<Issue> issues = entry.getValue();
            if (issues.isEmpty()) {
                continue;
            }
            out.println(file);
            for (Issue issue : issues) {
                out.printf("  %s:%d:%d  %s  %s  %s%n",
                        issue.severity(),
                        issue.position().line(),
                        issue.position().column(),
                        issue.ruleId(),
                        issue.ruleName(),
                        issue.message());
                totalIssues++;
            }
            out.println();
        }
        out.printf("Total issues: %d across %d file(s)%n", totalIssues, resultsByFile.size());
    }
}
