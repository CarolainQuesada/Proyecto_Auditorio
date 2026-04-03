package Controller;

import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.WebServlet;
import java.net.*;

@WebServlet("/listarReservas")
public class ListarReservasServlet extends HttpServlet {

    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        try {
            Socket socket = new Socket("localhost", 5000);

            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream());

            out.writeUTF("LISTAR");

            String respuesta = in.readUTF();

            socket.close();

            resp.setContentType("text/plain");
            resp.getWriter().write(respuesta);

        } catch (Exception e) {
            resp.getWriter().write("");
        }
    }
}