package org.fxt.freeplsql.sync;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MetadataExtractorTest {

    @Test
    void buildSourceDdlPrependsCreateOrReplaceAndTerminator() {
        var lines = List.of(
                "PACKAGE BODY pk_fundsxml_converter IS\n",
                "  PROCEDURE p IS BEGIN NULL; END;\n",
                "END pk_fundsxml_converter;\n");

        String ddl = MetadataExtractor.buildSourceDdl(lines);

        assertTrue(ddl.contains("-- Reconstructed from ALL_SOURCE"),
                "should carry an explanatory header comment");
        int comment = ddl.indexOf("-- Reconstructed");
        int create = ddl.indexOf("CREATE OR REPLACE PACKAGE BODY pk_fundsxml_converter IS");
        assertTrue(comment >= 0 && create > comment, "CREATE OR REPLACE follows the comment");
        assertTrue(ddl.contains("END pk_fundsxml_converter;"), "body preserved");
        assertTrue(ddl.endsWith("\n/\n"), "PL/SQL DDL terminated with slash, got:\n" + ddl);
    }

    @Test
    void buildSourceDdlHandlesLinesWithoutTrailingNewline() {
        var lines = List.of("FUNCTION f RETURN NUMBER IS", "BEGIN RETURN 1; END;");

        String ddl = MetadataExtractor.buildSourceDdl(lines);

        assertTrue(ddl.contains("CREATE OR REPLACE FUNCTION f RETURN NUMBER IS\nBEGIN RETURN 1; END;"));
        assertTrue(ddl.endsWith("\n/\n"));
    }

    @Test
    void buildViewDdlWrapsTextInCreateOrReplace() {
        String ddl = MetadataExtractor.buildViewDdl("ESI_XML", "V_FUNDS", "SELECT 1 FROM dual");

        assertEquals(
                "-- Reconstructed from ALL_VIEWS (DBMS_METADATA not permitted cross-schema)\n"
                        + "CREATE OR REPLACE FORCE VIEW \"ESI_XML\".\"V_FUNDS\" AS\n"
                        + "SELECT 1 FROM dual;\n",
                ddl);
    }

    @Test
    void isPermissionErrorRecognisesOra31603And31604() {
        assertTrue(MetadataExtractor.isMetadataPermissionError(new java.sql.SQLException("x", "72000", 31603)));
        assertTrue(MetadataExtractor.isMetadataPermissionError(new java.sql.SQLException("x", "72000", 31604)));
        assertEquals(false,
                MetadataExtractor.isMetadataPermissionError(new java.sql.SQLException("x", "72000", 942)));
    }
}
