package isp;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/** PostgreSQL persistence for the portal's JSON state document, accessed through JDBC. */
public final class JdbcStore {
    private final String url;
    private final String username;
    private final String password;

    public JdbcStore() {
        this.url = setting("ISP_DB_URL", "jdbc:postgresql://localhost:5432/isp_project");
        this.username = setting("ISP_DB_USER", "postgres");
        this.password = setting("ISP_DB_PASSWORD", null);
        if (password == null || password.isBlank()) {
            throw new IllegalStateException("Set ISP_DB_PASSWORD in PowerShell before starting the application.");
        }
    }

    private static String setting(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }

    public void initialize() throws SQLException {
        try (Connection connection = connect(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS isp_app_state ("
                    + "state_id SMALLINT PRIMARY KEY CHECK (state_id = 1), "
                    + "payload JSONB NOT NULL, updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP)");
        }
    }

    public String load() throws SQLException {
        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT payload::text FROM isp_app_state WHERE state_id = 1");
             ResultSet result = statement.executeQuery()) {
            return result.next() ? result.getString(1) : null;
        }
    }

    public void save(String json) throws SQLException {
        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO isp_app_state (state_id, payload) VALUES (1, ?::jsonb) "
                             + "ON CONFLICT (state_id) DO UPDATE SET payload = EXCLUDED.payload, "
                             + "updated_at = CURRENT_TIMESTAMP")) {
            statement.setString(1, json);
            statement.executeUpdate();
        }
    }

    public void checkConnection() throws SQLException {
        try (Connection connection = connect(); Statement statement = connection.createStatement()) {
            statement.execute("SELECT 1");
        }
    }
}
