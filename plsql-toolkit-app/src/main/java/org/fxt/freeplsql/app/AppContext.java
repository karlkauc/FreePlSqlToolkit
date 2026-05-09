package org.fxt.freeplsql.app;

import org.fxt.freeplsql.appsvc.connection.ConnectionManager;
import org.fxt.freeplsql.appsvc.connection.ConnectionProfile;
import org.fxt.freeplsql.appsvc.connection.CryptoStore;
import org.fxt.freeplsql.appsvc.connection.ProfileStore;
import org.fxt.freeplsql.appsvc.persistence.AppPaths;
import org.fxt.freeplsql.appsvc.persistence.AppSettings;
import org.fxt.freeplsql.appsvc.persistence.JsonStore;
import org.fxt.freeplsql.appsvc.persistence.WorkspaceState;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Lightweight DI container — owns paths, persistent settings, the
 * (lazily-unlocked) profile store, and the live connection manager.
 */
public final class AppContext {

    private final AppPaths paths;
    private final JsonStore<AppSettings> settingsStore;
    private final AppSettings settings;
    private final JsonStore<WorkspaceState> workspaceStore;
    private final WorkspaceState workspace;
    private final ProfileStore profileStore;
    private final ConnectionManager connectionManager;
    private List<ConnectionProfile> profiles = new ArrayList<>();

    public AppContext() {
        this(AppPaths.defaults());
    }

    public AppContext(AppPaths paths) {
        this.paths = paths;
        this.settingsStore = new JsonStore<>(paths.settings(), AppSettings.class);
        this.settings = settingsStore.loadOrDefault(AppSettings.defaults());
        this.workspaceStore = new JsonStore<>(paths.workspace(), WorkspaceState.class);
        this.workspace = workspaceStore.loadOrDefault(WorkspaceState.defaults());
        this.profileStore = new ProfileStore(new CryptoStore(paths.profiles()));
        this.connectionManager = new ConnectionManager();
    }

    public WorkspaceState workspace() {
        return workspace;
    }

    public void saveWorkspace() {
        workspaceStore.save(workspace);
    }

    public AppPaths paths() {
        return paths;
    }

    public AppSettings settings() {
        return settings;
    }

    public void saveSettings() {
        settingsStore.save(settings);
    }

    public ProfileStore profileStore() {
        return profileStore;
    }

    public ConnectionManager connectionManager() {
        return connectionManager;
    }

    public void setProfiles(List<ConnectionProfile> profiles) {
        this.profiles = new ArrayList<>(profiles);
    }

    public List<ConnectionProfile> profiles() {
        return List.copyOf(profiles);
    }

    public void saveProfiles() {
        try {
            profileStore.save(profiles);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not save profiles", e);
        }
    }

    public void shutdown() {
        connectionManager.shutdownAll();
    }
}
