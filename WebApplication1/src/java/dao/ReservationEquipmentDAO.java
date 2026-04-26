package dao;

import model.ReservationEquipment;
import util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for the {@code reservation_equipment} join table.
 *
 * <p>This DAO manages the many-to-many relationship between reservations and
 * equipment types. It supports both standalone operations (which open their own
 * connection) and transactional operations (which accept an externally managed
 * {@link Connection} so they can participate in a parent transaction initiated
 * by {@link ReservationDAO}).
 *
 * <h2>Design notes</h2>
 * <ul>
 *   <li>Methods that accept a {@link Connection} parameter do <em>not</em>
 *       close it; the caller is responsible for commit/rollback and close.</li>
 *   <li>Methods without a {@link Connection} parameter use try-with-resources
 *       and rely on auto-commit.</li>
 * </ul>
 *
 * @see ReservationDAO
 * @see model.ReservationEquipment
 */
public class ReservationEquipmentDAO {

    /**
     * Inserts a new {@code reservation_equipment} row using the provided
     * transactional connection.
     *
     * <p>On success, the generated primary key is written back to
     * {@link ReservationEquipment#setId(int)}.
     *
     * @param con the active transactional connection (auto-commit must be
     *            disabled by the caller)
     * @param re  the equipment-reservation link to persist; the
     *            {@code reservationId} field must already be set
     * @return {@code true} if the row was inserted; {@code false} on error
     */
    public boolean create(Connection con, ReservationEquipment re) {
        String sql = "INSERT INTO reservation_equipment "
                + "(reservation_id, equipment_id, quantity) "
                + "VALUES (?, ?, ?)";

        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, re.getReservationId());
            ps.setInt(2, re.getEquipmentId());
            ps.setInt(3, re.getQuantity());

            int rows = ps.executeUpdate();

