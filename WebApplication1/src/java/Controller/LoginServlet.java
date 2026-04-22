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
        
        if (email != null && email.endsWith("@una.ac.cr") && password != null && !password.isEmpty()) {
            
            HttpSession session = request.getSession();
            session.setAttribute("emailUsuario", email);
            session.setAttribute("rol", email.startsWith("admin") ? "ADMIN" : "CLIENTE");
            
            response.sendRedirect("dashboard.html");
            
        } else {
            response.sendRedirect("index.html?error=invalid_credentials");
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        response.sendRedirect("index.html");
    }
}