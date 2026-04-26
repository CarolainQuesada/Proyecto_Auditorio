package model;

/**
 * Represents the association between a {@link Reservation} and a specific
 * piece of equipment, including the quantity requested.
 *
 * <p>Each {@code ReservationEquipment} record mirrors a row in the
 * {@code reservation_equipment} join table in the database. A single
 * reservation may reference multiple equipment rows (one per equipment type).
 *
 * <p>This is a plain JavaBean with two constructors for convenience.
 */
public class ReservationEquipment {

    /**
     * Auto-incremented primary key of this join-table row.
     */
    private int id;

    /**
     * Foreign key referencing the owning {@code Reservation}.
     */
    private int reservationId;

    /**
     * Foreign key referencing the {@code Equipment} type
     * (1 = PROYECTOR, 2 = MICROFONO, 3 = SONIDO).
     */
    private int equipmentId;

    /**
     * Number of units of the referenced equipment requested for this
     * reservation.
     */
    private int quantity;

    /**
     * No-argument constructor required by frameworks that reflectively
     * instantiate model objects.
     */
    public ReservationEquipment() {}

    /**
     * Convenience constructor for creating a new association without a
     * database-assigned ID.
     *
     * @param reservationId the ID of the owning reservation
     * @param equipmentId   the ID of the equipment type
     * @param quantity      the number of units requested
     */
    public ReservationEquipment(int reservationId, int equipmentId, int quantity) {
        this.reservationId = reservationId;
        this.equipmentId   = equipmentId;
        this.quantity      = quantity;
    }

    /**
     * Returns the primary key of this join-table row.
     *
     * @return the row ID
     */
    public int getId() { return id; }

    /**
     * Sets the primary key of this join-table row.
     *
     * @param id the row ID assigned by the database
     */
    public void setId(int id) { this.id = id; }

    /**
     * Returns the ID of the owning reservation.
     *
     * @return the reservation ID
     */
    public int getReservationId() { return reservationId; }

    /**
     * Sets the ID of the owning reservation.
     *
     * @param reservationId the reservation ID
     */
    public void setReservationId(int reservationId) { this.reservationId = reservationId; }

    /**
     * Returns the ID of the equipment type.
     *
     * @return the equipment ID (1 = PROYECTOR, 2 = MICROFONO, 3 = SONIDO)
     */
    public int getEquipmentId() { return equipmentId; }

    /**
     * Sets the ID of the equipment type.
     *
     * @param equipmentId the equipment ID
     */
    public void setEquipmentId(int equipmentId) { this.equipmentId = equipmentId; }

    /**
     * Returns the number of units requested for this equipment.
     *
     * @return the requested quantity
     */
    public int getQuantity() { return quantity; }

    /**
     * Sets the number of units requested for this equipment.
     *
     * @param quantity the requested quantity
     */
    public void setQuantity(int quantity) { this.quantity = quantity; }
}