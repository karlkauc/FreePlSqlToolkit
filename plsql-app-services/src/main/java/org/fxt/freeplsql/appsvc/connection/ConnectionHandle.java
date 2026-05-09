package org.fxt.freeplsql.appsvc.connection;

import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * One active Oracle connection: an open Hikari pool plus the originating
 * profile. Borrowed connections must be closed by the caller (returns them to
 * the pool).
 */
public final class ConnectionHandle implements AutoCloseable {

    private final ConnectionProfile profile;
    private final HikariDataSource dataSource;

    public ConnectionHandle(ConnectionProfile profile, HikariDataSource dataSource) {
        this.profile = profile;
        this.dataSource = dataSource;
    }

    public ConnectionProfile profile() {
        return profile;
    }

    public DataSource dataSource() {
        return dataSource;
    }

    public Connection borrow() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public void close() {
        if (!dataSource.isClosed()) {
            dataSource.close();
        }
    }
}
