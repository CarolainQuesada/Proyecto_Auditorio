package service;

import dao.ReservationDAO;
import java.util.ArrayList;
import model.Reservation;
import model.ReservationEquipment;
import java.util.List;

public class ReservationService {

    private final ReservationDAO dao = new ReservationDAO();

    public String createReservationWithEquipment(String user, String date, String startTime, String endTime,
    int quantity, List<ReservationEquipment> equipments) {
        
        if (startTime.compareTo(endTime) >= 0) return "hour";
        if (quantity <= 0 || quantity > 200) return "quantity";

        if (dao.existsOverlap(date, startTime, endTime)) {
            return "busy_time";
        }

        boolean ok = dao.createWithEquipment(user, date, startTime, endTime, quantity, equipments);
        return ok ? "created" : "error";
    }

    public String createReservation(String user, String date, String startTime, String endTime,
    int quantity, String equipmentType, int equipmentQty) {
        
        if (startTime.compareTo(endTime) >= 0) return "hour";
        if (quantity <= 0 || quantity > 200) return "quantity";

        if (dao.existsOverlap(date, startTime, endTime)) {
            return "busy_time";
        }

        boolean ok = dao.create(user, date, startTime, endTime, quantity);
        return ok ? "created" : "error";
    }

    public String editReservation(int id, String date, String startTime, String endTime,
    int quantity, String status) {

        try {
            if (startTime.compareTo(endTime) >= 0) return "hour";
            if (quantity <= 0 || quantity > 200) return "quantity";

            Reservation reservation = dao.getById(id);
            if (reservation == null) return "error";

            reservation.setDate(date);
            reservation.setStartTime(startTime);
            reservation.setEndTime(endTime);
            reservation.setQuantity(quantity);
            reservation.setStatus(status);

            boolean ok = dao.update(reservation);
            return ok ? "updated" : "error";

        } catch (Exception e) {
            e.printStackTrace();
            return "error";
        }
    }

    public String confirmReservation(int id) {
        boolean ok = dao.confirm(id);
        return ok ? "confirmed" : "error";
    }

    public String deleteReservation(int id) {
        boolean ok = dao.delete(id);
        return ok ? "deleted" : "error";
    }

public String listReservations() {
    List<Reservation> list = dao.getAll();
    StringBuilder sb = new StringBuilder();

    for (Reservation r : list) {
        // Cargar equipos de esta reserva
        Reservation full = dao.getByIdWithEquipment(r.getId());
        List<ReservationEquipment> equipments = full.getEquipments();
        
        // Construir string de equipos: "1:2,3:1" = (eqId:qty, eqId:qty)
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