package Controller;

import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.WebServlet;

@WebServlet("/CurrentUserServlet")  
public class CurrentUserServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        
        resp.setContentType("text/plain");
        resp.setCharacterEncoding("UTF-8");

        HttpSession session = req.getSession(false);

        if (session != null && session.getAttribute("emailUsuario") != null) {
            resp.getWriter().write(session.getAttribute("emailUsuario").toString());
        } else {
            resp.getWriter().write("");
        }
    }
}