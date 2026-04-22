package Controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

@WebServlet("/deleteReservation")
public class DeleteReservationServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        String id = req.getParameter("id");
        resp.setContentType("text/plain;charset=UTF-8");

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

        try (
            Socket socket = new Socket("localhost", 5000);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))
        ) {
            out.println("DELETE;" + id);
            String responseServer = in.readLine();

            if (responseServer != null && !responseServer.toLowerCase().contains("error")) {
                resp.getWriter().write("ok");
            } else {
                resp.setStatus(500);
                resp.getWriter().write("error");
            }

        } catch (Exception e) {
            e.printStackTrace();
            resp.setStatus(500);
            resp.getWriter().write("error");
        }
    }
}