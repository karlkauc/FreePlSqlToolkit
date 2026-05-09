package org.fxt.freeplsql.appsvc.persistence;

import java.nio.file.Path;

/**
 * Resolves config-file locations under a single root. Production uses
 * {@code ~/.fpltoolkit}; tests inject a temp directory.
 */
public record AppPaths(Path configHome) {

    public static AppPaths defaults() {
        return new AppPaths(Path.of(System.getProperty("user.home"), ".fpltoolkit"));
    }

    public Path settings() {
        return configHome.resolve("settings.json");
    }

    public Path workspace() {
        return configHome.resolve("workspace.json");
    }

    public Path profiles() {
        return configHome.resolve("profiles.enc");
    }
}
