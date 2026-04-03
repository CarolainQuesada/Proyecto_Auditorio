package Controller;

import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.WebServlet;

@WebServlet("/rol")
public class RolServlet extends HttpServlet {

    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        HttpSession session = req.getSession(false);

        if (session != null && session.getAttribute("rol") != null) {
            resp.getWriter().write(session.getAttribute("rol").toString());
        } else {
            resp.getWriter().write("CLIENTE");
        }
    }
}