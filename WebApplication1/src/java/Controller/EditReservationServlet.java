package Controller;

import javax.servlet.http.*;
import javax.servlet.annotation.WebServlet;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.LocalDate;

@WebServlet("/editReservation")
public class EditReservationServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("emailUsuario") == null || session.getAttribute("role") == null) {
            response.sendRedirect("index.html");
            return;
        }

        String role = (String) session.getAttribute("role");

        if (!"ADMIN".equals(role)) {
            response.sendRedirect("dashboard.html?msg=unauthorized");
            return;
        }

        String id = request.getParameter("id");
        String date = request.getParameter("date");
        String startTime = request.getParameter("start_time");
        String endTime = request.getParameter("end_time");
        String quantity = request.getParameter("quantity");
        String status = request.getParameter("status");

        try {
            if (id == null || id.trim().isEmpty()
                    || date == null || date.trim().isEmpty()
                    || startTime == null || startTime.trim().isEmpty()
                    || endTime == null || endTime.trim().isEmpty()
                    || quantity == null || quantity.trim().isEmpty()
                    || status == null || status.trim().isEmpty()) {
                response.sendRedirect("admin.html?msg=error");
                return;
            }

            LocalDate reservationDate = LocalDate.parse(date);
            LocalDate today = LocalDate.now();

            if (reservationDate.isBefore(today)) {
                response.sendRedirect("admin.html?msg=past");
                return;
            }

            try (
                Socket socket = new Socket("localhost", 5000);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))
            ) {
                String command = "EDIT;" + id + ";" + date + ";" + startTime + ";" + endTime + ";" + quantity + ";" + status;

                out.println(command);
                String responseServer = in.readLine();

                if (responseServer == null) {
                    response.sendRedirect("admin.html?msg=server");
                } else if (responseServer.contains("updated")) {
                    response.sendRedirect("admin.html?msg=ok");
                } else if (responseServer.contains("hour")) {
                    response.sendRedirect("admin.html?msg=hour");
                } else if (responseServer.contains("quantity")) {
                    response.sendRedirect("admin.html?msg=quantity");
                } else if (responseServer.contains("busy")) {
                    response.sendRedirect("admin.html?msg=busy");
                } else if (responseServer.contains("past")) {
                    response.sendRedirect("admin.html?msg=past");
                } else {
                    response.sendRedirect("admin.html?msg=error");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect("admin.html?msg=server");
        }
    }
}