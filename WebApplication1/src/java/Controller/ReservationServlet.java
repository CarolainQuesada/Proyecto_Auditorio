package Controller;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.WebServlet;
import java.io.*;
import java.net.*;
import java.time.LocalDate;

@WebServlet("/reserve")
public class ReservationServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("user") == null) {
            response.sendRedirect("index.html");
            return;
        }

        String user = (String) session.getAttribute("user");
        String role = (String) session.getAttribute("role");

        String date = request.getParameter("date");
        String startTime = request.getParameter("start_time");
        String endTime = request.getParameter("end_time");
        String quantity = request.getParameter("quantity");

        String page = "dashboard.html";
        if ("ADMIN".equals(role)) {
            page = "admin.html";
        }

        try {
            if (date == null || date.trim().isEmpty()) {
                response.sendRedirect(page + "?msg=error");
                return;
            }

            LocalDate reservationDate = LocalDate.parse(date);
            LocalDate today = LocalDate.now();

            if (reservationDate.isBefore(today)) {
                response.sendRedirect(page + "?msg=past");
                return;
            }

            Socket socket = new Socket("localhost", 5000);

            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream());

            String command = "RESERVE;" + user + ";" + date + ";" + startTime + ";" + endTime + ";" + quantity;

            out.writeUTF(command);
            out.flush();

            String responseServer = in.readUTF();

            socket.close();

            if (responseServer.contains("created")) {
                response.sendRedirect(page + "?msg=ok");
            } else if (responseServer.contains("hour")) {
                response.sendRedirect(page + "?msg=hour");
            } else if (responseServer.contains("quantity")) {
                response.sendRedirect(page + "?msg=quantity");
            } else if (responseServer.contains("busy")) {
                response.sendRedirect(page + "?msg=busy");
            } else {
                response.sendRedirect(page + "?msg=error");
            }

        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect(page + "?msg=server");
        }
    }
}