package org.fxt.freeplsql.appsvc.lint;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.fxt.freeplsql.lint.Issue;
import org.fxt.freeplsql.lint.Severity;
import org.fxt.freeplsql.sync.DbObject;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Renders a {@link LintReport} as Markdown, HTML, or SARIF 2.1.0. */
public final class LintReportRenderer {

    private static final ObjectMapper SARIF_MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);
    private static final String TOOL_NAME = "FreePlSqlToolkit";
    private static final String TOOL_VERSION = "0.2.0";
    private static final String TOOL_URI = "https://github.com/karlkauc/FreePlSqlToolkit";

    public String renderMarkdown(LintReport report) {
        var sb = new StringBuilder();
        sb.append("# Lint Report\n\n");
        sb.append("- **Connection:** ").append(report.connectionName()).append("\n");
        sb.append("- **Schema:** ").append(report.schemaName()).append("\n");
        sb.append("- **Run at:** ").append(report.runAt()).append("\n");
        sb.append("- **Objects with issues:** ").append(report.totalObjects()).append("\n");
        sb.append("- **Total issues:** ").append(report.totalIssues()).append("\n\n");

        if (report.totalIssues() == 0) {
            sb.append("_No issues found._\n");
            return sb.toString();
        }

        for (var entry : report.issuesByObject().entrySet()) {
            DbObject obj = entry.getKey();
            List<Issue> issues = entry.getValue();
            sb.append("## ").append(obj.name())
              .append(" (").append(obj.type()).append(")\n\n");
            sb.append("| Severity | Rule | Line | Message |\n");
            sb.append("|---|---|---|---|\n");
            for (Issue i : issues) {
                sb.append("| ").append(i.severity().name())
                  .append(" | ").append(i.ruleId())
                  .append(" | ").append(i.position().line())
                  .append(" | ").append(escapeMd(i.message()))
                  .append(" |\n");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    public String renderHtml(LintReport report) {
        var sb = new StringBuilder();
        sb.append("<!DOCTYPE html>\n<html><head><meta charset=\"utf-8\">\n");
        sb.append("<title>Lint Report — ").append(escapeHtml(report.schemaName())).append("</title>\n");
        sb.append("<style>body{font-family:-apple-system,Segoe UI,sans-serif;margin:24px;}");
        sb.append("table{border-collapse:collapse;margin-bottom:24px;}");
        sb.append("th,td{border:1px solid #ccc;padding:4px 8px;font-size:13px;}");
        sb.append("th{background:#f4f4f4;text-align:left;}");
        sb.append(".sev-ERROR{color:#c0392b;font-weight:600;}");
        sb.append(".sev-WARNING{color:#d68910;}");
        sb.append(".sev-INFO{color:#2874a6;}");
        sb.append("</style></head><body>\n");
        sb.append("<h1>Lint Report</h1>\n<ul>");
        sb.append("<li><b>Connection:</b> ").append(escapeHtml(report.connectionName())).append("</li>");
        sb.append("<li><b>Schema:</b> ").append(escapeHtml(report.schemaName())).append("</li>");
        sb.append("<li><b>Run at:</b> ").append(report.runAt()).append("</li>");
        sb.append("<li><b>Objects with issues:</b> ").append(report.totalObjects()).append("</li>");
        sb.append("<li><b>Total issues:</b> ").append(report.totalIssues()).append("</li>");
        sb.append("</ul>\n");

        if (report.totalIssues() == 0) {
            sb.append("<p><em>No issues found.</em></p>");
        }
        for (var entry : report.issuesByObject().entrySet()) {
            DbObject obj = entry.getKey();
            sb.append("<h2>").append(escapeHtml(obj.name()))
              .append(" <small>(").append(obj.type()).append(")</small></h2>\n");
            sb.append("<table><thead><tr><th>Severity</th><th>Rule</th><th>Line</th><th>Message</th></tr></thead><tbody>\n");
            for (Issue i : entry.getValue()) {
                sb.append("<tr><td class=\"sev-").append(i.severity().name()).append("\">")
                  .append(i.severity().name()).append("</td>");
                sb.append("<td>").append(i.ruleId()).append("</td>");
                sb.append("<td>").append(i.position().line()).append("</td>");
                sb.append("<td>").append(escapeHtml(i.message())).append("</td></tr>\n");
            }
            sb.append("</tbody></table>\n");
        }
        sb.append("</body></html>");
        return sb.toString();
    }

    public String renderSarif(LintReport report) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("$schema", "https://schemastore.azurewebsites.net/schemas/json/sarif-2.1.0.json");
        root.put("version", "2.1.0");

        Set<String> ruleIds = new LinkedHashSet<>();
        for (var issues : report.issuesByObject().values()) {
            for (Issue i : issues) ruleIds.add(i.ruleId());
        }
        List<Map<String, Object>> rules = ruleIds.stream()
                .map(id -> {
                    Map<String, Object> r = new LinkedHashMap<>();
                    r.put("id", id);
                    return r;
                })
                .collect(Collectors.toList());

        Map<String, Object> driver = new LinkedHashMap<>();
        driver.put("name", TOOL_NAME);
        driver.put("version", TOOL_VERSION);
        driver.put("informationUri", TOOL_URI);
        driver.put("rules", rules);

        Map<String, Object> tool = new LinkedHashMap<>();
        tool.put("driver", driver);

        List<Map<String, Object>> results = new java.util.ArrayList<>();
        for (var entry : report.issuesByObject().entrySet()) {
            DbObject obj = entry.getKey();
            for (Issue i : entry.getValue()) {
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("ruleId", i.ruleId());
                result.put("level", sarifLevel(i.severity()));
                result.put("message", Map.of("text", i.message()));
                Map<String, Object> region = new LinkedHashMap<>();
                region.put("startLine", i.position().line());
                region.put("startColumn", Math.max(1, i.position().column()));
                Map<String, Object> physicalLocation = new LinkedHashMap<>();
                physicalLocation.put("artifactLocation",
                        Map.of("uri", obj.relativePath()));
                physicalLocation.put("region", region);
                result.put("locations", List.of(Map.of("physicalLocation", physicalLocation)));
                results.add(result);
            }
        }

        Map<String, Object> run = new LinkedHashMap<>();
        run.put("tool", tool);
        run.put("results", results);
        root.put("runs", List.of(run));

        try {
            return SARIF_MAPPER.writeValueAsString(root);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialise SARIF", e);
        }
    }

    private static String sarifLevel(Severity s) {
        return switch (s) {
            case ERROR -> "error";
            case WARNING -> "warning";
            case INFO -> "note";
        };
    }

    private static String escapeMd(String s) {
        if (s == null) return "";
        return s.replace("|", "\\|").replace("\n", " ");
    }

    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
