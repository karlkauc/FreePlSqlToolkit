package org.fxt.freeplsql.appsvc.persistence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppPathsTest {

    @Test
    void defaultsResolveUnderUserHome(@TempDir Path tmp) {
        var paths = AppPaths.defaults();
        assertNotNull(paths.configHome());
        assertTrue(paths.settings().toString().endsWith("settings.json"));
        assertTrue(paths.workspace().toString().endsWith("workspace.json"));
        assertTrue(paths.profiles().toString().endsWith("profiles.enc"));
    }

    @Test
    void customConfigHomeIsRespected(@TempDir Path tmp) {
        var paths = new AppPaths(tmp);
        assertEquals(tmp.resolve("settings.json"), paths.settings());
        assertEquals(tmp.resolve("workspace.json"), paths.workspace());
        assertEquals(tmp.resolve("profiles.enc"), paths.profiles());
    }
}
