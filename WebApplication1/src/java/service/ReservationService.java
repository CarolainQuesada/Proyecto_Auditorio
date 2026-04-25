package service;

import concurrency.SystemLog;
import dao.ReservationDAO;
import java.util.ArrayList;
import model.Reservation;
import model.ReservationEquipment;
import java.util.List;

public class ReservationService {

    private final ReservationDAO dao = new ReservationDAO();
    private final SystemLog log = SystemLog.getInstance(); // ← singleton

    public String createReservationWithEquipment(String user, String date, String startTime,
            String endTime, int quantity, List<ReservationEquipment> equipments) {

        if (startTime.compareTo(endTime) >= 0) {
            log.log(user, "RESERVE_REJECT", "Hora inválida: " + startTime + "-" + endTime);
            return "hour";
        }
        if (quantity <= 0 || quantity > 200) {
            log.log(user, "RESERVE_REJECT", "Cantidad inválida: " + quantity);
            return "quantity";
        }
        if (dao.existsOverlap(date, startTime, endTime)) {
            log.log(user, "RESERVE_REJECT", "Horario ocupado: " + date + " " + startTime + "-" + endTime);
            return "busy_time";
        }

        boolean ok = dao.createWithEquipment(user, date, startTime, endTime, quantity, equipments);
        if (ok) {
log.log(user, "RESERVE_OK", "Reserva creada: " + date + " " + startTime + "-" + endTime + " Asistentes: " + quantity);
        } else {
            log.log(user, "RESERVE_ERROR", "Error al crear reserva: " + date + " " + startTime + "-" + endTime);
        }
        return ok ? "created" : "error";
    }

    public String createReservation(String user, String date, String startTime, String endTime,
            int quantity, String equipmentType, int equipmentQty) {

        if (startTime.compareTo(endTime) >= 0) {
            log.log(user, "RESERVE_REJECT", "Hora inválida: " + startTime + "-" + endTime);
            return "hour";
        }
        if (quantity <= 0 || quantity > 200) {
            log.log(user, "RESERVE_REJECT", "Cantidad inválida: " + quantity);
            return "quantity";
        }
        if (dao.existsOverlap(date, startTime, endTime)) {
            log.log(user, "RESERVE_REJECT", "Horario ocupado: " + date + " " + startTime + "-" + endTime);
            return "busy_time";
        }

        boolean ok = dao.create(user, date, startTime, endTime, quantity);
        if (ok) {
            log.log(user, "RESERVE_OK", "Reserva creada: " + date + " " + startTime + "-" + endTime);
        } else {
            log.log(user, "RESERVE_ERROR", "Error al crear reserva: " + date + " " + startTime + "-" + endTime);
        }
        return ok ? "created" : "error";
    }

    public String editReservation(int id, String date, String startTime, String endTime,
            int quantity, String status) {
        try {
            if (startTime.compareTo(endTime) >= 0) {
                log.log("SYSTEM", "EDIT_REJECT", "Hora inválida en edición id=" + id);
                return "hour";
            }
            if (quantity <= 0 || quantity > 200) {
                log.log("SYSTEM", "EDIT_REJECT", "Cantidad inválida en edición id=" + id);
                return "quantity";
            }

            Reservation reservation = dao.getById(id);
            if (reservation == null) {
                log.log("SYSTEM", "EDIT_ERROR", "Reserva no encontrada id=" + id);
                return "error";
            }

            reservation.setDate(date);
            reservation.setStartTime(startTime);
            reservation.setEndTime(endTime);
            reservation.setQuantity(quantity);
            reservation.setStatus(status);

            boolean ok = dao.update(reservation);
 if (ok) {
    log.log(reservation.getUser(), "EDIT_OK",
        "id=" + id + ";fecha=" + date + ";inicio=" + startTime + ";fin=" + endTime + ";asistentes=" + quantity);
} else {
                log.log("SYSTEM", "EDIT_ERROR", "Error al editar id=" + id);
            }
            return ok ? "updated" : "error";

        } catch (Exception e) {
            log.log("SYSTEM", "EDIT_ERROR", "Excepción en edición id=" + id + ": " + e.getMessage());
            e.printStackTrace();
            return "error";
        }
    }

    public String confirmReservation(int id) {
        boolean ok = dao.confirm(id);
        if (ok) {
            log.log("SYSTEM", "CONFIRM_OK", "Reserva confirmada id=" + id);
        } else {
            log.log("SYSTEM", "CONFIRM_ERROR", "Error al confirmar id=" + id);
        }
        return ok ? "confirmed" : "error";
    }

    public String deleteReservation(int id) {
        boolean ok = dao.delete(id);
        if (ok) {
            log.log("SYSTEM", "DELETE_OK", "Reserva eliminada id=" + id);
        } else {
            log.log("SYSTEM", "DELETE_ERROR", "Error al eliminar id=" + id);
        }
        return ok ? "deleted" : "error";
    }

    public String listReservations() {
        List<Reservation> list = dao.getAll();
        StringBuilder sb = new StringBuilder();
        for (Reservation r : list) {
            Reservation full = dao.getByIdWithEquipment(r.getId());
            List<ReservationEquipment> equipments = full.getEquipments();

            String equipStr = "";
            if (!equipments.isEmpty()) {
                List<String> parts = new ArrayList<>();
                for (ReservationEquipment re : equipments) {
                    parts.add(re.getEquipmentId() + ":" + re.getQuantity());
                }
                equipStr = String.join(",", parts);
            }

            sb.append(r.getId()).append(",")
              .append(r.getDate()).append(",")
              .append(r.getStartTime()).append(",")
              .append(r.getEndTime()).append(",")
              .append(r.getQuantity()).append(",")
              .append(r.getStatus()).append(",")
              .append(r.getUser()).append(",")
              .append(equipStr)
              .append("|");
        }
        return sb.toString();
    }
}