            if (rows > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        re.setId(rs.getInt(1));
                    }
                }
                return true;
            }

            return false;

        } catch (SQLException e) {
            System.err.println("[DAO ERROR] ReservationEquipment create: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Inserts a new {@code reservation_equipment} row using an independent
     * auto-commit connection.
     *
     * <p>Use this method when no parent transaction is active. For
     * transactional inserts, prefer {@link #create(Connection, ReservationEquipment)}.
     *
     * @param reservationId the owning reservation's ID
     * @param equipmentId   the equipment type's ID
     * @param quantity      the number of units to link
     * @return {@code true} if the row was inserted; {@code false} on error
     */
    public boolean addEquipment(int reservationId, int equipmentId, int quantity) {
        String sql = "INSERT INTO reservation_equipment "
                + "(reservation_id, equipment_id, quantity) "
                + "VALUES (?, ?, ?)";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, reservationId);
            ps.setInt(2, equipmentId);
            ps.setInt(3, quantity);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("[DAO ERROR] addEquipment: " + e.getMessage());
            return false;
        }
    }

    /**
     * Checks whether a specific equipment type is already linked to a
     * reservation.
     *
     * @param reservationId the reservation ID
     * @param equipmentId   the equipment type ID
     * @return {@code true} if at least one row exists for the combination;
     *         {@code false} otherwise or on error
     */
    public boolean exists(int reservationId, int equipmentId) {
        String sql = "SELECT COUNT(*) FROM reservation_equipment "
                + "WHERE reservation_id = ? AND equipment_id = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, reservationId);
            ps.setInt(2, equipmentId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }

        } catch (SQLException e) {
            System.err.println("[DAO ERROR] exists: " + e.getMessage());
        }

        return false;
    }

    /**
     * Inserts or updates the quantity for an equipment type linked to a
     * reservation.
     *
     * <p>If a row for the given {@code (reservationId, equipmentId)} pair
     * already exists, the quantity is updated via
     * {@link #updateQuantity(int, int, int)}. Otherwise a new row is inserted
     * via {@link #addEquipment(int, int, int)}.
     *
     * @param reservationId the reservation ID
     * @param equipmentId   the equipment type ID
     * @param quantity      the desired quantity
     * @return {@code true} if the upsert succeeded; {@code false} on error
     */
    public boolean addOrUpdate(int reservationId, int equipmentId, int quantity) {
        if (exists(reservationId, equipmentId)) {
            return updateQuantity(reservationId, equipmentId, quantity);
        }

        return addEquipment(reservationId, equipmentId, quantity);
    }

    /**
     * Retrieves all equipment records linked to the given reservation.
     *
     * @param reservationId the reservation ID
     * @return a list of {@link ReservationEquipment} objects; never
     *         {@code null}, but may be empty if none are found or on error
     */
    public List<ReservationEquipment> getByReservationId(int reservationId) {
        List<ReservationEquipment> list = new ArrayList<>();

        String sql = "SELECT * FROM reservation_equipment WHERE reservation_id = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, reservationId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }

        } catch (SQLException e) {
            System.err.println("[DAO ERROR] getByReservationId: " + e.getMessage());
        }

        return list;
    }

    /**
     * Deletes all equipment records linked to a reservation using an
     * independent auto-commit connection.
     *
     * <p>For transactional deletions (e.g. as part of a reservation delete),
     * use {@link #deleteByReservationId(Connection, int)}.
     *
     * @param reservationId the reservation ID whose equipment rows should be
     *                      removed
     * @return {@code true} on success; {@code false} on error
     */
    public boolean deleteByReservationId(int reservationId) {
        String sql = "DELETE FROM reservation_equipment WHERE reservation_id = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, reservationId);
            ps.executeUpdate();
            return true;

        } catch (SQLException e) {
            System.err.println("[DAO ERROR] deleteByReservationId: " + e.getMessage());
            return false;
        }
    }

    /**
     * Deletes all equipment records linked to a reservation using the
     * provided transactional connection.
     *
     * <p>The connection is <em>not</em> committed or closed by this method;
     * the caller manages the transaction boundary.
     *
     * @param con           the active transactional connection
     * @param reservationId the reservation ID whose equipment rows should be
     *                      removed
     * @return {@code true} on success; {@code false} on error
     */
    public boolean deleteByReservationId(Connection con, int reservationId) {
        String sql = "DELETE FROM reservation_equipment WHERE reservation_id = ?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, reservationId);
            ps.executeUpdate();
            return true;

        } catch (SQLException e) {
            System.err.println("[DAO ERROR] deleteByReservationId con transacción: " + e.getMessage());
            return false;
        }
    }

    /**
     * Deletes a single equipment record identified by both reservation ID and
     * equipment ID.
     *
     * @param reservationId the reservation ID
     * @param equipmentId   the equipment type ID
     * @return {@code true} if a row was deleted; {@code false} if no matching
     *         row existed or on error
     */
    public boolean delete(int reservationId, int equipmentId) {
        String sql = "DELETE FROM reservation_equipment "
                + "WHERE reservation_id = ? AND equipment_id = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, reservationId);
            ps.setInt(2, equipmentId);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("[DAO ERROR] delete: " + e.getMessage());
            return false;
        }
    }

    /**
     * Updates the quantity of a specific equipment type linked to a
     * reservation.
     *
     * @param reservationId the reservation ID
     * @param equipmentId   the equipment type ID
     * @param quantity      the new quantity value
     * @return {@code true} if at least one row was updated; {@code false} if
     *         the record was not found or an error occurred
     */
    public boolean updateQuantity(int reservationId, int equipmentId, int quantity) {
        String sql = "UPDATE reservation_equipment "
                + "SET quantity = ? "
                + "WHERE reservation_id = ? AND equipment_id = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, quantity);
            ps.setInt(2, reservationId);
            ps.setInt(3, equipmentId);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("[DAO ERROR] updateQuantity: " + e.getMessage());
            return false;
        }
    }

    /**
     * Maps the current row of a {@link ResultSet} to a
     * {@link ReservationEquipment} object.
     *
     * @param rs an open {@link ResultSet} positioned on the row to map
     * @return a populated {@link ReservationEquipment} instance
     * @throws SQLException if a column cannot be read
     */
    private ReservationEquipment map(ResultSet rs) throws SQLException {
        ReservationEquipment re = new ReservationEquipment();

        re.setId(rs.getInt("id"));
        re.setReservationId(rs.getInt("reservation_id"));
        re.setEquipmentId(rs.getInt("equipment_id"));
        re.setQuantity(rs.getInt("quantity"));

        return re;
    }
}