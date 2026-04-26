package dao;

import model.User;
import util.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Data Access Object for user-related database operations.
 *
 * <p>This DAO provides the three database operations required by
 * {@link service.UserService}:
 * <ul>
 *   <li>Authenticating an existing user by email and password.</li>
 *   <li>Looking up a user record by email alone.</li>
 *   <li>Creating a new user with the {@code CLIENT} role.</li>
 * </ul>
 *
 * <p>Each method opens its own {@link java.sql.Connection} via
 * {@link DBConnection#getConnection()} using try-with-resources.
 *
 * <p><strong>Security note:</strong> passwords are stored and compared in
 * plain text. Consider migrating to a hashed approach (e.g. BCrypt) in a
 * future release.
 *
 * @see service.UserService
 * @see model.User
 */
public class UserDAO {

    /**
     * Authenticates a user by email and password.
     *
     * <p>Executes an exact-match query against the {@code users} table.
     * Returns the full user record on success, or {@code null} if no
     * matching row is found.
     *
     * @param email    the user's email address
     * @param password the plain-text password to verify
     * @return a populated {@link User} object if credentials match;
     *         {@code null} otherwise or on database error
     */
    public User login(String email, String password) {
        String sql = "SELECT * FROM users WHERE email = ? AND password = ?";

        try (
            Connection con = DBConnection.getConnection();
            PreparedStatement ps = con.prepareStatement(sql)
        ) {
            ps.setString(1, email);
            ps.setString(2, password);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    User user = new User();
                    user.setId(rs.getInt("id"));
                    user.setEmail(rs.getString("email"));
                    user.setPassword(rs.getString("password"));
                    user.setRole(rs.getString("role"));
                    return user;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Looks up a user record by email address, ignoring the password.
     *
     * <p>Used to distinguish between "wrong password" (email exists, login
     * failed) and "new user" (email not found) in the auto-registration flow
     * of {@link service.UserService}.
     *
     * @param email the email address to search for
     * @return a populated {@link User} object if the email is registered;
     *         {@code null} if no matching row is found or on database error
     */
    public User findByEmail(String email) {
        String sql = "SELECT * FROM users WHERE email = ?";

        try (
            Connection con = DBConnection.getConnection();
            PreparedStatement ps = con.prepareStatement(sql)
        ) {
            ps.setString(1, email);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    User user = new User();
                    user.setId(rs.getInt("id"));
                    user.setEmail(rs.getString("email"));
                    user.setPassword(rs.getString("password"));
                    user.setRole(rs.getString("role"));
                    return user;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Inserts a new user with the {@code CLIENT} role.
     *
     * <p>This method is called by {@link service.UserService} when an unknown
     * email attempts to log in, triggering automatic self-registration.
     *
     * @param email    the email address for the new account
     * @param password the plain-text password to store
     * @return {@code true} if the row was inserted successfully;
     *         {@code false} on error (e.g. duplicate email)
     */
    public boolean createClientUser(String email, String password) {
        String sql = "INSERT INTO users (email, password, role) VALUES (?, ?, 'CLIENT')";

        try (
            Connection con = DBConnection.getConnection();
            PreparedStatement ps = con.prepareStatement(sql)
        ) {
            ps.setString(1, email);
            ps.setString(2, password);

            return ps.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}