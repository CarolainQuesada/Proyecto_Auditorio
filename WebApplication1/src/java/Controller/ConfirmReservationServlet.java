package Controller;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.WebServlet;
import java.io.*;
import java.net.*;

@WebServlet("/confirmReservation")
public class ConfirmReservationServlet extends HttpServlet {

    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        String id = req.getParameter("id");

        HttpSession session = req.getSession(false);

        if (session == null || session.getAttribute("role") == null) {
            resp.sendRedirect("index.html");
            return;
        }

        String role = (String) session.getAttribute("role");

        if (!"ADMIN".equals(role)) {
            resp.sendRedirect("dashboard.html?msg=unauthorized");
            return;
        }

        try {
            Socket socket = new Socket("localhost", 5000);

            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream());

            out.writeUTF("CONFIRM;" + id);

            in.readUTF();

            socket.close();

            resp.sendRedirect("admin.html?msg=confirmed");

        } catch (Exception e) {
            resp.sendRedirect("admin.html?msg=server");
        }
    }
}