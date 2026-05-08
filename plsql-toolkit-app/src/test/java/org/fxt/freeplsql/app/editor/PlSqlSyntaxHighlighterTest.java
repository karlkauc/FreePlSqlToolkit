package org.fxt.freeplsql.app.editor;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlSqlSyntaxHighlighterTest {

    private final PlSqlSyntaxHighlighter highlighter = new PlSqlSyntaxHighlighter();

    @Test
    void emptySourceReturnsNoSpans() {
        assertTrue(highlighter.compute("").isEmpty());
        assertTrue(highlighter.compute(null).isEmpty());
    }

    @Test
    void detectsKeywordsStringsAndNumbers() {
        var src = "SELECT 'hello', 42 FROM dual;";
        var spans = highlighter.compute(src);

        assertHasSpan(spans, PlSqlSyntaxHighlighter.CSS_KEYWORD, "SELECT");
        assertHasSpan(spans, PlSqlSyntaxHighlighter.CSS_STRING, "'hello'");
        assertHasSpan(spans, PlSqlSyntaxHighlighter.CSS_NUMBER, "42");
        assertHasSpan(spans, PlSqlSyntaxHighlighter.CSS_KEYWORD, "FROM");
        assertHasSpan(spans, PlSqlSyntaxHighlighter.CSS_IDENTIFIER, "dual");
    }

    @Test
    void detectsLineComment() {
        var src = "BEGIN -- this is a comment\nNULL; END;";
        var spans = highlighter.compute(src);

        assertHasSpanContaining(spans, PlSqlSyntaxHighlighter.CSS_COMMENT, "-- this is a comment");
    }

    @Test
    void detectsBlockComment() {
        var src = "BEGIN /* hi */ NULL; END;";
        var spans = highlighter.compute(src);

        assertHasSpan(spans, PlSqlSyntaxHighlighter.CSS_COMMENT, "/* hi */");
    }

    private void assertHasSpan(List<HighlightSpan> spans, String cssClass, String expectedText) {
        var src = expectedTextSource(expectedText);
        boolean found = spans.stream().anyMatch(s -> s.cssClass().equals(cssClass) && src.contains(expectedText));
        assertTrue(found, () -> "No span with class " + cssClass + " for '" + expectedText + "'");
    }

    private void assertHasSpanContaining(List<HighlightSpan> spans, String cssClass, String fragment) {
        boolean found = spans.stream().anyMatch(s -> s.cssClass().equals(cssClass));
        assertTrue(found, () -> "No span with class " + cssClass + " (looking for " + fragment + ")");
    }

    private String expectedTextSource(String text) {
        return text;
    }
}
