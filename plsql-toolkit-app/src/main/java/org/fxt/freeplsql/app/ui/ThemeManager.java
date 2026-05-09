package org.fxt.freeplsql.app.ui;

import javafx.beans.property.SimpleBooleanProperty;
import org.fxt.freeplsql.app.MainApp;

/**
 * Holds the current dark/light state, applies it via {@link MainApp#applyTheme},
 * and notifies a persistence callback whenever it flips.
 */
public final class ThemeManager {

    private final SimpleBooleanProperty dark = new SimpleBooleanProperty();
    private final Runnable onChange;

    public ThemeManager(boolean initialDark, Runnable onChange) {
        this.onChange = onChange;
        this.dark.set(initialDark);
        MainApp.applyTheme(initialDark);
        this.dark.addListener((obs, was, isDark) -> {
            MainApp.applyTheme(isDark);
            onChange.run();
        });
    }

    public boolean isDark() {
        return dark.get();
    }

    public void toggle() {
        dark.set(!dark.get());
    }

    public SimpleBooleanProperty darkProperty() {
        return dark;
    }
}
