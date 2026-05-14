package org.fxt.freeplsql.app.ui;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Scene;
import org.fxt.freeplsql.app.MainApp;

/**
 * Owns the dark/light state. Applies AtlantaFX theme + toggles the `.dark`
 * style class on the scene root (which switches our -fxt-* tokens).
 */
public final class ThemeManager {

    private final SimpleBooleanProperty dark = new SimpleBooleanProperty();
    private final Scene scene;
    private final Runnable onChange;

    public ThemeManager(Scene scene, boolean initialDark, Runnable onChange) {
        this.scene = scene;
        this.onChange = onChange;
        this.dark.set(initialDark);
        apply(initialDark);
        this.dark.addListener((obs, was, isDark) -> {
            apply(isDark);
            onChange.run();
        });
    }

    private void apply(boolean isDark) {
        MainApp.applyTheme(isDark);
        var classes = scene.getRoot().getStyleClass();
        if (isDark && !classes.contains("dark")) classes.add("dark");
        if (!isDark) classes.remove("dark");
    }

    public boolean isDark() { return dark.get(); }
    public void toggle()    { dark.set(!dark.get()); }
    public SimpleBooleanProperty darkProperty() { return dark; }
}
