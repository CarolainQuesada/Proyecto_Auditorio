package Controller;

import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.WebServlet;
import java.net.*;

@WebServlet("/listReservations")
public class ListReservationsServlet extends HttpServlet {

    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        try {
            Socket socket = new Socket("localhost", 5000);

            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream());

            out.writeUTF("LIST");

            String responseServer = in.readUTF();

            socket.close();

            resp.setContentType("text/plain");
            resp.getWriter().write(responseServer);

        } catch (Exception e) {
            resp.getWriter().write("");
        }
    }
}