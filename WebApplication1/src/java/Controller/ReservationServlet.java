package Controller;

import util.SocketClient;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.time.LocalDate;

@WebServlet("/ReservationServlet")
public class ReservationServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("emailUsuario") == null) {
            response.sendRedirect("index.html");
            return;
        }

        String user = (String) session.getAttribute("emailUsuario");

        String date = request.getParameter("date");
        String start = request.getParameter("start_time");
        String end = request.getParameter("end_time");
        String quantity = request.getParameter("quantity");
        
        String[] equipmentIds = request.getParameterValues("equipment"); 
        String[] equipmentQtys = request.getParameterValues("eqQty");    
        
        String equipmentStr = (equipmentIds != null && equipmentIds.length > 0) 
                              ? String.join(",", equipmentIds) : "";
        String eqQtyStr = (equipmentQtys != null && equipmentQtys.length > 0) 
                          ? String.join(",", equipmentQtys) : "";

        try {
            if (date == null || date.trim().isEmpty()
                    || start == null || start.trim().isEmpty()
                    || end == null || end.trim().isEmpty()
                    || quantity == null || quantity.trim().isEmpty()) {
                response.sendRedirect("dashboard.html?msg=error");
                return;
            }

            LocalDate reservationDate = LocalDate.parse(date);
            LocalDate today = LocalDate.now();

            if (reservationDate.isBefore(today)) {
                response.sendRedirect("dashboard.html?msg=past");
                return;
            }

        } catch (Exception e) {
            response.sendRedirect("dashboard.html?msg=error");
            return;
        }

        String command = String.format(
                "RESERVE;%s;%s;%s;%s;%s;%s;%s",
                user, date, start, end, quantity, equipmentStr, eqQtyStr
        );

        System.out.println("[Servlet] Enviando: " + command);

        String resultado = SocketClient.sendCommand(command);

        if (resultado == null) {
            response.sendRedirect("dashboard.html?msg=server");
            return;
        }

        resultado = resultado.trim().toLowerCase();

        if (resultado.equals("created")) {
            response.sendRedirect("dashboard.html?msg=ok");
        } else {
            String msg = "error";
            if (resultado.equals("busy_time")) msg = "busy";
            if (resultado.equals("busy_capacity")) msg = "busy_capacity";
            if (resultado.equals("busy_equipment")) msg = "busy_equipment";
            if (resultado.equals("hour")) msg = "hour";
            if (resultado.equals("past")) msg = "past";

            response.sendRedirect("dashboard.html?msg=" + msg);
        }
    }
}