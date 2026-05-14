package org.fxt.freeplsql.app.ui.shell;

import javafx.scene.text.Font;

import java.util.List;
import java.util.Objects;

/**
 * Loads the bundled font files into JavaFX's font registry at startup.
 * Failure is non-fatal — CSS fallback chains still render correctly.
 */
public final class FontLoader {

    /** Visible to tests. */
    public static final List<String> RESOURCE_PATHS = List.of(
            "/fonts/Inter-Regular.ttf",
            "/fonts/Inter-SemiBold.ttf",
            "/fonts/JetBrainsMono-Regular.ttf");

    private FontLoader() {}

    /** Loads every bundled font. Silently skips fonts that fail to load. */
    public static void loadAll() {
        for (String path : RESOURCE_PATHS) {
            var url = FontLoader.class.getResource(path);
            if (url == null) continue;
            Font.loadFont(Objects.requireNonNull(url).toExternalForm(), 12.0);
        }
    }
}
