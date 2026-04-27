package service;

import dao.UserDAO;
import model.User;

/**
 * Service layer responsible for user authentication and registration rules.
 *
 * <p>This class contains the business logic for the access module. It
 * validates login credentials, verifies whether users already exist, registers
 * new client accounts and normalizes user roles returned from the database.
 *
 * <p>The service works together with {@link UserDAO}, which performs the
 * database operations against the {@code users} table.
 *
 * <p>Main responsibilities:
 * <ul>
 *   <li>Authenticate existing users.</li>
 *   <li>Detect unregistered emails.</li>
 *   <li>Register new users with the {@code CLIENT} role.</li>
 *   <li>Validate institutional email domain {@code @una.ac.cr}.</li>
 *   <li>Validate password and confirmation fields.</li>
 *   <li>Normalize database roles to application roles.</li>
 * </ul>
 *
 * @see UserDAO
 * @see User
 */
public class UserService {

    /**
     * DAO used to query and persist users in the database.
     */
    private final UserDAO dao = new UserDAO();

    /**
     * Authenticates an existing user.
     *
     * <p>This method first verifies whether the email exists in the
     * {@code users} table. If the email is not registered, it returns
     * {@code "NOT_REGISTERED"} so the login page can guide the user to the
     * registration form.
     *
     * <p>If the email exists, the method validates the password using
     * {@link UserDAO#login(String, String)}. When the password is incorrect,
     * it returns {@code "INVALID"}.
     *
     * <p>If authentication succeeds, the user role is normalized and returned.
     *
     * <p>Possible return values:
     * <ul>
     *   <li>{@code "ADMIN"} — authenticated administrator user.</li>
     *   <li>{@code "CLIENT"} — authenticated client user.</li>
     *   <li>{@code "NOT_REGISTERED"} — email does not exist in the database.</li>
     *   <li>{@code "INVALID"} — email exists but password is incorrect.</li>
     *   <li>{@code "ERROR"} — invalid or unknown role.</li>
     * </ul>
     *
     * @param email    institutional email entered in the login form
     * @param password password entered in the login form
     * @return authentication result or normalized user role
     */
    public String loginUser(String email, String password) {
        User existingUser = dao.findByEmail(email);

        if (existingUser == null) {
            return "NOT_REGISTERED";
        }

        User user = dao.login(email, password);

        if (user == null) {
            return "INVALID";
        }

        return normalizeRole(user.getRole());
    }

    /**
     * Registers a new client user.
     *
     * <p>This method validates the registration data received from
     * {@code register.html}. Only institutional emails ending in
     * {@code @una.ac.cr} are accepted. The password and confirmation password
     * must be present, must match, and must meet the minimum length rule.
     *
     * <p>Before creating the account, the method verifies that the email is not
     * already registered. If all validations pass, a new user is inserted into
     * the database with the {@code CLIENT} role.
     *
     * <p>Possible return values:
     * <ul>
     *   <li>{@code "CREATED"} — user was successfully registered.</li>
     *   <li>{@code "EMAIL"} — email is empty or does not belong to {@code @una.ac.cr}.</li>
     *   <li>{@code "PASSWORD"} — password or confirmation password is empty.</li>
     *   <li>{@code "PASSWORD_MATCH"} — password and confirmation do not match.</li>
     *   <li>{@code "PASSWORD_SHORT"} — password length is below the minimum requirement.</li>
     *   <li>{@code "EXISTS"} — email is already registered.</li>
     *   <li>{@code "ERROR"} — database insertion failed.</li>
     * </ul>
     *
     * @param email           institutional email entered in the registration form
     * @param password        password entered in the registration form
     * @param confirmPassword password confirmation entered in the registration form
     * @return registration result code
     */
    public String registerClient(String email, String password, String confirmPassword) {

        if (email == null || email.trim().isEmpty()) {
            return "EMAIL";
        }

        email = email.trim().toLowerCase();

        if (!email.endsWith("@una.ac.cr")) {
            return "EMAIL";
        }

        if (password == null || password.trim().isEmpty()
                || confirmPassword == null || confirmPassword.trim().isEmpty()) {
            return "PASSWORD";
        }

        password = password.trim();
        confirmPassword = confirmPassword.trim();

        if (!password.equals(confirmPassword)) {
            return "PASSWORD_MATCH";
        }

        if (password.length() < 3) {
            return "PASSWORD_SHORT";
        }

        User existingUser = dao.findByEmail(email);

        if (existingUser != null) {
            return "EXISTS";
        }

        boolean created = dao.createClientUser(email, password);

        return created ? "CREATED" : "ERROR";
    }

    /**
     * Normalizes the role value stored in the database.
     *
     * <p>The application works with the roles {@code ADMIN} and {@code CLIENT}.
     * This method also accepts the Spanish value {@code CLIENTE} and converts
     * it to {@code CLIENT}, allowing compatibility with older database records.
     *
     * @param role role value retrieved from the database
     * @return normalized role value, or {@code "ERROR"} if the role is invalid
     */
    private String normalizeRole(String role) {
        if (role == null) {
            return "ERROR";
        }

        role = role.toUpperCase();

        if ("CLIENTE".equals(role)) {
            return "CLIENT";
        }

        if ("ADMIN".equals(role) || "CLIENT".equals(role)) {
            return role;
        }

        return "ERROR";
    }
}