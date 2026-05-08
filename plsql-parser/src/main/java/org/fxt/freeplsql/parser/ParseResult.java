package org.fxt.freeplsql.parser;

import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.List;

public record ParseResult(String source, ParseTree tree, CommonTokenStream tokens, List<SyntaxError> errors) {

    public boolean hasErrors() {
        return !errors.isEmpty();
    }
}
