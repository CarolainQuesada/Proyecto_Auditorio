package dao;

import util.DBConnection;
import model.Log;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object (DAO) for the system audit log ({@code log} table).
 *
 * <p>Handles all persistence operations for {@link model.Log} entries in the
 * MySQL database. Synchronization of concurrent write access is managed at
 * a higher level by {@link concurrency.SystemLog}; this class only guarantees
 * correct, isolated database interactions.
 *
 * <p>All methods print a descriptive error to {@code System.err} on SQL
 * failure and return a safe default value ({@code null} or an empty list)
 * rather than propagating exceptions, so callers are never required to handle
 * checked SQL exceptions.
 *
 * <p>Database table: {@code log}
 * <pre>
 * +-------------+--------------+
 * | Column      | Type         |
 * +-------------+--------------+
 * | id          | INT PK AUTO  |
 * | user        | VARCHAR      |
 * | action      | VARCHAR      |
 * | description | TEXT         |
 * | created_at  | DATETIME     |
 * +-------------+--------------+
 * </pre>
 *
 * @see concurrency.SystemLog
 * @see model.Log
 */
public class LogDAO {

    /**
     * Inserts a new audit log entry into the {@code log} table with the
     * current timestamp.
     *
     * <p>If any of the parameters is {@code null}, a safe default is used:
     * {@code "SYSTEM"} for {@code user}, {@code "UNKNOWN"} for {@code action},
     * and an empty string for {@code description}.
     *
     * @param user        the email or identifier of the user who triggered
     *                    the event; may be {@code null}
     * @param action      a short action code (e.g. {@code "RESERVE_OK"},
     *                    {@code "TTL_EXPIRED"}); may be {@code null}
     * @param description a human-readable description of the event;
     *                    may be {@code null}
     */
    public void register(String user, String action, String description) {
        String sql = "INSERT INTO log (user, action, description, created_at) VALUES (?, ?, ?, NOW())";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, user != null ? user : "SYSTEM");
            ps.setString(2, action != null ? action : "UNKNOWN");
            ps.setString(3, description != null ? description : "");
            ps.executeUpdate();

        } catch (Exception e) {
            System.err.println("[LogDAO ERROR] register: " + e.getMessage());
        }
    }

    /**
     * Retrieves all audit log entries ordered by {@code created_at} descending
     * (most recent first).
     *
     * @return a {@link List} of {@link model.Log} objects representing every
     *         entry in the {@code log} table; never {@code null}, but may be
     *         empty if the table contains no records or a SQL error occurs
     */
    public List<Log> getAll() {
        List<Log> list = new ArrayList<>();
        String sql = "SELECT * FROM log ORDER BY created_at DESC";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(map(rs));
            }

        } catch (SQLException e) {
            System.err.println("[LogDAO ERROR] getAll: " + e.getMessage());
        }
        return list;
    }

    /**
     * Retrieves all audit log entries for a specific user, ordered by
     * {@code created_at} descending.
     *
     * @param user the exact email or identifier to filter by; must not be
     *             {@code null}
     * @return a {@link List} of {@link model.Log} objects matching the given
     *         user; never {@code null}, but may be empty if no records match
     *         or a SQL error occurs
     */
    public List<Log> getByUser(String user) {
        List<Log> list = new ArrayList<>();
        String sql = "SELECT * FROM log WHERE user = ? ORDER BY created_at DESC";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, user);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }

        } catch (SQLException e) {
            System.err.println("[LogDAO ERROR] getByUser: " + e.getMessage());
        }
        return list;
    }

    /**
     * Retrieves all audit log entries for a specific action code, ordered by
     * {@code created_at} descending.
     *
     * @param action the exact action code to filter by (e.g.
     *               {@code "RESERVE_OK"}, {@code "TTL_EXPIRED"});
     *               must not be {@code null}
     * @return a {@link List} of {@link model.Log} objects matching the given
     *         action; never {@code null}, but may be empty if no records match
     *         or a SQL error occurs
     */
    public List<Log> getByAction(String action) {
        List<Log> list = new ArrayList<>();
        String sql = "SELECT * FROM log WHERE action = ? ORDER BY created_at DESC";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, action);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }

        } catch (SQLException e) {
            System.err.println("[LogDAO ERROR] getByAction: " + e.getMessage());
        }
        return list;
    }

    /**
     * Maps a single {@link ResultSet} row to a {@link model.Log} instance.
     *
     * @param rs the {@link ResultSet} positioned at the row to map;
     *           must not be {@code null}
     * @return a fully populated {@link model.Log} object
     * @throws SQLException if any column cannot be read from the result set
     */
    private Log map(ResultSet rs) throws SQLException {
        Log log = new Log();
        log.setId(rs.getInt("id"));
        log.setUser(rs.getString("user"));
        log.setAction(rs.getString("action"));
        log.setDescription(rs.getString("description"));
        log.setCreatedAt(rs.getString("created_at"));
        return log;
    }
}