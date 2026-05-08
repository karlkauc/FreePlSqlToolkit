package org.fxt.freeplsql.parser;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlSqlParserServiceTest {

    private final PlSqlParserService service = new PlSqlParserService();

    @Test
    void parsesCleanPackageWithoutErrors() throws IOException {
        var source = readSample("samples/clean_package.sql");

        var result = service.parse(source);

        assertNotNull(result.tree());
        assertFalse(result.hasErrors(), () -> "Unexpected errors: " + result.errors());
    }

    @Test
    void parsesDirtyButValidPackage() throws IOException {
        var source = readSample("samples/dirty_package.sql");

        var result = service.parse(source);

        assertNotNull(result.tree());
        assertFalse(result.hasErrors(), () -> "Unexpected errors: " + result.errors());
    }

    @Test
    void reportsSyntaxErrorOnGarbage() {
        var result = service.parse("THIS IS NOT VALID PL/SQL @@@");

        assertTrue(result.hasErrors(), "Expected at least one syntax error");
    }

    private String readSample(String path) throws IOException {
        try (var in = Objects.requireNonNull(
                getClass().getClassLoader().getResourceAsStream(path),
                "missing sample: " + path)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
