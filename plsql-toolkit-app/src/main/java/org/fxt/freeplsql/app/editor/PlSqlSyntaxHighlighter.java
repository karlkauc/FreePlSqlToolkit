package org.fxt.freeplsql.app.editor;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.Vocabulary;
import org.fxt.freeplsql.parser.gen.PlSqlLexer;

import java.util.ArrayList;
import java.util.List;

/**
 * Computes syntax-highlight spans from PL/SQL source. Pure function, no JavaFX
 * dependency — directly testable.
 */
public final class PlSqlSyntaxHighlighter {

    public static final String CSS_KEYWORD = "syntax-keyword";
    public static final String CSS_STRING = "syntax-string";
    public static final String CSS_NUMBER = "syntax-number";
    public static final String CSS_COMMENT = "syntax-comment";
    public static final String CSS_IDENTIFIER = "syntax-identifier";

    public List<HighlightSpan> compute(String source) {
        var spans = new ArrayList<HighlightSpan>();
        if (source == null || source.isEmpty()) {
            return spans;
        }
        var lexer = new PlSqlLexer(CharStreams.fromString(source));
        lexer.removeErrorListeners();
        var tokens = new CommonTokenStream(lexer);
        tokens.fill();
        Vocabulary vocab = lexer.getVocabulary();

        for (Token token : tokens.getTokens()) {
            if (token.getType() == Token.EOF) {
                continue;
            }
            int len = token.getStopIndex() - token.getStartIndex() + 1;
            if (len <= 0) continue;
            String css = classifyToken(token, vocab);
            if (css != null) {
                spans.add(new HighlightSpan(token.getStartIndex(), len, css));
            }
        }
        return spans;
    }

    private static String classifyToken(Token token, Vocabulary vocab) {
        int type = token.getType();
        String name = vocab.getSymbolicName(type);
        if (name == null) {
            return null;
        }
        return switch (name) {
            case "SINGLE_LINE_COMMENT", "MULTI_LINE_COMMENT", "REMARK_COMMENT" -> CSS_COMMENT;
            case "CHAR_STRING", "NCHAR_STRING", "QUOTED_STRING", "CHAR_STRING_PERL" -> CSS_STRING;
            case "UNSIGNED_INTEGER", "APPROXIMATE_NUM_LIT" -> CSS_NUMBER;
            case "REGULAR_ID" -> CSS_IDENTIFIER;
            default -> isKeywordTokenName(name) ? CSS_KEYWORD : null;
        };
    }

    private static boolean isKeywordTokenName(String name) {
        // Lexer keywords are uppercase letters with optional digits/underscores;
        // operators tend to be symbolic (and have null symbolic names).
        if (name.length() < 2) return false;
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            boolean ok = (c >= 'A' && c <= 'Z') || c == '_' || (c >= '0' && c <= '9');
            if (!ok) return false;
        }
        return true;
    }
}
