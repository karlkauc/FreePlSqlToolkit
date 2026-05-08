package org.fxt.freeplsql.parser;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.tree.ParseTree;
import org.fxt.freeplsql.parser.gen.PlSqlLexer;
import org.fxt.freeplsql.parser.gen.PlSqlParser;

import java.util.ArrayList;
import java.util.List;

public final class PlSqlParserService {

    public ParseResult parse(String source) {
        var input = CharStreams.fromString(source);
        var lexer = new PlSqlLexer(input);
        var tokens = new CommonTokenStream(lexer);
        var parser = new PlSqlParser(tokens);

        var errorCollector = new ErrorCollector();
        lexer.removeErrorListeners();
        lexer.addErrorListener(errorCollector);
        parser.removeErrorListeners();
        parser.addErrorListener(errorCollector);

        ParseTree tree = parser.sql_script();
        return new ParseResult(source, tree, tokens, errorCollector.errors);
    }

    private static final class ErrorCollector extends BaseErrorListener {
        final List<SyntaxError> errors = new ArrayList<>();

        @Override
        public void syntaxError(Recognizer<?, ?> recognizer,
                                Object offendingSymbol,
                                int line, int charPositionInLine,
                                String msg,
                                RecognitionException e) {
            errors.add(new SyntaxError(line, charPositionInLine, msg));
        }
    }
}
