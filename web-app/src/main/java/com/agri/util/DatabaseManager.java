
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
 * Central place for JDBC access.
 * Supports local SQLite and deployed PostgreSQL.
 */
public final class DatabaseManager {

    private DatabaseManager() {
    }

    // -------------------------------------------------- paths

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

        return override != null
                ? Path.of(override)
                : scriptDir().resolve("agri.db");
    }

    // -------------------------------------------------- connections

    public static Connection getConnection() throws SQLException {

        String dbUrl = System.getenv("DB_URL");

        // PostgreSQL connection for Render and Neon
        if (dbUrl != null && !dbUrl.isBlank()) {

            String dbUser = System.getenv("DB_USER");
            String dbPassword = System.getenv("DB_PASSWORD");

            if (dbUser == null || dbPassword == null) {
                throw new SQLException(
                    "PostgreSQL database credentials are missing."
                );
            }

            return DriverManager.getConnection(
                dbUrl,
                dbUser,
                dbPassword
            );
        }

        // Local SQLite connection
        Connection connection = DriverManager.getConnection(
            "jdbc:sqlite:" + dbFile()
        );

        try (Statement st = connection.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
        }

        return connection;
    }

    // -------------------------------------------------- scripts

    /**
     * Creates tables and views if they do not exist.
     * Selects the correct schema based on the database type.
     */
    public static void initializeSchema()
            throws SQLException, IOException {

        String dbUrl = System.getenv("DB_URL");

        String schemaFile;

        if (dbUrl != null && !dbUrl.isBlank()) {
            schemaFile = "schema-postgres.sql";
        } else {
            schemaFile = "schema.sql";
        }

        runScript(scriptDir().resolve(schemaFile));
    }

    public static void loadSampleData()
            throws SQLException, IOException {

        runScript(scriptDir().resolve("sample_data.sql"));
    }

    /**
     * Executes SQL statements inside one transaction.
     */
    public static void runScript(Path script)
            throws IOException, SQLException {

        String sql = Files.readString(
            script,
            StandardCharsets.UTF_8
        ).replaceAll("(?m)--.*$", "");

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

    // -------------------------------------------------- helpers

    private static void bind(
            PreparedStatement ps,
            Object[] params
    ) throws SQLException {

        for (int i = 0; i < params.length; i++) {

            Object value = params[i] instanceof LocalDate
                    ? params[i].toString()
                    : params[i];

            ps.setObject(i + 1, value);
        }
    }

    /**
     * Runs an INSERT and returns the generated primary key.
     */
    public static int insert(
            String sql,
            Object... params
    ) throws SQLException {

        try (
            Connection c = getConnection();
            PreparedStatement ps = c.prepareStatement(
                sql,
                Statement.RETURN_GENERATED_KEYS
            )
        ) {

            bind(ps, params);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : -1;
            }
        }
    }

    /**
     * Runs an UPDATE or DELETE.
     */
    public static int update(
            String sql,
            Object... params
    ) throws SQLException {

        try (
            Connection c = getConnection();
            PreparedStatement ps = c.prepareStatement(sql)
        ) {

            bind(ps, params);
            return ps.executeUpdate();
        }
    }

    /**
     * Checks whether a query returns at least one row.
     */
    public static boolean exists(
            String sql,
            Object... params
    ) throws SQLException {

        try (
            Connection c = getConnection();
            PreparedStatement ps = c.prepareStatement(sql)
        ) {

            bind(ps, params);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Returns the first column as a double.
     */
    public static double sum(
            String sql,
            Object... params
    ) throws SQLException {

        try (
            Connection c = getConnection();
            PreparedStatement ps = c.prepareStatement(sql)
        ) {

            bind(ps, params);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getDouble(1) : 0.0;
            }
        }
    }

    /**
     * Maps the first row of a query.
     */
    public static <T> Optional<T> queryOne(
            String sql,
            RowMapper<T> mapper,
            Object... params
    ) throws SQLException {

        try (
            Connection c = getConnection();
            PreparedStatement ps = c.prepareStatement(sql)
        ) {

            bind(ps, params);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next()
                        ? Optional.of(mapper.map(rs))
                        : Optional.empty();
            }
        }
    }
}
