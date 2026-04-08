package service;

import concurrency.CalendarLock;
import concurrency.CapacityControl;
import java.time.LocalDate;
import java.util.List;
import model.Reservation;

public class ReservationService {

    private final ReservationDAO dao = new ReservationDAO();

    public String createReservation(String user, String date, String startTime, String endTime, int quantity) {

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
                if (startTime.compareTo(r.getEndTime()) < 0 &&
                    endTime.compareTo(r.getStartTime()) > 0) {
                    return "busy";
                }
            }

            if (!CapacityControl.acquire(quantity)) {
                return "busy";
            }

            dao.create(user, date, startTime, endTime, quantity);

            return "created";

        } catch (Exception e) {
            e.printStackTrace();
            return "error";
        } finally {
            CalendarLock.unlock();
        }
    }

 public String editReservation(int id, String date, String startTime, String endTime, int quantity) {

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
        Reservation actual = dao.getById(id);

        if (actual == null) {
            return "error";
        }

        List<Reservation> existingReservations = dao.getByDate(date);

        for (Reservation r : existingReservations) {
            if (r.getId() == id) {
                continue;
            }

            if (startTime.compareTo(r.getEndTime()) < 0 &&
                endTime.compareTo(r.getStartTime()) > 0) {
                return "busy";
            }
        }

        int diferencia = quantity - actual.getQuantity();

        if (diferencia > 0) {
            if (!CapacityControl.acquire(diferencia)) {
                return "busy";
            }
        } else if (diferencia < 0) {
            CapacityControl.release(-diferencia);
        }

        actual.setDate(date);
        actual.setStartTime(startTime);
        actual.setEndTime(endTime);
        actual.setQuantity(quantity);

        boolean updated = dao.update(actual);

        return updated ? "updated" : "error";

    } catch (Exception e) {
        e.printStackTrace();
        return "error";
    } finally {
        CalendarLock.unlock();
    }
}

    public String confirmReservation(int id) {
        dao.confirm(id);
        return "confirmed";
    }

    public String deleteReservation(int id) {

        CalendarLock.lock();

        try {
            Reservation reservation = dao.getById(id);

            if (reservation != null) {
                CapacityControl.release(reservation.getQuantity());
                dao.delete(id);
            }

            return "deleted";

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
}