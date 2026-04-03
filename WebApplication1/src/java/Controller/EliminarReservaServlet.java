package Controller;

import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.WebServlet;
import java.net.*;

@WebServlet("/eliminarReserva")
public class EliminarReservaServlet extends HttpServlet {

    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        String id = req.getParameter("id");

        resp.setContentType("text/plain");

        try {
            Socket socket = new Socket("localhost", 5000);

            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream());

            out.writeUTF("ELIMINAR;" + id);

            String respuesta = in.readUTF();

            socket.close();

            resp.getWriter().write("ok"); 

        } catch (Exception e) {
            resp.setStatus(500);
            resp.getWriter().write("error");
        }
    }
}