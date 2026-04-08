package Controller;

import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.WebServlet;
import java.net.*;

@WebServlet("/deleteReservation")
public class DeleteReservationServlet extends HttpServlet {

    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        String id = req.getParameter("id");

        resp.setContentType("text/plain");

        HttpSession session = req.getSession(false);

        if (session == null || session.getAttribute("role") == null) {
            resp.setStatus(401);
            resp.getWriter().write("unauthorized");
            return;
        }

        String role = (String) session.getAttribute("role");

        if (!"ADMIN".equals(role)) {
            resp.setStatus(403);
            resp.getWriter().write("forbidden");
            return;
        }

        try {
            Socket socket = new Socket("localhost", 5000);

            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream());

            out.writeUTF("DELETE;" + id);

            String responseServer = in.readUTF();

            socket.close();

            resp.getWriter().write("ok");

        } catch (Exception e) {
            resp.setStatus(500);
            resp.getWriter().write("error");
        }
    }
}