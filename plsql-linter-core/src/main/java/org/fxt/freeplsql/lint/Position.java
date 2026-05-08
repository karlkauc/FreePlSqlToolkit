package org.fxt.freeplsql.lint;

public record Position(int line, int column, int length) {

    public static Position at(int line, int column, int length) {
        return new Position(line, column, length);
    }
}
