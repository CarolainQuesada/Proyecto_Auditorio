package dao;

import util.DBConnection;
import java.sql.*;
import model.ReservationEquipment;

/**
 * Data Access Object for equipment-related database operations.
 *
 * <p>This DAO handles:
 * <ul>
 *   <li>Looking up equipment IDs by name.</li>
 *   <li>Updating the available quantity of an equipment type after a
 *       reservation is created or released.</li>
 *   <li>Inserting rows into the {@code reservation_equipment} join table.</li>
 * </ul>
 *
 * <p>Each method opens its own {@link java.sql.Connection} via
 * {@link DBConnection#getConnection()} and closes it automatically using
 * try-with-resources. No connection pooling is used at this layer.
 *
 * @see model.Equipment
 * @see ReservationEquipmentDAO
 */
public class EquipmentDAO {

    /**
     * Placeholder create method — not yet implemented.
     *
     * @param re the reservation-equipment record to create
     * @return never returns normally
     * @throws UnsupportedOperationException always
     * @deprecated Not yet implemented; use {@link ReservationEquipmentDAO#create} instead.
     */
    @Deprecated
    static boolean create(ReservationEquipment re) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    /**
     * Retrieves the database ID of an equipment type by its upper-case name.
     *
     * <p>The name comparison is case-insensitive at the application level
     * (converted to upper-case before querying).
     *
     * @param name the equipment name (e.g., {@code "PROYECTOR"},
     *             {@code "MICROFONO"}, {@code "SONIDO"})
     * @return the equipment ID, or {@code -1} if no matching equipment is
     *         found or a database error occurs
     */
    public int getEquipmentIdByName(String name) {
        String sql = "SELECT id FROM equipment WHERE name = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {

            stmt.setString(1, name.toUpperCase());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }

        } catch (SQLException e) {
            System.err.println("[DAO ERROR] getEquipmentIdByName: " + e.getMessage());
        }
        return -1;
    }

    /**
     * Updates the {@code available_quantity} column for the specified
     * equipment record in the database.
     *
     * <p>This method should be called after acquiring or releasing semaphore
     * permits in {@link concurrency.EquipmentControl} so that the database
     * reflects the current in-memory availability.
     *
     * @param equipmentId the ID of the equipment record to update
     * @param newQuantity the new available quantity value
     * @return {@code true} if at least one row was updated; {@code false} if
     *         no row matched the given ID or a database error occurred
     */
    public boolean updateAvailability(int equipmentId, int newQuantity) {
        String sql = "UPDATE equipment SET available_quantity = ? WHERE id = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {

            stmt.setInt(1, newQuantity);
            stmt.setInt(2, equipmentId);

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("[DAO ERROR] updateAvailability: " + e.getMessage());
            return false;
        }
    }

    /**
     * Inserts a new row into the {@code reservation_equipment} join table.
     *
     * <p>This links a reservation to a specific equipment type with the
     * requested quantity.
     *
     * @param reservationId the ID of the owning reservation
     * @param equipmentId   the ID of the equipment type
     * @param quantity      the number of units requested
     * @return {@code true} if the row was successfully inserted; {@code false}
     *         on error
     */
    public boolean insertReservationEquipment(int reservationId, int equipmentId, int quantity) {
        String sql = "INSERT INTO reservation_equipment (reservation_id, equipment_id, quantity)"
                   + " VALUES (?, ?, ?)";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {

            stmt.setInt(1, reservationId);
            stmt.setInt(2, equipmentId);
            stmt.setInt(3, quantity);

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("[DAO ERROR] insertReservationEquipment: " + e.getMessage());
            return false;
        }
    }
}