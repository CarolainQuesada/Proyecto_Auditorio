package Controller;

import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.WebServlet;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        String email = req.getParameter("email");
        String password = req.getParameter("password");
        
        if (email == null || !email.endsWith("@una.ac.cr")) {
            resp.sendRedirect("index.html?error=correo");
            return;
        }
        
        HttpSession session = req.getSession(true);

        session.setAttribute("usuario", email);

        if ("admin@una.ac.cr".equals(email)) {

            if (!"123".equals(password)) {
                resp.sendRedirect("index.html?error=login");
                return;
            }

            session.setAttribute("rol", "ADMIN");
            resp.sendRedirect("admin.html");

        } else {
            
            session.setAttribute("rol", "CLIENTE");
            resp.sendRedirect("dashboard.html");
        }
    }
}