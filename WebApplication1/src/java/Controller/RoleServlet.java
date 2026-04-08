package Controller;

import java.io.IOException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebServlet("/role")
public class RoleServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        resp.setContentType("text/plain;charset=UTF-8");

        HttpSession session = req.getSession(false);

        if (session != null && session.getAttribute("role") != null) {
            resp.getWriter().write(session.getAttribute("role").toString());
        } else {
            resp.getWriter().write("NO_SESSION");
        }
    }
}