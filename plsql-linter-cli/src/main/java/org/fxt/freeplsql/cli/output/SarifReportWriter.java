package org.fxt.freeplsql.cli.output;

import org.fxt.freeplsql.lint.DefaultRules;
import org.fxt.freeplsql.lint.Issue;
import org.fxt.freeplsql.lint.LintRule;
import org.fxt.freeplsql.lint.Severity;

import java.io.PrintStream;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class SarifReportWriter implements ReportWriter {

    private static final String TOOL_NAME = "FreePlSqlToolkit";
    private static final String TOOL_VERSION = "0.1.0";
    private static final String TOOL_URI = "https://github.com/karlkauc/FreePlSqlToolkit";
    private static final String SARIF_VERSION = "2.1.0";
    private static final String SARIF_SCHEMA = "https://json.schemastore.org/sarif-2.1.0.json";

    @Override
    public void write(Map<Path, List<Issue>> resultsByFile, PrintStream out) {
        var rulesById = new java.util.LinkedHashMap<String, LintRule>();
        for (LintRule rule : DefaultRules.all()) {
            rulesById.put(rule.id(), rule);
        }

        Set<String> usedRuleIds = new LinkedHashSet<>();
        for (List<Issue> issues : resultsByFile.values()) {
            for (Issue issue : issues) {
                usedRuleIds.add(issue.ruleId());
            }
        }

        var sb = new StringBuilder();
        sb.append('{');
        sb.append("\"$schema\":").append(JsonUtil.str(SARIF_SCHEMA)).append(',');
        sb.append("\"version\":").append(JsonUtil.str(SARIF_VERSION)).append(',');
        sb.append("\"runs\":[{");

        sb.append("\"tool\":{\"driver\":{");
        sb.append("\"name\":").append(JsonUtil.str(TOOL_NAME)).append(',');
        sb.append("\"version\":").append(JsonUtil.str(TOOL_VERSION)).append(',');
        sb.append("\"informationUri\":").append(JsonUtil.str(TOOL_URI)).append(',');
        sb.append("\"rules\":[");
        boolean firstRule = true;
        for (String ruleId : usedRuleIds) {
            LintRule rule = rulesById.get(ruleId);
            if (rule == null) continue;
            if (!firstRule) sb.append(',');
            firstRule = false;
            sb.append('{')
                    .append("\"id\":").append(JsonUtil.str(rule.id())).append(',')
                    .append("\"name\":").append(JsonUtil.str(rule.name())).append(',')
                    .append("\"shortDescription\":{\"text\":").append(JsonUtil.str(rule.name())).append("},")
                    .append("\"defaultConfiguration\":{\"level\":")
                    .append(JsonUtil.str(toSarifLevel(rule.defaultSeverity())))
                    .append('}')
                    .append('}');
        }
        sb.append("]}},");

        sb.append("\"results\":[");
        boolean firstResult = true;
        for (var entry : resultsByFile.entrySet()) {
            String uri = entry.getKey().toString();
            for (Issue issue : entry.getValue()) {
                if (!firstResult) sb.append(',');
                firstResult = false;
                sb.append('{')
                        .append("\"ruleId\":").append(JsonUtil.str(issue.ruleId())).append(',')
                        .append("\"level\":").append(JsonUtil.str(toSarifLevel(issue.severity()))).append(',')
                        .append("\"message\":{\"text\":").append(JsonUtil.str(issue.message())).append("},")
                        .append("\"locations\":[{\"physicalLocation\":{")
                        .append("\"artifactLocation\":{\"uri\":").append(JsonUtil.str(uri)).append("},")
                        .append("\"region\":{")
                        .append("\"startLine\":").append(issue.position().line()).append(',')
                        .append("\"startColumn\":").append(issue.position().column() + 1).append(',')
                        .append("\"snippet\":{\"text\":").append(JsonUtil.str(issue.snippet())).append('}')
                        .append("}}}]")
                        .append('}');
            }
        }
        sb.append("]}]}");
        out.println(sb);
    }

    private static String toSarifLevel(Severity severity) {
        return switch (severity) {
            case ERROR -> "error";
            case WARNING -> "warning";
            case INFO -> "note";
        };
    }
}
