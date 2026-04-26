package Controller;

import util.SocketClient;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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

            if (start.compareTo(end) >= 0) {
                response.sendRedirect("dashboard.html?msg=hour");
                return;
            }

            int qty = Integer.parseInt(quantity);

            if (qty <= 0 || qty > 200) {
                response.sendRedirect("dashboard.html?msg=quantity");
                return;
            }

        } catch (Exception e) {
            response.sendRedirect("dashboard.html?msg=error");
            return;
        }

        String[] selectedEquipments = request.getParameterValues("equipment");

        List<String> equipmentIds = new ArrayList<>();
        List<String> equipmentQtys = new ArrayList<>();

        if (selectedEquipments != null) {
            for (String equipmentIdStr : selectedEquipments) {
                try {
                    int equipmentId = Integer.parseInt(equipmentIdStr);
                    int maxAllowed = getEquipmentMax(equipmentId);

                    if (maxAllowed == 0) {
                        response.sendRedirect("dashboard.html?msg=busy_equipment");
                        return;
                    }

                    String qtyParam = request.getParameter("eqQty_" + equipmentId);

                    if (qtyParam == null || qtyParam.trim().isEmpty()) {
                        response.sendRedirect("dashboard.html?msg=busy_equipment");
                        return;
                    }

                    int equipmentQuantity = Integer.parseInt(qtyParam.trim());

                    if (equipmentQuantity <= 0 || equipmentQuantity > maxAllowed) {
                        response.sendRedirect("dashboard.html?msg=busy_equipment");
                        return;
                    }

                    equipmentIds.add(String.valueOf(equipmentId));
                    equipmentQtys.add(String.valueOf(equipmentQuantity));

                } catch (NumberFormatException e) {
                    response.sendRedirect("dashboard.html?msg=busy_equipment");
                    return;
                }
            }
        }

        String equipmentStr = String.join(",", equipmentIds);
        String eqQtyStr = String.join(",", equipmentQtys);

        String command = String.format(
                "RESERVE;%s;%s;%s;%s;%s;%s;%s",
                user,
                date,
                start,
                end,
                quantity,
                equipmentStr,
                eqQtyStr
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
            return;
        }

        String msg = "error";

        if (resultado.equals("busy_time")) {
            msg = "busy";
        } else if (resultado.equals("busy_capacity")) {
            msg = "busy_capacity";
        } else if (resultado.equals("busy_equipment")) {
            msg = "busy_equipment";
        } else if (resultado.equals("hour")) {
            msg = "hour";
        } else if (resultado.equals("quantity")) {
            msg = "quantity";
        } else if (resultado.equals("past")) {
            msg = "past";
        }

        response.sendRedirect("dashboard.html?msg=" + msg);
    }

    private int getEquipmentMax(int equipmentId) {
        switch (equipmentId) {
            case 1:
                return 2; // Proyector
            case 2:
                return 5; // Micrófono
            case 3:
                return 3; // Sistema de sonido
            default:
                return 0;
        }
    }
}