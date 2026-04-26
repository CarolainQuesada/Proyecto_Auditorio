package model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an auditorium reservation made by a user.
 *
 * <p>Each {@code Reservation} mirrors a row in the {@code reservations} database
 * table and may optionally include a list of {@link ReservationEquipment} items
 * describing the equipment requested for that event.
 *
 * <p>Lifecycle of a reservation:
 * <ol>
 *   <li><b>PENDING</b> — just created; must be confirmed within 10 minutes.</li>
 *   <li><b>CONFIRMED</b> — confirmed by an administrator.</li>
 *   <li><b>EXPIRED</b> — automatically set by the TTL monitor if the reservation
 *       was not confirmed within the configured time window.</li>
 * </ol>
 *
 * <p>This is a JavaBean enriched with a convenience method
 * ({@link #addEquipment(ReservationEquipment)}) for building the equipment list.
 */
public class Reservation {

    /**
     * Auto-incremented primary key that uniquely identifies this reservation.
     */
    private int id;

    /**
     * Email of the user who created the reservation.
     */
    private String user;

    /**
     * Date of the event in {@code yyyy-MM-dd} format.
     */
    private String date;

    /**
     * Start time of the event in {@code HH:mm} format.
     */
    private String startTime;

    /**
     * End time of the event in {@code HH:mm} format.
     */
    private String endTime;

    /**
     * Number of expected attendees (1 – 200).
     */
    private int quantity;

    /**
     * Current lifecycle status of the reservation.
     * Valid values: {@code "PENDING"}, {@code "CONFIRMED"}, {@code "EXPIRED"}.
     */
    private String status;

    /**
     * Unix epoch timestamp (milliseconds) used internally for ordering or
     * expiry calculations, when needed outside the database layer.
     */
    private long timestamp;

    /**
     * Optional list of equipment items associated with this reservation.
     * Lazy-initialized on first access via {@link #getEquipments()}.
     */
    private List<ReservationEquipment> equipments;

    // ── Getters & Setters ──────────────────────────────────────────────────

    /**
     * Returns the primary key of this reservation.
     *
     * @return the reservation ID
     */
    public int getId() { return id; }

    /**
     * Sets the primary key of this reservation.
     *
     * @param id the reservation ID
     */
    public void setId(int id) { this.id = id; }

    /**
     * Returns the email of the user who owns this reservation.
     *
     * @return the user email
     */
    public String getUser() { return user; }

    /**
     * Sets the email of the user who owns this reservation.
     *
     * @param user the user email
     */
    public void setUser(String user) { this.user = user; }

    /**
     * Returns the event date in {@code yyyy-MM-dd} format.
     *
     * @return the event date
     */
    public String getDate() { return date; }

    /**
     * Sets the event date.
     *
     * @param date the event date in {@code yyyy-MM-dd} format
     */
    public void setDate(String date) { this.date = date; }

    /**
     * Returns the event start time in {@code HH:mm} format.
     *
     * @return the start time
     */
    public String getStartTime() { return startTime; }

    /**
     * Sets the event start time.
     *
     * @param startTime the start time in {@code HH:mm} format
     */
    public void setStartTime(String startTime) { this.startTime = startTime; }

    /**
     * Returns the event end time in {@code HH:mm} format.
     *
     * @return the end time
     */
    public String getEndTime() { return endTime; }

    /**
     * Sets the event end time.
     *
     * @param endTime the end time in {@code HH:mm} format
     */
    public void setEndTime(String endTime) { this.endTime = endTime; }

    /**
     * Returns the expected number of attendees.
     *
     * @return the attendee count
     */
    public int getQuantity() { return quantity; }

    /**
     * Sets the expected number of attendees.
     *
     * @param quantity the attendee count (1 – 200)
     */
    public void setQuantity(int quantity) { this.quantity = quantity; }

    /**
     * Returns the current status of this reservation.
     *
     * @return {@code "PENDING"}, {@code "CONFIRMED"}, or {@code "EXPIRED"}
     */
    public String getStatus() { return status; }

    /**
     * Sets the status of this reservation.
     *
     * @param status the new status value
     */
    public void setStatus(String status) { this.status = status; }

    /**
     * Returns the Unix epoch timestamp associated with this reservation.
     *
     * @return the timestamp in milliseconds
     */
    public long getTimestamp() { return timestamp; }

    /**
     * Sets the Unix epoch timestamp for this reservation.
     *
     * @param timestamp the timestamp in milliseconds
     */
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    /**
     * Not yet implemented. Intended to return the primary equipment type for
     * legacy single-equipment support.
     *
     * @return never returns normally
     * @throws UnsupportedOperationException always
     * @deprecated Use {@link #getEquipments()} for multi-equipment support.
     */
    @Deprecated
    public Object getEquipmentType() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    // ── Equipment helpers ──────────────────────────────────────────────────

    /**
     * Returns the list of equipment items associated with this reservation.
     * Lazy-initializes an empty {@link ArrayList} if the list has not been
     * set yet.
     *
     * @return the mutable equipment list; never {@code null}
     */
    public List<ReservationEquipment> getEquipments() {
        if (equipments == null) equipments = new ArrayList<>();
        return equipments;
    }

    /**
     * Replaces the equipment list for this reservation.
     *
     * @param equipments the new list of equipment items; may be {@code null}
     */
    public void setEquipments(List<ReservationEquipment> equipments) {
        this.equipments = equipments;
    }

    /**
     * Appends a single {@link ReservationEquipment} item to this reservation's
     * equipment list.
     *
     * @param equipment the equipment item to add; must not be {@code null}
     */
    public void addEquipment(ReservationEquipment equipment) {
        getEquipments().add(equipment);
    }
}