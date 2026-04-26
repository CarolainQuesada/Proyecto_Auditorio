package model;

/**
 * Represents an application user stored in the {@code users} database table.
 *
 * <p>Users are identified by their institutional email address
 * ({@code @una.ac.cr}) and are assigned one of two roles:
 * <ul>
 *   <li>{@code "ADMIN"} — can view all reservations, confirm, edit, and
 *       manage equipment.</li>
 *   <li>{@code "CLIENT"} — can create their own reservations.</li>
 * </ul>
 *
 * <p><b>Security note:</b> Passwords are currently stored and compared as
 * plain text. In a production system they should be hashed with a strong
 * algorithm (e.g., bcrypt).
 *
 * <p>This is a plain JavaBean with no business logic.
 */
public class User {

    /**
     * Auto-incremented primary key that uniquely identifies this user.
     */
    private int id;

    /**
     * Institutional email address used as the login identifier.
     * Must end with {@code @una.ac.cr}.
     */
    private String email;

    /**
     * User's password (stored as plain text — see security note in class
     * Javadoc).
     */
    private String password;

    /**
     * The user's role in the system. Valid values: {@code "ADMIN"},
     * {@code "CLIENT"}.
     */
    private String role;

    /**
     * Returns the primary key of this user.
     *
     * @return the user ID
     */
    public int getId() { return id; }

    /**
     * Sets the primary key of this user.
     *
     * @param id the user ID
     */
    public void setId(int id) { this.id = id; }

    /**
     * Returns the email address of this user.
     *
     * @return the institutional email
     */
    public String getEmail() { return email; }

    /**
     * Sets the email address of this user.
     *
     * @param email the institutional email
     */
    public void setEmail(String email) { this.email = email; }

    /**
     * Returns the password of this user.
     *
     * @return the plain-text password
     */
    public String getPassword() { return password; }

    /**
     * Sets the password of this user.
     *
     * @param password the plain-text password
     */
    public void setPassword(String password) { this.password = password; }

    /**
     * Returns the role of this user.
     *
     * @return {@code "ADMIN"} or {@code "CLIENT"}
     */
    public String getRole() { return role; }

    /**
     * Sets the role of this user.
     *
     * @param role the role value
     */
    public void setRole(String role) { this.role = role; }
}