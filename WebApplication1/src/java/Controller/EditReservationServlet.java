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

        if (session == null
                || session.getAttribute("emailUsuario") == null
                || session.getAttribute("role") == null) {

            response.sendRedirect("index.html");
            return;
        }

        String role = session.getAttribute("role").toString();

        if (!"ADMIN".equalsIgnoreCase(role)) {
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

            id = id.trim();
            date = date.trim();
            startTime = startTime.trim();
            endTime = endTime.trim();
            quantity = quantity.trim();
            status = status.trim().toUpperCase();

            if (!"PENDING".equals(status)
                    && !"CONFIRMED".equals(status)
                    && !"EXPIRED".equals(status)) {

                response.sendRedirect("admin.html?msg=error");
                return;
            }

            int qty = Integer.parseInt(quantity);

            if (qty <= 0 || qty > 200) {
                response.sendRedirect("admin.html?msg=quantity");
                return;
            }

            LocalDate reservationDate = LocalDate.parse(date);
            LocalDate today = LocalDate.now();

            if (reservationDate.isBefore(today)) {
                response.sendRedirect("admin.html?msg=past");
                return;
            }

            if (startTime.compareTo(endTime) >= 0) {
                response.sendRedirect("admin.html?msg=hour");
                return;
            }

            try (
                Socket socket = new Socket("localhost", 5000);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))
            ) {
                String command = "EDIT;"
                        + id + ";"
                        + date + ";"
                        + startTime + ";"
                        + endTime + ";"
                        + quantity + ";"
                        + status;

                out.println(command);

                String responseServer = in.readLine();

                if (responseServer == null) {
                    response.sendRedirect("admin.html?msg=server");
                    return;
                }

                String result = responseServer.trim().toLowerCase();

                if (result.contains("updated")) {
                    response.sendRedirect("admin.html?msg=ok");
                } else if (result.contains("hour")) {
                    response.sendRedirect("admin.html?msg=hour");
                } else if (result.contains("quantity")) {
                    response.sendRedirect("admin.html?msg=quantity");
                } else if (result.contains("busy_time") || result.contains("busy")) {
                    response.sendRedirect("admin.html?msg=busy");
                } else if (result.contains("past")) {
                    response.sendRedirect("admin.html?msg=past");
                } else {
                    response.sendRedirect("admin.html?msg=error");
                }
            }

        } catch (NumberFormatException e) {
            response.sendRedirect("admin.html?msg=quantity");

        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect("admin.html?msg=server");
        }
    }
}