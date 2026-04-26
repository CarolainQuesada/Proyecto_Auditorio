package service;

import dao.UserDAO;
import model.User;

public class UserService {

    private final UserDAO dao = new UserDAO();

    private static final String ADMIN_EMAIL = "admin@una.ac.cr";
    private static final String ADMIN_PASSWORD = "123";

    public String loginOrRegisterUser(String email, String password) {

        try {
            if (ADMIN_EMAIL.equalsIgnoreCase(email) && ADMIN_PASSWORD.equals(password)) {
                return "ADMIN";
            }

            User user = dao.login(email, password);

            if (user != null) {
                return normalizeRole(user.getRole());
            }

            User existingUser = dao.findByEmail(email);

            if (existingUser != null) {
                return "ERROR";
            }

            boolean created = dao.createClientUser(email, password);

            if (!created) {
                return "ERROR";
            }

            User newUser = dao.login(email, password);

            if (newUser != null) {
                return normalizeRole(newUser.getRole());
            }

            return "ERROR";

        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR";
        }
    }

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