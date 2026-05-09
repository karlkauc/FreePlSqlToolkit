package org.fxt.freeplsql.appsvc.lint;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.fxt.freeplsql.lint.Issue;
import org.fxt.freeplsql.lint.Position;
import org.fxt.freeplsql.lint.Severity;
import org.fxt.freeplsql.sync.DbObject;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LintReportRendererTest {

    private final LintReportRenderer renderer = new LintReportRenderer();

    private LintReport sampleReport() {
        var obj = new DbObject("HR", "EMP_API", "PACKAGE", Instant.parse("2025-01-01T00:00:00Z"));
        var issue1 = new Issue("F-7", "UpdateDeleteWithoutWhere", Severity.WARNING,
                Position.at(23, 5, 6), "UPDATE without WHERE", "UPDATE emp …");
        var issue2 = new Issue("F-1", "IdentifierLength", Severity.INFO,
                Position.at(7, 1, 1), "Identifier 'x' is too short", "x NUMBER;");
        var byObj = new LinkedHashMap<DbObject, List<Issue>>();
        byObj.put(obj, List.of(issue1, issue2));
        return new LintReport("Local-XE", "HR", Instant.parse("2026-05-09T13:00:00Z"), byObj);
    }

    @Test
    void markdownContainsHeaderAndIssueRow() {
        String md = renderer.renderMarkdown(sampleReport());
        assertTrue(md.contains("# Lint Report"));
        assertTrue(md.contains("Local-XE"));
        assertTrue(md.contains("EMP_API"));
        assertTrue(md.contains("F-7"));
        assertTrue(md.contains("UPDATE without WHERE"));
    }

    @Test
    void markdownNoIssuesEdgeCase() {
        var empty = LintReport.empty("Local-XE", "HR");
        String md = renderer.renderMarkdown(empty);
        assertTrue(md.contains("_No issues found._"));
    }

    @Test
    void htmlContainsTableRowsAndSeverityClass() {
        String html = renderer.renderHtml(sampleReport());
        assertTrue(html.contains("<title>Lint Report"));
        assertTrue(html.contains("class=\"sev-WARNING\""));
        assertTrue(html.contains("EMP_API"));
        assertTrue(html.contains("UPDATE without WHERE"));
    }

    @Test
    void sarifIsValidJsonWithExpectedShape() throws Exception {
        String json = renderer.renderSarif(sampleReport());
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(json);
        assertEquals("2.1.0", root.path("version").asText());
        JsonNode runs = root.path("runs");
        assertEquals(1, runs.size());
        JsonNode run = runs.get(0);
        assertEquals("FreePlSqlToolkit", run.path("tool").path("driver").path("name").asText());
        JsonNode results = run.path("results");
        assertEquals(2, results.size());
        JsonNode first = results.get(0);
        assertEquals("F-7", first.path("ruleId").asText());
        assertEquals("warning", first.path("level").asText());
        assertEquals(23,
                first.path("locations").get(0).path("physicalLocation").path("region").path("startLine").asInt());
        assertNotNull(first.path("locations").get(0)
                .path("physicalLocation").path("artifactLocation").path("uri").asText());
    }

    @Test
    void sarifSeverityMapping() throws Exception {
        var obj = new DbObject("HR", "X", "PACKAGE", Instant.EPOCH);
        var byObj = new LinkedHashMap<DbObject, List<Issue>>();
        byObj.put(obj, List.of(
                new Issue("E", "n", Severity.ERROR, Position.at(1, 1, 1), "e", null),
                new Issue("W", "n", Severity.WARNING, Position.at(1, 1, 1), "w", null),
                new Issue("I", "n", Severity.INFO, Position.at(1, 1, 1), "i", null)
        ));
        var report = new LintReport("c", "HR", Instant.now(), byObj);

        var json = new ObjectMapper().readTree(renderer.renderSarif(report));
        var results = json.path("runs").get(0).path("results");
        assertEquals("error", results.get(0).path("level").asText());
        assertEquals("warning", results.get(1).path("level").asText());
        assertEquals("note", results.get(2).path("level").asText());
    }
}
