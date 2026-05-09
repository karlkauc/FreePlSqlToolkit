package org.fxt.freeplsql.appsvc.persistence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonStoreTest {

    @Test
    void loadReturnsEmptyWhenFileMissing(@TempDir Path tmp) throws IOException {
        var store = new JsonStore<>(tmp.resolve("missing.json"), AppSettings.class);
        assertTrue(store.load().isEmpty());
    }

    @Test
    void saveCreatesParentDirectoriesAndRoundtrips(@TempDir Path tmp) throws IOException {
        var nested = tmp.resolve("a/b/c/settings.json");
        var store = new JsonStore<>(nested, AppSettings.class);

        var written = new AppSettings();
        written.setDark(true);
        store.save(written);

        assertTrue(Files.exists(nested));
        Optional<AppSettings> loaded = store.load();
        assertTrue(loaded.isPresent());
        assertTrue(loaded.get().isDark());
    }

    @Test
    void saveOverwritesAtomicallyAndLeavesNoTempFile(@TempDir Path tmp) throws IOException {
        var path = tmp.resolve("settings.json");
        var store = new JsonStore<>(path, AppSettings.class);

        var first = new AppSettings();
        first.setDark(false);
        store.save(first);

        var second = new AppSettings();
        second.setDark(true);
        store.save(second);

        assertTrue(store.load().orElseThrow().isDark());
        assertFalse(Files.exists(path.resolveSibling("settings.json.tmp")),
                "atomic move should leave no .tmp behind");
    }

    @Test
    void loadOrDefaultReturnsFallbackOnCorruptFile(@TempDir Path tmp) throws IOException {
        var path = tmp.resolve("settings.json");
        Files.writeString(path, "{ this is not valid json");
        var store = new JsonStore<>(path, AppSettings.class);

        var fallback = new AppSettings();
        var result = store.loadOrDefault(fallback);
        assertSame(fallback, result);
    }

    @Test
    void loadOrDefaultReturnsFallbackWhenMissing(@TempDir Path tmp) {
        var store = new JsonStore<>(tmp.resolve("missing.json"), AppSettings.class);
        var fallback = new AppSettings();
        assertSame(fallback, store.loadOrDefault(fallback));
    }

    @Test
    void roundTripsExactValue(@TempDir Path tmp) throws IOException {
        var path = tmp.resolve("settings.json");
        var store = new JsonStore<>(path, AppSettings.class);

        var written = new AppSettings();
        written.setDark(true);
        store.save(written);

        AppSettings loaded = store.load().orElseThrow();
        assertEquals(written.isDark(), loaded.isDark());
    }
}
