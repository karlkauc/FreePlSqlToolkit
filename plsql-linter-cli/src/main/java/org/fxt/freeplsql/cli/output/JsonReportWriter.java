package org.fxt.freeplsql.cli.output;

import org.fxt.freeplsql.lint.Issue;

import java.io.PrintStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public final class JsonReportWriter implements ReportWriter {

    @Override
    public void write(Map<Path, List<Issue>> resultsByFile, PrintStream out) {
        var sb = new StringBuilder();
        sb.append("{\"results\":[");
        boolean firstFile = true;
        for (var entry : resultsByFile.entrySet()) {
            if (!firstFile) sb.append(',');
            firstFile = false;
            sb.append("{\"file\":").append(JsonUtil.str(entry.getKey().toString()))
              .append(",\"issues\":[");
            boolean firstIssue = true;
            for (Issue issue : entry.getValue()) {
                if (!firstIssue) sb.append(',');
                firstIssue = false;
                sb.append('{')
                        .append("\"ruleId\":").append(JsonUtil.str(issue.ruleId())).append(',')
                        .append("\"ruleName\":").append(JsonUtil.str(issue.ruleName())).append(',')
                        .append("\"severity\":").append(JsonUtil.str(issue.severity().name())).append(',')
                        .append("\"line\":").append(issue.position().line()).append(',')
                        .append("\"column\":").append(issue.position().column()).append(',')
                        .append("\"length\":").append(issue.position().length()).append(',')
                        .append("\"message\":").append(JsonUtil.str(issue.message())).append(',')
                        .append("\"snippet\":").append(JsonUtil.str(issue.snippet()))
                        .append('}');
            }
            sb.append("]}");
        }
        sb.append("]}");
        out.println(sb);
    }
}
