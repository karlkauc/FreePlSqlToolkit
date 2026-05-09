package org.fxt.freeplsql.appsvc.invalid;

public record InvalidEntry(String owner,
                           String name,
                           String type,
                           int line,
                           int position,
                           String text,
                           String attribute) {
}
