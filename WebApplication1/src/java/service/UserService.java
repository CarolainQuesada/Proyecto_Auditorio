package service;

import model.User;

public class UserService {

    private final UserDAO dao = new UserDAO();

    private static final String ADMIN_EMAIL = "admin@una.ac.cr";
    private static final String ADMIN_PASSWORD = "123";

    public String loginOrRegisterUser(String email, String password) {

        if (ADMIN_EMAIL.equalsIgnoreCase(email) && ADMIN_PASSWORD.equals(password)) {
            return "ADMIN";
        }

        User user = dao.login(email, password);

        if (user != null) {
            return user.getRole().toUpperCase();
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
            return newUser.getRole().toUpperCase();
        }

        return "ERROR";
    }
}