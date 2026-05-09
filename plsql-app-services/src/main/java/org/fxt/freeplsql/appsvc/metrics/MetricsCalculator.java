package org.fxt.freeplsql.appsvc.metrics;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.Vocabulary;
import org.fxt.freeplsql.parser.gen.PlSqlLexer;
import org.fxt.freeplsql.sync.DbObject;

import java.util.Set;

/**
 * Computes lightweight code metrics for a single PL/SQL source string:
 * <ul>
 *   <li>LOC — total physical lines</li>
 *   <li>SLOC — non-blank, non-comment-only lines</li>
 *   <li>CCN — cyclomatic complexity, counted from ANTLR tokens
 *       (IF, ELSIF, CASE, WHEN, WHILE, FOR, AND, OR), starting from 1</li>
 * </ul>
 */
public final class MetricsCalculator {

    private static final Set<String> CCN_TOKENS = Set.of(
            "IF", "ELSIF", "CASE", "WHEN", "WHILE", "FOR", "AND", "OR");

    public ObjectMetrics compute(DbObject obj, String source, int issueCount) {
        if (source == null) source = "";
        return new ObjectMetrics(obj, countLoc(source), countSloc(source),
                countCcn(source), issueCount);
    }

    public int countLoc(String source) {
        if (source.isEmpty()) return 0;
        return source.split("\\r?\\n", -1).length;
    }

    public int countSloc(String source) {
        if (source.isEmpty()) return 0;
        int sloc = 0;
        boolean inBlockComment = false;
        for (String raw : source.split("\\r?\\n", -1)) {
            String line = raw.trim();
            if (inBlockComment) {
                int end = line.indexOf("*/");
                if (end >= 0) {
                    inBlockComment = false;
                    String tail = line.substring(end + 2).trim();
                    if (!tail.isEmpty() && !tail.startsWith("--")) sloc++;
                }
                continue;
            }
            if (line.isEmpty()) continue;
            if (line.startsWith("--")) continue;
            if (line.startsWith("/*")) {
                int end = line.indexOf("*/", 2);
                if (end < 0) {
                    inBlockComment = true;
                    continue;
                }
                String tail = line.substring(end + 2).trim();
                if (!tail.isEmpty() && !tail.startsWith("--")) sloc++;
                continue;
            }
            sloc++;
        }
        return sloc;
    }

    public int countCcn(String source) {
        if (source.isBlank()) return 1;
        var lexer = new PlSqlLexer(CharStreams.fromString(source));
        lexer.removeErrorListeners();
        var stream = new CommonTokenStream(lexer);
        try {
            stream.fill();
        } catch (RuntimeException e) {
            return 1;
        }
        Vocabulary vocab = lexer.getVocabulary();
        int ccn = 1;
        String prev = null;
        for (Token t : stream.getTokens()) {
            if (t.getChannel() != Token.DEFAULT_CHANNEL) continue;
            String name = vocab.getSymbolicName(t.getType());
            if (name == null) continue;
            // Don't count branch keywords that appear after END (e.g. "END IF", "END CASE").
            if (CCN_TOKENS.contains(name) && !"END".equals(prev)) {
                ccn++;
            }
            prev = name;
        }
        return ccn;
    }
}
