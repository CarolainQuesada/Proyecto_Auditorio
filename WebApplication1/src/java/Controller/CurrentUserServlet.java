package Controller;

import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.WebServlet;

@WebServlet("/currentUser")
public class CurrentUserServlet extends HttpServlet {

    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        resp.setContentType("text/plain");

        HttpSession session = req.getSession(false);

        if (session != null && session.getAttribute("user") != null) {
            resp.getWriter().write(session.getAttribute("user").toString());
        } else {
            resp.getWriter().write("");
        }
    }
}