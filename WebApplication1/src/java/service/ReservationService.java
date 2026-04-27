package service;

import concurrency.CalendarLock;
import concurrency.CapacityControl;
import concurrency.SystemLog;
import dao.ReservationDAO;
import java.util.ArrayList;
import model.Reservation;
import model.ReservationEquipment;
import java.util.List;

/**
 * Service layer responsible for applying business rules related to reservations.
 *
 * <p>This class coordinates reservation operations between the socket layer,
 * the DAO layer and the concurrency utilities. It validates reservation data,
 * checks time conflicts, controls capacity limits, validates audiovisual
 * equipment and records important system events in the audit log.
 *
 * <p>Concurrency is controlled using {@link CalendarLock}. Reservation creation
 * and edition are protected as critical sections to avoid race conditions such
 * as two users reserving the same date and time range simultaneously.
 *
 * <p>Main responsibilities:
 * <ul>
 *   <li>Create reservations with or without equipment.</li>
 *   <li>Edit existing reservations.</li>
 *   <li>Confirm pending reservations.</li>
 *   <li>Delete reservations.</li>
 *   <li>List reservations in a format readable by the admin interface.</li>
 *   <li>Validate equipment quantities and reservation capacity.</li>
 * </ul>
 *
 * @see ReservationDAO
 * @see CalendarLock
 * @see CapacityControl
 * @see SystemLog
 */
public class ReservationService {

    /**
     * DAO used to persist, retrieve, update and delete reservations.
     */
    private final ReservationDAO dao = new ReservationDAO();

    /**
     * Singleton audit log used to register reservation-related events.
     */
    private final SystemLog log = SystemLog.getInstance();

    /**
     * Creates a new reservation with one or more audiovisual equipment items.
     *
     * <p>This method validates the time range, auditorium capacity, equipment
     * quantities and schedule availability before inserting the reservation.
     * The complete operation is protected by {@link CalendarLock} to prevent
     * concurrent double-booking.
     *
     * <p>Possible return values:
     * <ul>
     *   <li>{@code "created"} — reservation was successfully created.</li>
     *   <li>{@code "hour"} — start time is greater than or equal to end time.</li>
     *   <li>{@code "quantity"} — attendee quantity is outside the allowed range.</li>
     *   <li>{@code "busy_equipment"} — equipment is invalid or exceeds allowed limits.</li>
     *   <li>{@code "busy_time"} — another reservation overlaps the requested time range.</li>
     *   <li>{@code "error"} — reservation could not be saved.</li>
     * </ul>
     *
     * @param user       email of the user creating the reservation
     * @param date       reservation date in {@code yyyy-MM-dd} format
     * @param startTime  reservation start time in {@code HH:mm} format
     * @param endTime    reservation end time in {@code HH:mm} format
     * @param quantity   number of attendees requested
     * @param equipments list of equipment items requested for the reservation
     * @return a status code indicating the result of the operation
     */
    public String createReservationWithEquipment(String user, String date, String startTime,
            String endTime, int quantity, List<ReservationEquipment> equipments) {

        CalendarLock.lock();

        try {
            if (startTime.compareTo(endTime) >= 0) {
                log.log(user, "RESERVE_REJECT", "Hora inválida: " + startTime + "-" + endTime);
                return "hour";
            }

            if (!CapacityControl.isValidCapacity(quantity)) {
                log.log(user, "RESERVE_REJECT", "Cantidad inválida: " + quantity);
                return "quantity";
            }

            String equipmentValidation = validateEquipments(equipments);

            if (!"ok".equals(equipmentValidation)) {
                log.log(user, "RESERVE_REJECT", "Equipamiento inválido: " + equipmentValidation);
                return "busy_equipment";
            }

            if (dao.existsOverlap(date, startTime, endTime)) {
                log.log(user, "RESERVE_REJECT",
                        "Horario ocupado: " + date + " " + startTime + "-" + endTime);
                return "busy_time";
            }

            boolean ok = dao.createWithEquipment(user, date, startTime, endTime, quantity, equipments);

            if (ok) {
                log.log(user, "RESERVE_OK",
                        "Reserva creada: " + date + " " + startTime + "-" + endTime
                        + " Asistentes: " + quantity);
            } else {
                log.log(user, "RESERVE_ERROR",
                        "Error al crear reserva: " + date + " " + startTime + "-" + endTime);
            }

            return ok ? "created" : "error";

        } finally {
            CalendarLock.unlock();
        }
    }

