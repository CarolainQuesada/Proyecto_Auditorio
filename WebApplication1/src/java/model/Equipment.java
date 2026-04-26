package model;

/**
 * Represents a piece of auditorium equipment (e.g., projector, microphone,
 * sound system).
 *
 * <p>Each {@code Equipment} record mirrors a row in the {@code equipment}
 * database table and tracks both the total physical inventory and the
 * current number of units available for reservation.
 *
 * <p>This is a plain JavaBean — it contains no business logic and relies
 * entirely on getters and setters for field access.
 */
public class Equipment {

    /**
     * Primary key that uniquely identifies this equipment type in the database.
     */
    private int id;

    /**
     * Human-readable name of the equipment (stored in upper-case, e.g.,
     * {@code "PROYECTOR"}, {@code "MICROFONO"}, {@code "SONIDO"}).
     */
    private String name;

    /**
     * Total number of physical units owned by the auditorium regardless of
     * current reservations.
     */
    private int totalQuantity;

    /**
     * Number of units currently available for new reservations.
     * This value decreases when equipment is reserved and increases when
     * it is released or a reservation expires.
     */
    private int availableQuantity;

    /**
     * Returns the primary key of this equipment record.
     *
     * @return the equipment ID
     */
    public int getId() { return id; }

    /**
     * Sets the primary key of this equipment record.
     *
     * @param id the equipment ID
     */
    public void setId(int id) { this.id = id; }

    /**
     * Returns the name of this equipment type.
     *
     * @return the equipment name (upper-case)
     */
    public String getName() { return name; }

    /**
     * Sets the name of this equipment type.
     *
     * @param name the equipment name
     */
    public void setName(String name) { this.name = name; }

    /**
     * Returns the total number of units in the auditorium inventory.
     *
     * @return total quantity
     */
    public int getTotalQuantity() { return totalQuantity; }

    /**
     * Sets the total number of units in the auditorium inventory.
     *
     * @param totalQuantity total quantity
     */
    public void setTotalQuantity(int totalQuantity) { this.totalQuantity = totalQuantity; }

    /**
     * Returns the number of units currently available for reservation.
     *
     * @return available quantity
     */
    public int getAvailableQuantity() { return availableQuantity; }

    /**
     * Sets the number of units currently available for reservation.
     *
     * @param availableQuantity available quantity
     */
    public void setAvailableQuantity(int availableQuantity) { this.availableQuantity = availableQuantity; }
}