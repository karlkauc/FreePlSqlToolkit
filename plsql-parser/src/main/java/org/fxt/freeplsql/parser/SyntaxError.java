package org.fxt.freeplsql.parser;

public record SyntaxError(int line, int column, String message) {
}
