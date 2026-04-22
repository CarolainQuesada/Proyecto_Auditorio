package Controller;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.IOException;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String email = request.getParameter("email");
        String password = request.getParameter("password");

        if (email == null || email.isEmpty() || !email.endsWith("@una.ac.cr")) {
            response.sendRedirect("index.html?error=email");
            return;
        }

        if (password == null || password.isEmpty()) {
            response.sendRedirect("index.html?error=login");
            return;
        }

        HttpSession session = request.getSession();
        session.setAttribute("emailUsuario", email);

        String role = email.startsWith("admin") ? "ADMIN" : "CLIENTE";
        session.setAttribute("role", role);

        if ("ADMIN".equals(role)) {
            response.sendRedirect("admin.html");
        } else {
            response.sendRedirect("dashboard.html");
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.sendRedirect("index.html");
    }
}