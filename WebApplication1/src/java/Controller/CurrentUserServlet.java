package Controller;

import java.io.IOException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

@WebServlet("/currentUser")
public class CurrentUserServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        resp.setContentType("text/plain;charset=UTF-8");

        HttpSession session = req.getSession(false);

        if (session == null || session.getAttribute("emailUsuario") == null) {
            resp.getWriter().write("NO_SESSION");
            return;
        }

        resp.getWriter().write(session.getAttribute("emailUsuario").toString());
    }
}