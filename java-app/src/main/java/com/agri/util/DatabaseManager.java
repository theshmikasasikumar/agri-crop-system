package com.agri.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Central place for JDBC access: opens connections, runs SQL scripts and offers small helpers
 * (insert / update / exists / sum / queryOne) so the DAO classes stay short.
 *
 * Paths: the database file is  database/agri.db  in the repository. It is found automatically when
 * the app is started from the repository root or from the java-app folder. It can be overridden with
 * -Dagri.db=/path/to/file.db  and -Dagri.dbdir=/path/to/folder-with-sql-scripts.
 */
public final class DatabaseManager {

    private DatabaseManager() {
    }

    // ------------------------------------------------------------------ paths
    public static Path scriptDir() {
        String override = System.getProperty("agri.dbdir");
        if (override != null) {
            return Path.of(override);
        }
        if (Files.isDirectory(Path.of("../database"))) {
            return Path.of("../database");
        }
        if (Files.isDirectory(Path.of("database"))) {
            return Path.of("database");
        }
        return Path.of(".");
    }

    public static Path dbFile() {
        String override = System.getProperty("agri.db");
        return override != null ? Path.of(override) : scriptDir().resolve("agri.db");
    }

    // ------------------------------------------------------------ connections
    public static Connection getConnection() throws SQLException {
        Connection connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile());
        try (Statement st = connection.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
        }
        return connection;
    }

    // ---------------------------------------------------------------- scripts
    /** Creates the tables and the view if they do not exist yet. */
    public static void initializeSchema() throws SQLException, IOException {
        runScript(scriptDir().resolve("schema.sql"));
    }

    public static void loadSampleData() throws SQLException, IOException {
        runScript(scriptDir().resolve("sample_data.sql"));
    }

    /** Executes every statement of an .sql file inside one transaction. */
    public static void runScript(Path script) throws IOException, SQLException {
        String sql = Files.readString(script, StandardCharsets.UTF_8).replaceAll("--[^\\r\\n]*", "");
        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);
            try (Statement st = connection.createStatement()) {
                for (String part : sql.split(";")) {
                    String statement = part.trim();
                    if (!statement.isEmpty()) {
                        st.execute(statement);
                    }
                }
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    // ---------------------------------------------------------------- helpers
    private static void bind(PreparedStatement ps, Object[] params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            Object value = params[i] instanceof LocalDate ? params[i].toString() : params[i];
            ps.setObject(i + 1, value);
        }
    }

    /** Runs an INSERT and returns the generated primary key. */
    public static int insert(String sql, Object... params) throws SQLException {
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, params);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : -1;
            }
        }
    }

    /** Runs an UPDATE or DELETE and returns the number of affected rows. */
    public static int update(String sql, Object... params) throws SQLException {
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, params);
            return ps.executeUpdate();
        }
    }

    /** True if the query returns at least one row. */
    public static boolean exists(String sql, Object... params) throws SQLException {
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** Returns the first column of the first row as a double (0 when there is no row or it is NULL). */
    public static double sum(String sql, Object... params) throws SQLException {
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getDouble(1) : 0.0;
            }
        }
    }

    /** Maps the first row of the result, if any. */
    public static <T> Optional<T> queryOne(String sql, RowMapper<T> mapper, Object... params) throws SQLException {
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapper.map(rs)) : Optional.empty();
            }
        }
    }
}
