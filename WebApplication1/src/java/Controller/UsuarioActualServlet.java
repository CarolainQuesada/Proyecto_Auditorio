package Controller;

import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.WebServlet;

@WebServlet("/usuarioActual")
public class UsuarioActualServlet extends HttpServlet {

    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        HttpSession session = req.getSession(false);

        if (session != null && session.getAttribute("usuario") != null) {
            resp.getWriter().write(session.getAttribute("usuario").toString());
        } else {
            resp.getWriter().write("");
        }
    }
}