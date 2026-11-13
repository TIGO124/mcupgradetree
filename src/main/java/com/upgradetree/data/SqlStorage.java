package com.upgradetree.data;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * SQLite or MySQL/MariaDB storage. Paper ships both JDBC drivers. Only the saver thread
 * touches the connection, so one connection (reopened when it drops) is enough.
 */
public final class SqlStorage implements Storage {

    public enum Kind { SQLITE, MYSQL }

    private final Kind kind;
    private final String url;
    private final String user;
    private final String password;
    private final String table;
    private final Logger log;
    private Connection connection;

    private SqlStorage(Kind kind, String url, String user, String password, String table, Logger log) {
        this.kind = kind;
        this.url = url;
        this.user = user;
        this.password = password;
        this.table = table.replaceAll("[^A-Za-z0-9_]", "");
        this.log = log;
    }

    public static SqlStorage sqlite(File file, String table, Logger log) throws SQLException {
        SqlStorage s = new SqlStorage(Kind.SQLITE, "jdbc:sqlite:" + file.getAbsolutePath(), null, null, table, log);
        s.init();
        return s;
    }

    public static SqlStorage mysql(String host, int port, String database, String user, String password, String params,
                                   String table, Logger log) throws SQLException {
        String url = "jdbc:mysql://" + host + ":" + port + "/" + database + (params == null || params.isBlank() ? "" : "?" + params);
        SqlStorage s = new SqlStorage(Kind.MYSQL, url, user, password, table, log);
        s.init();
        return s;
    }

    private synchronized Connection connection() throws SQLException {
        if (connection == null || connection.isClosed() || !connection.isValid(2)) {
            if (kind == Kind.SQLITE) {
                try {
                    Class.forName("org.sqlite.JDBC");
                } catch (ClassNotFoundException ignored) {
                    // DriverManager may still find it
                }
                connection = DriverManager.getConnection(url);
            } else {
                connection = DriverManager.getConnection(url, user, password);
            }
        }
        return connection;
    }

    private void init() throws SQLException {
        try (Statement st = connection().createStatement()) {
            st.executeUpdate("CREATE TABLE IF NOT EXISTS " + table + " ("
                    + "uuid VARCHAR(36) PRIMARY KEY, "
                    + "name VARCHAR(32), "
                    + "points DOUBLE, "
                    + "prestige INT, "
                    + "total_levels INT, "
                    + "data TEXT)");
        }
    }

    @Override
    public synchronized PlayerData.Snapshot load(UUID uuid, String name) throws Exception {
        try (PreparedStatement ps = connection().prepareStatement("SELECT data FROM " + table + " WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return DataCodec.decode(uuid, name, rs.getString(1));
            }
        }
    }

    @Override
    public synchronized void save(PlayerData.Snapshot s) throws SQLException {
        String sql = kind == Kind.SQLITE
                ? "INSERT INTO " + table + " (uuid, name, points, prestige, total_levels, data) VALUES (?, ?, ?, ?, ?, ?) "
                + "ON CONFLICT(uuid) DO UPDATE SET name = excluded.name, points = excluded.points, "
                + "prestige = excluded.prestige, total_levels = excluded.total_levels, data = excluded.data"
                : "INSERT INTO " + table + " (uuid, name, points, prestige, total_levels, data) VALUES (?, ?, ?, ?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE name = VALUES(name), points = VALUES(points), prestige = VALUES(prestige), "
                + "total_levels = VALUES(total_levels), data = VALUES(data)";
        try (PreparedStatement ps = connection().prepareStatement(sql)) {
            ps.setString(1, s.uuid().toString());
            ps.setString(2, s.name());
            ps.setDouble(3, s.points());
            ps.setInt(4, s.prestige());
            ps.setInt(5, s.totalLevels());
            ps.setString(6, DataCodec.encode(s));
            ps.executeUpdate();
        }
    }

    @Override
    public synchronized List<PlayerData.Snapshot> loadAll() throws SQLException {
        List<PlayerData.Snapshot> out = new ArrayList<>();
        try (Statement st = connection().createStatement();
             ResultSet rs = st.executeQuery("SELECT uuid, name, data FROM " + table)) {
            while (rs.next()) {
                try {
                    UUID id = UUID.fromString(rs.getString(1));
                    out.add(DataCodec.decode(id, rs.getString(2), rs.getString(3)));
                } catch (Exception e) {
                    log.fine("Skipped: " + rs.getString(1));
                }
            }
        }
        return out;
    }

    public synchronized boolean isEmpty() throws SQLException {
        try (Statement st = connection().createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + table)) {
            return !rs.next() || rs.getInt(1) == 0;
        }
    }

    @Override
    public String describe() {
        return kind == Kind.SQLITE ? "SQLite" : "MySQL";
    }

    @Override
    public synchronized void close() {
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException ignored) {
            // closing anyway
        }
    }
}
