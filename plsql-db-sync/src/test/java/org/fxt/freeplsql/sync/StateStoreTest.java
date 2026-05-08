package org.fxt.freeplsql.sync;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StateStoreTest {

    private final StateStore store = new StateStore();

    @Test
    void roundTripsThroughJson(@TempDir Path tempDir) throws Exception {
        Map<String, Instant> original = new LinkedHashMap<>();
        original.put("HR/PACKAGE/PKG_A", Instant.parse("2026-05-08T10:00:00Z"));
        original.put("HR/PACKAGE BODY/PKG_A", Instant.parse("2026-05-08T10:05:00Z"));

        Path file = tempDir.resolve("state.json");
        store.save(file, original);
        Map<String, Instant> reloaded = store.load(file);

        assertEquals(original, reloaded);
    }

    @Test
    void missingFileReturnsEmpty(@TempDir Path tempDir) throws Exception {
        var loaded = store.load(tempDir.resolve("does-not-exist.json"));
        assertTrue(loaded.isEmpty());
    }
}
