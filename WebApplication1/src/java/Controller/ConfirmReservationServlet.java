package Controller;

import javax.servlet.http.*;
import javax.servlet.annotation.WebServlet;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

@WebServlet("/confirmReservation")
public class ConfirmReservationServlet extends HttpServlet {

    @Override
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

        try (
            Socket socket = new Socket("localhost", 5000);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))
        ) {
            out.println("CONFIRM;" + id);

            String responseServer = in.readLine();

            if (responseServer != null && responseServer.toLowerCase().contains("confirm")) {
                resp.sendRedirect("admin.html?msg=confirmed");
            } else {
                resp.sendRedirect("admin.html?msg=error");
            }

        } catch (Exception e) {
            e.printStackTrace();
            resp.sendRedirect("admin.html?msg=server");
        }
    }
}