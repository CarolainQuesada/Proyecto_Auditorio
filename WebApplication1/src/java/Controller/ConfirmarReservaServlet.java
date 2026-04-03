package Controller;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.WebServlet;
import java.io.*;
import java.net.*;

@WebServlet("/confirmarReserva")
public class ConfirmarReservaServlet extends HttpServlet {

    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        String id = req.getParameter("id");

        try {
            Socket socket = new Socket("localhost", 5000);

            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream());

            out.writeUTF("CONFIRMAR;" + id);

            in.readUTF();

            socket.close();

            String rol = (String) req.getSession().getAttribute("rol");

            String pagina = "dashboard.html"; 

            if ("ADMIN".equals(rol)) {
                pagina = "admin.html";
            }

            resp.sendRedirect(pagina + "?msg=confirmada");

        } catch (Exception e) {

            String rol = (String) req.getSession().getAttribute("rol");
            String pagina = "dashboard.html";

            if ("ADMIN".equals(rol)) {
                pagina = "admin.html";
            }

            resp.sendRedirect(pagina + "?msg=server");
        }
    }
}