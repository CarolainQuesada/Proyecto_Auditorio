package service;

import dao.ReservationDAO;
import concurrency.CalendarLock;
import concurrency.CapacityControl;
import java.time.LocalDate;
import java.util.List;
import model.Reservation;

public class ReservationService {

    private final ReservationDAO dao = new ReservationDAO();
    private final EquipmentService equipmentService = new EquipmentService();

    public ReservationService() {
    }

    public String createReservation(String user, String date, String startTime, String endTime, 
                                    int quantity, String equipmentType, int equipmentQty) {

        if (date == null || date.isEmpty()) {
            return "error_date";
        }

        try {
            LocalDate reservationDate = LocalDate.parse(date);
            LocalDate today = LocalDate.now();
            if (reservationDate.isBefore(today)) {
                return "past";
            }
        } catch (Exception e) {
            return "error_date";
        }

        if (startTime.compareTo(endTime) >= 0) {
            return "hour";
        }

        if (quantity <= 0) {
            return "quantity";
        }

        CalendarLock.lock();

        try {
            List<Reservation> existingReservations = dao.getByDate(date);
            for (Reservation r : existingReservations) {
                if ("EXPIRED".equalsIgnoreCase(r.getStatus()) || 
                    "CANCELLED".equalsIgnoreCase(r.getStatus())) {
                    continue;
                }
                if (startTime.compareTo(r.getEndTime()) < 0 && 
                    endTime.compareTo(r.getStartTime()) > 0) {
                    return "busy_time";
                }
            }

            if (!CapacityControl.acquire(quantity)) {
                return "busy_capacity";
            }

            boolean equipmentReserved = false;
            if (!"NINGUNO".equalsIgnoreCase(equipmentType) && equipmentQty > 0) {
                if (!equipmentService.reserveEquipment(equipmentType, equipmentQty, "PENDING")) {
                    CapacityControl.release(quantity);
                    return "busy_equipment";
                }
                equipmentReserved = true;
            }

            boolean saved = dao.create(user, date, startTime, endTime, quantity);
            
            if (!saved) {
                CapacityControl.release(quantity);
                if (equipmentReserved) {
                    equipmentService.releaseEquipment(equipmentType, equipmentQty, "PENDING");
                }
                return "error_db";
            }

            return "created";

        } catch (Exception e) {
            CapacityControl.release(quantity);
            e.printStackTrace();
            return "error";
        } finally {
            CalendarLock.unlock();
        }
    }

    public String editReservation(int id, String date, String startTime, String endTime, int quantity) {

        if (date == null || date.isEmpty()) return "error_date";

        try {
            if (LocalDate.parse(date).isBefore(LocalDate.now())) return "past";
        } catch (Exception e) {
            return "error_date";
        }

        if (startTime.compareTo(endTime) >= 0) return "hour";
        if (quantity <= 0) return "quantity";

        CalendarLock.lock();

        try {
            Reservation actual = dao.getById(id);
            if (actual == null) {
                return "not_found";
            }

            List<Reservation> existingReservations = dao.getByDate(date);
            for (Reservation r : existingReservations) {
                if (r.getId() == id) continue;
                if ("EXPIRED".equalsIgnoreCase(r.getStatus()) || 
                    "CANCELLED".equalsIgnoreCase(r.getStatus())) {
                    continue;
                }
                if (startTime.compareTo(r.getEndTime()) < 0 && 
                    endTime.compareTo(r.getStartTime()) > 0) {
                    return "busy_time";
                }
            }
            int diferencia = quantity - actual.getQuantity();
            if (diferencia > 0) {
                if (!CapacityControl.acquire(diferencia)) {
                    return "busy_capacity";
                }
            } else if (diferencia < 0) {
                CapacityControl.release(-diferencia);
            }

            actual.setDate(date);
            actual.setStartTime(startTime);
            actual.setEndTime(endTime);
            actual.setQuantity(quantity);

            boolean updated = dao.update(actual);
            return updated ? "updated" : "error_db";

        } catch (Exception e) {
            e.printStackTrace();
            return "error";
        } finally {
            CalendarLock.unlock();
        }
    }

    public String confirmReservation(int id) {
        try {
            boolean confirmed = dao.confirm(id);
            return confirmed ? "confirmed" : "not_found";
        } catch (Exception e) {
            e.printStackTrace();
            return "error";
        }
    }

    public String deleteReservation(int id) {
        CalendarLock.lock();

        try {
            Reservation reservation = dao.getById(id);
            if (reservation == null) {
                return "not_found";
            }

            CapacityControl.release(reservation.getQuantity());

            // Liberar equipamiento si la reserva tenía (ajustar según tu modelo)
            // if (reservation.getEquipmentType() != null && !reservation.getEquipmentType().equals("NINGUNO")) {
            //     equipmentService.releaseEquipment(
            //         reservation.getEquipmentType(), 
            //         reservation.getEquipmentQty(), 
            //         String.valueOf(id)
            //     );
            // }

            boolean deleted = dao.delete(id);
            return deleted ? "deleted" : "error_db";

        } finally {
            CalendarLock.unlock();
        }
    }

    public String listReservations() {
        StringBuilder sb = new StringBuilder();
        List<Reservation> list = dao.getAll();

        for (Reservation r : list) {
            sb.append(r.getId()).append(",")
              .append(r.getDate()).append(",")
              .append(r.getStartTime()).append(",")
              .append(r.getEndTime()).append(",")
              .append(r.getQuantity()).append(",")
              .append(r.getStatus()).append(",")
              .append(r.getUser())
              .append("|");
        }
        return sb.toString();
    }

    public Reservation getReservation(int id) {
        return dao.getById(id);
    }

    public void cleanExpiredReservations(int ttlMinutes) {
        // Nota: Esta operación NO requiere CalendarLock porque trabaja sobre
        // reservas que ya deberían estar "abandonadas" (PENDING viejas).
        // Pero si quieres máxima seguridad, puedes envolverlo en lock.
        
        List<Reservation> pending = dao.getAll(); 
        
        for (Reservation r : pending) {
            if ("PENDING".equalsIgnoreCase(r.getStatus())) {
                // Aquí verificarías si created_at + TTL < now
                // Por simplicidad, delegamos en el DAO
            }
        }
        dao.cleanExpired(ttlMinutes);
        
        // Nota: Para liberar recursos de reservas expiradas, necesitarías
        // consultar cuáles fueron expiradas y liberar capacidad/equipo.
        // Dependiendo de si DAO retorna los IDs expirados.
    }
}