    /**
     * Creates a new reservation without using the multi-equipment list.
     *
     * <p>This method is maintained for compatibility with earlier reservation
     * logic. It validates the time range, capacity and schedule availability,
     * then inserts the reservation without equipment records.
     *
     * <p>The operation is protected by {@link CalendarLock} because it checks
     * availability and writes the reservation as a critical section.
     *
     * @param user          email of the user creating the reservation
     * @param date          reservation date in {@code yyyy-MM-dd} format
     * @param startTime     reservation start time in {@code HH:mm} format
     * @param endTime       reservation end time in {@code HH:mm} format
     * @param quantity      number of attendees requested
     * @param equipmentType legacy equipment type parameter
     * @param equipmentQty  legacy equipment quantity parameter
     * @return {@code "created"} if successful, or an error status code otherwise
     */
    public String createReservation(String user, String date, String startTime, String endTime,
            int quantity, String equipmentType, int equipmentQty) {

        CalendarLock.lock();

        try {
            if (startTime.compareTo(endTime) >= 0) {
                log.log(user, "RESERVE_REJECT", "Hora inválida: " + startTime + "-" + endTime);
                return "hour";
            }

            if (!CapacityControl.isValidCapacity(quantity)) {
                log.log(user, "RESERVE_REJECT", "Cantidad inválida: " + quantity);
                return "quantity";
            }

            if (dao.existsOverlap(date, startTime, endTime)) {
                log.log(user, "RESERVE_REJECT",
                        "Horario ocupado: " + date + " " + startTime + "-" + endTime);
                return "busy_time";
            }

            boolean ok = dao.create(user, date, startTime, endTime, quantity);

            if (ok) {
                log.log(user, "RESERVE_OK",
                        "Reserva creada: " + date + " " + startTime + "-" + endTime);
            } else {
                log.log(user, "RESERVE_ERROR",
                        "Error al crear reserva: " + date + " " + startTime + "-" + endTime);
            }

            return ok ? "created" : "error";

        } finally {
            CalendarLock.unlock();
        }
    }

