package org.fxt.freeplsql.sync;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DbObjectTest {

    @Test
    void metadataTypeReplacesSpacesWithUnderscores() {
        var obj = new DbObject("HR", "PKG_X", "PACKAGE BODY", Instant.EPOCH);
        assertEquals("PACKAGE_BODY", obj.metadataType());
    }

    @Test
    void keyIsStable() {
        var obj = new DbObject("HR", "PKG_X", "PACKAGE", Instant.EPOCH);
        assertEquals("HR/PACKAGE/PKG_X", obj.key());
    }

    @Test
    void relativePathIsLowercaseSlashedFolders() {
        var obj = new DbObject("HR", "PKG_X", "PACKAGE BODY", Instant.EPOCH);
        assertEquals("hr/package_body/pkg_x.sql", obj.relativePath());
    }

    @Test
    void simpleProcedureRelativePath() {
        var obj = new DbObject("HR", "HIRE_EMPLOYEE", "PROCEDURE", Instant.EPOCH);
        assertEquals("hr/procedure/hire_employee.sql", obj.relativePath());
    }
}
