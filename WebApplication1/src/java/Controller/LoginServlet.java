package Controller;

import service.UserService;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.IOException;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    private final UserService userService = new UserService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String email = request.getParameter("email");
        String password = request.getParameter("password");

        if (email == null || email.trim().isEmpty()) {
            response.sendRedirect("index.html?error=email");
            return;
        }

        email = email.trim().toLowerCase();

        if (!email.endsWith("@una.ac.cr")) {
            response.sendRedirect("index.html?error=email");
            return;
        }

        if (password == null || password.trim().isEmpty()) {
            response.sendRedirect("index.html?error=login");
            return;
        }

        password = password.trim();

        String role = userService.loginOrRegisterUser(email, password);

        if (role == null || "ERROR".equalsIgnoreCase(role)) {
            response.sendRedirect("index.html?error=login");
            return;
        }

        role = role.toUpperCase();

        HttpSession session = request.getSession(true);
        session.setAttribute("emailUsuario", email);
        session.setAttribute("role", role);

        if ("ADMIN".equals(role)) {
            response.sendRedirect("admin.html");
        } else if ("CLIENT".equals(role)) {
            response.sendRedirect("dashboard.html");
        } else {
            session.invalidate();
            response.sendRedirect("index.html?error=login");
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.sendRedirect("index.html");
    }
}