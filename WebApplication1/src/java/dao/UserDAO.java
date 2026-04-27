package dao;

import model.User;
import util.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Data Access Object responsible for all database operations related to users.
 *
 * <p>This class communicates directly with the {@code users} table in the
 * database. It is used by the service layer to authenticate existing users,
 * verify whether an email is already registered, and create new client
 * accounts.
 *
 * <p>The {@code users} table is expected to contain at least the following
 * columns:
 * <ul>
 *   <li>{@code id} — unique identifier of the user.</li>
 *   <li>{@code email} — institutional email address of the user.</li>
 *   <li>{@code password} — user password.</li>
 *   <li>{@code role} — user role, such as {@code ADMIN} or {@code CLIENT}.</li>
 * </ul>
 *
 * <p>This DAO uses prepared statements to avoid SQL injection and obtains
 * database connections through {@link DBConnection}.
 *
 * @see User
 * @see DBConnection
 */
public class UserDAO {

    /**
     * Authenticates a user by verifying the submitted email and password
     * against the {@code users} table.
     *
     * <p>If a matching record is found, the database row is converted into a
     * {@link User} object using {@link #mapUser(ResultSet)}. If no matching
     * record exists, the method returns {@code null}.
     *
     * <p>This method is used during the login process.
     *
     * @param email    the institutional email entered by the user
     * @param password the password entered by the user
     * @return a {@link User} object if the credentials are valid;
     *         {@code null} otherwise
     */
    public User login(String email, String password) {
        String sql = "SELECT id, email, password, role FROM users WHERE email = ? AND password = ?";

        try (
            Connection con = DBConnection.getConnection();
            PreparedStatement ps = con.prepareStatement(sql)
        ) {
            ps.setString(1, email);
            ps.setString(2, password);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapUser(rs);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Searches for a user by email address.
     *
     * <p>This method is commonly used before registration to verify whether
     * an email is already stored in the {@code users} table. It is also used
     * during login to distinguish between an unregistered email and an invalid
     * password.
     *
     * @param email the institutional email to search for
     * @return a {@link User} object if the email exists in the database;
     *         {@code null} if no user is found
     */
    public User findByEmail(String email) {
        String sql = "SELECT id, email, password, role FROM users WHERE email = ?";

        try (
            Connection con = DBConnection.getConnection();
            PreparedStatement ps = con.prepareStatement(sql)
        ) {
            ps.setString(1, email);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapUser(rs);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Creates a new client user in the {@code users} table.
     *
     * <p>New users registered from the public registration form are always
     * created with the {@code CLIENT} role. Administrator users should not be
     * created through this method.
     *
     * <p>This method only performs the database insertion. Business validations,
     * such as checking the institutional email domain or confirming that both
     * passwords match, are handled in the service layer.
     *
     * @param email    the institutional email of the new user
     * @param password the password of the new user
     * @return {@code true} if the user was inserted successfully;
     *         {@code false} if the insertion failed
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

    /**
     * Maps the current row of a {@link ResultSet} into a {@link User} object.
     *
     * <p>This private helper method centralizes the conversion from database
     * fields to the {@code User} model, avoiding duplicated mapping logic in
     * the DAO methods.
     *
     * @param rs the {@link ResultSet} positioned on a valid user row
     * @return a populated {@link User} object
     * @throws Exception if an error occurs while reading values from the result set
     */
    private User mapUser(ResultSet rs) throws Exception {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setEmail(rs.getString("email"));
        user.setPassword(rs.getString("password"));
        user.setRole(rs.getString("role"));
        return user;
    }
}