    /**
     * Edits an existing reservation.
     *
     * <p>This method validates the new time range, capacity, reservation status
     * and schedule overlap before updating the reservation. The reservation
     * being edited is excluded from the overlap validation so it does not
     * conflict with itself.
     *
     * <p>The operation is protected by {@link CalendarLock} to avoid concurrent
     * edits or conflicting reservations.
     *
     * @param id        identifier of the reservation to edit
     * @param date      new reservation date
     * @param startTime new start time
     * @param endTime   new end time
     * @param quantity  new attendee quantity
     * @param status    new reservation status
     * @return {@code "updated"} if successful, or an error status code otherwise
     */
    public String editReservation(int id, String date, String startTime, String endTime,
            int quantity, String status) {

        CalendarLock.lock();

        try {
            if (startTime.compareTo(endTime) >= 0) {
                log.log("SYSTEM", "EDIT_REJECT", "Hora inválida en edición id=" + id);
                return "hour";
            }

            if (!CapacityControl.isValidCapacity(quantity)) {
                log.log("SYSTEM", "EDIT_REJECT", "Cantidad inválida en edición id=" + id);
                return "quantity";
            }

            if (dao.existsOverlapExcludingId(id, date, startTime, endTime)) {
                log.log("SYSTEM", "EDIT_REJECT",
                        "Horario ocupado en edición id=" + id + ": "
                        + date + " " + startTime + "-" + endTime);
                return "busy_time";
            }

            Reservation reservation = dao.getById(id);

            if (reservation == null) {
                log.log("SYSTEM", "EDIT_ERROR", "Reserva no encontrada id=" + id);
                return "error";
            }

            if (!"PENDING".equals(status)
                    && !"CONFIRMED".equals(status)
                    && !"EXPIRED".equals(status)) {

                log.log("SYSTEM", "EDIT_REJECT",
                        "Estado inválido en edición id=" + id + ": " + status);
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
                        "id=" + id
                        + ";fecha=" + date
                        + ";inicio=" + startTime
                        + ";fin=" + endTime
                        + ";asistentes=" + quantity);
            } else {
                log.log("SYSTEM", "EDIT_ERROR", "Error al editar id=" + id);
            }

            return ok ? "updated" : "error";

        } catch (Exception e) {
            log.log("SYSTEM", "EDIT_ERROR",
                    "Excepción en edición id=" + id + ": " + e.getMessage());
            e.printStackTrace();
            return "error";

        } finally {
            CalendarLock.unlock();
        }
    }

    /**
     * Confirms a reservation if it is eligible for confirmation.
     *
     * <p>A reservation cannot be confirmed if it does not exist or if it has
     * already expired. If it is already confirmed, the method returns
     * {@code "confirmed"} without modifying it.
     *
     * <p>The DAO confirmation method also validates the TTL window, so a
     * pending reservation that exceeded its allowed confirmation time will not
     * be confirmed.
     *
     * @param id identifier of the reservation to confirm
     * @return {@code "confirmed"}, {@code "expired"} or {@code "error"}
     */
    public String confirmReservation(int id) {
        Reservation reservation = dao.getById(id);

        if (reservation == null) {
            log.log("SYSTEM", "CONFIRM_ERROR", "Reserva no encontrada id=" + id);
            return "error";
        }

        if ("EXPIRED".equalsIgnoreCase(reservation.getStatus())) {
            log.log("SYSTEM", "CONFIRM_REJECT", "No se puede confirmar reserva expirada id=" + id);
            return "expired";
        }

        if ("CONFIRMED".equalsIgnoreCase(reservation.getStatus())) {
            log.log("SYSTEM", "CONFIRM_REJECT", "La reserva ya estaba confirmada id=" + id);
            return "confirmed";
        }

        boolean ok = dao.confirm(id);

        if (ok) {
            log.log("SYSTEM", "CONFIRM_OK", "Reserva confirmada id=" + id);
        } else {
            log.log("SYSTEM", "CONFIRM_ERROR",
                    "No se pudo confirmar id=" + id + ". Puede estar expirada por TTL.");
        }

        return ok ? "confirmed" : "expired";
    }

    /**
     * Deletes an existing reservation.
     *
     * <p>The reservation deletion is delegated to the DAO. Associated equipment
     * records are also removed by the DAO as part of the deletion process.
     *
     * @param id identifier of the reservation to delete
     * @return {@code "deleted"} if successful; otherwise {@code "error"}
     */
    public String deleteReservation(int id) {
        boolean ok = dao.delete(id);

        if (ok) {
            log.log("SYSTEM", "DELETE_OK", "Reserva eliminada id=" + id);
        } else {
            log.log("SYSTEM", "DELETE_ERROR", "Error al eliminar id=" + id);
        }

        return ok ? "deleted" : "error";
    }

    /**
     * Lists all non-expired reservations in a plain text format.
     *
     * <p>The returned string is used by the admin web interface. Each
     * reservation is separated by {@code |}, and each field inside a
     * reservation is separated by commas.
     *
     * <p>Format:
     * <pre>
     * id,date,startTime,endTime,quantity,status,user,equipment|
     * </pre>
     *
     * <p>The equipment field uses the format {@code equipmentId:quantity}.
     * Multiple equipment items are separated by commas.
     *
     * @return formatted string containing all reservations
     */
    public String listReservations() {
        List<Reservation> list = dao.getAll();
        StringBuilder sb = new StringBuilder();

        for (Reservation r : list) {
            Reservation full = dao.getByIdWithEquipment(r.getId());

            List<ReservationEquipment> equipments = new ArrayList<>();

            if (full != null) {
                equipments = full.getEquipments();
            }

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

    /**
     * Validates the list of audiovisual equipment requested for a reservation.
     *
     * <p>The method verifies that each equipment identifier exists, that the
     * requested quantity is positive, and that it does not exceed the maximum
     * allowed inventory for that equipment type.
     *
     * @param equipments list of equipment items to validate
     * @return {@code "ok"} if all equipment data is valid; otherwise a
     *         descriptive validation message
     */
    private String validateEquipments(List<ReservationEquipment> equipments) {
        if (equipments == null || equipments.isEmpty()) {
            return "ok";
        }

        for (ReservationEquipment equipment : equipments) {
            int equipmentId = equipment.getEquipmentId();
            int quantity = equipment.getQuantity();

            int maxAllowed = getEquipmentMax(equipmentId);

            if (maxAllowed == 0) {
                return "Equipo no existe: id=" + equipmentId;
            }

            if (quantity <= 0) {
                return "Cantidad inválida para equipo id=" + equipmentId;
            }

            if (quantity > maxAllowed) {
                return "Cantidad excedida para equipo id=" + equipmentId
                        + ". Máximo permitido: " + maxAllowed;
            }
        }

        return "ok";
    }

    /**
     * Returns the maximum allowed quantity for a specific equipment type.
     *
     * <p>Supported equipment identifiers:
     * <ul>
     *   <li>{@code 1} — Proyector, maximum 2 units.</li>
     *   <li>{@code 2} — Micrófono, maximum 5 units.</li>
     *   <li>{@code 3} — Sistema de sonido, maximum 3 units.</li>
     * </ul>
     *
     * @param equipmentId numeric identifier of the equipment type
     * @return maximum allowed quantity, or {@code 0} if the equipment does not exist
     */
    private int getEquipmentMax(int equipmentId) {
        switch (equipmentId) {
            case 1:
                return 2;
            case 2:
                return 5;
            case 3:
                return 3;
            default:
                return 0;
        }
    }
}