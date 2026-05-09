package org.fxt.freeplsql.appsvc.persistence;

/**
 * Persistent application settings. Mutable POJO so FX-thread code can flip
 * fields and trigger a {@link JsonStore#save} without rebuilding instances.
 * Future steps add window geometry, lint debounce, etc.
 */
public final class AppSettings {

    private boolean dark;

    public boolean isDark() {
        return dark;
    }

    public void setDark(boolean dark) {
        this.dark = dark;
    }

    public static AppSettings defaults() {
        return new AppSettings();
    }
}
