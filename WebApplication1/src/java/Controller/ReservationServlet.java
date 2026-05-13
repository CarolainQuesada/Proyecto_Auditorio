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
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.sendRedirect("index.html");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String redirectPage = "dashboard.html";
        String source = request.getParameter("source");
        if ("admin".equals(source)) {
            redirectPage = "admin.html";
        }
        
        String usuario = (String) request.getSession().getAttribute("emailUsuario");
        if (usuario == null) {
            response.sendRedirect("index.html?msg=session_expired");
            return;
        }

        String action = request.getParameter("action");
        System.out.println("[Servlet] Action recibida: [" + action + "]"); // 🔍 DEBUG

        if ("reservar".equals(action)) {
            String fecha = request.getParameter("date");
            String horaInicio = request.getParameter("start_time");
            String horaFin = request.getParameter("end_time");
            String cantidadStr = request.getParameter("quantity");

            if (fecha == null || horaInicio == null || horaFin == null || cantidadStr == null) {
                System.err.println("[Servlet] Faltan parámetros requeridos");
                response.sendRedirect(redirectPage + "?msg=error_params");
                return;
            }

            try {
                LocalDate fechaReserva = LocalDate.parse(fecha);
                if (fechaReserva.isBefore(LocalDate.now())) {
                    response.sendRedirect(redirectPage + "?msg=past");
                    return;
                }
            } catch (Exception e) {
                response.sendRedirect(redirectPage + "?msg=error_date");
                return;
            }

            if (horaInicio.compareTo(horaFin) >= 0) {
                response.sendRedirect(redirectPage + "?msg=hour");
                return;
            }

            int cantidad;
            try {
                cantidad = Integer.parseInt(cantidadStr);
                if (cantidad < 1 || cantidad > 200) {
                    response.sendRedirect(redirectPage + "?msg=quantity");
                    return;
                }
            } catch (NumberFormatException e) {
                response.sendRedirect(redirectPage + "?msg=quantity");
                return;
            }

            
            String[] equipmentIds = request.getParameterValues("equipment");
            StringBuilder eqIds = new StringBuilder();
            StringBuilder eqQtys = new StringBuilder();

            if (equipmentIds != null) {
                for (String eqId : equipmentIds) {
                    String qtyParam = "eqQty_" + eqId;
                    String qtyStr = request.getParameter(qtyParam);
                    if (qtyStr != null) {
                        try {
                            int qty = Integer.parseInt(qtyStr);
                            int max = getEquipmentMax(Integer.parseInt(eqId));
                            if (qty >= 1 && qty <= max) {
                                if (eqIds.length() > 0) { eqIds.append(","); eqQtys.append(","); }
                                eqIds.append(eqId);
                                eqQtys.append(qty);
                            }
                        } catch (Exception ignored) {}
                    }
                }
            }

            String command = String.format("RESERVE;%s;%s;%s;%s;%d;%s;%s",
                    usuario, fecha, horaInicio, horaFin, cantidad,
                    eqIds.toString(), eqQtys.toString());

            System.out.println("\n" + "=".repeat(60));
            System.out.println("🔍 DEBUG SERVLET - COMANDO A ENVIAR");
            System.out.println("Usuario: [" + usuario + "]");
            System.out.println("Fecha: [" + fecha + "]");
            System.out.println("Hora Inicio: [" + horaInicio + "]");
            System.out.println("Hora Fin: [" + horaFin + "]");
            System.out.println("Cantidad: [" + cantidad + "]");
            System.out.println("Eq IDs: [" + eqIds.toString() + "]");
            System.out.println("Eq Qtys: [" + eqQtys.toString() + "]");
            System.out.println("COMANDO RAW: [" + command + "]");
            System.out.println("=".repeat(60) + "\n");

            try {
                String resultado = SocketClient.sendCommand(command);
                System.out.println("✅ RESPUESTA DEL SERVER: [" + resultado + "]");

                String msg = "error";
                if ("created".equals(resultado)) msg = "ok";
                else if ("busy_time".equals(resultado)) msg = "busy_time";
                else if ("busy_equipment".equals(resultado)) msg = "busy_equipment";
                else if ("busy_capacity".equals(resultado)) msg = "busy_capacity";
                else if ("blocked_date".equals(resultado)) msg = "blocked_date";
                else if ("quantity".equals(resultado)) msg = "quantity";
                else if ("hour".equals(resultado)) msg = "hour";
                else if ("past".equals(resultado)) msg = "past";
                
                response.sendRedirect(redirectPage + "?msg=" + msg);

            } catch (Exception e) {
                System.err.println("[Servlet] ERROR en SocketClient: " + e.getMessage());
                e.printStackTrace();
                response.sendRedirect(redirectPage + "?msg=server_error");
            }

        } else if ("editar".equals(action) || "eliminar".equals(action)) {
            response.sendRedirect("admin.html?msg=ok");
        } else {
            System.err.println("[Servlet] Acción desconocida: [" + action + "]");
            response.sendRedirect(redirectPage + "?msg=unknown_action");
        }
    }

    private int getEquipmentMax(int equipmentId) {
        switch (equipmentId) {
            case 1: return 2;
            case 2: return 5;
            case 3: return 3;
            default: return 0;
        }
    }
}