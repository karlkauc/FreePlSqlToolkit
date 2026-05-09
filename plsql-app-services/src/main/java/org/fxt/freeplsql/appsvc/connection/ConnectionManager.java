package org.fxt.freeplsql.appsvc.connection;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Tracks active Oracle connections (one Hikari pool per active profile). All
 * mutating operations are synchronous — wrap them in a JavaFX {@code Task}
 * from the UI to keep the FX thread responsive.
 */
public final class ConnectionManager {

    private final Map<String, ConnectionHandle> active = new ConcurrentHashMap<>();
    private final List<Consumer<ConnectionHandle>> connectListeners = new CopyOnWriteArrayList<>();
    private final List<Consumer<String>> disconnectListeners = new CopyOnWriteArrayList<>();

    /**
     * Opens a Hikari pool, takes one connection to verify the credentials, and
     * registers the handle. Returns the handle on success; on failure no pool
     * is leaked.
     */
    public ConnectionHandle connect(ConnectionProfile profile) throws SQLException {
        if (active.containsKey(profile.id())) {
            return active.get(profile.id());
        }
        OracleAuthStrategy strategy = OracleAuthStrategy.forType(profile.authType());
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(strategy.jdbcUrl(profile));
        config.setMaximumPoolSize(Math.max(1, profile.poolSize()));
        config.setConnectionTimeout(15_000);
        config.setIdleTimeout(60_000);
        config.setPoolName("fpl-" + profile.name());
        strategy.applyConfig(config, profile);

        HikariDataSource ds = new HikariDataSource(config);
        try (Connection probe = ds.getConnection()) {
            probe.isValid(5);
        } catch (SQLException e) {
            ds.close();
            throw e;
        }
        ConnectionHandle handle = new ConnectionHandle(profile, ds);
        active.put(profile.id(), handle);
        for (Consumer<ConnectionHandle> l : connectListeners) {
            l.accept(handle);
        }
        return handle;
    }

    public void disconnect(String profileId) {
        ConnectionHandle handle = active.remove(profileId);
        if (handle != null) {
            handle.close();
            for (Consumer<String> l : disconnectListeners) {
                l.accept(profileId);
            }
        }
    }

    public boolean isActive(String profileId) {
        return active.containsKey(profileId);
    }

    public Optional<ConnectionHandle> get(String profileId) {
        return Optional.ofNullable(active.get(profileId));
    }

    public Collection<ConnectionHandle> activeConnections() {
        return List.copyOf(active.values());
    }

    public void addConnectListener(Consumer<ConnectionHandle> listener) {
        connectListeners.add(listener);
    }

    public void addDisconnectListener(Consumer<String> listener) {
        disconnectListeners.add(listener);
    }

    public void shutdownAll() {
        for (String id : List.copyOf(active.keySet())) {
            disconnect(id);
        }
    }
}
