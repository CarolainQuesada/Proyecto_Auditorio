package dao;

import model.Reservation;
import model.ReservationEquipment;
import util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for reservation-related database operations.
 *
 * <p>This DAO provides full CRUD access for the {@code reservations} table and
 * coordinates with {@link ReservationEquipmentDAO} for operations that span
 * both {@code reservations} and {@code reservation_equipment}. Transactional
 * operations (create-with-equipment, delete) manage their own
 * {@link java.sql.Connection} lifecycle manually and use explicit
 * {@code commit}/{@code rollback} calls rather than auto-commit.
 *
 * <p>Single-table operations open their connection via try-with-resources and
 * rely on auto-commit.
 *
 * @see ReservationEquipmentDAO
 * @see model.Reservation
 */
public class ReservationDAO {

    /** DAO for the {@code reservation_equipment} join table. */
    private final ReservationEquipmentDAO equipmentDAO;

    /**
     * Constructs a {@code ReservationDAO} with a default
     * {@link ReservationEquipmentDAO}.
     */
    public ReservationDAO() {
        this.equipmentDAO = new ReservationEquipmentDAO();
    }

    /**
     * Verifies that a user row with the given email exists in the
     * {@code users} table, creating one automatically if it does not.
     *
     * <p>The auto-created user is assigned a time-based password and the role
     * {@code "CLIENT"}. This method participates in the caller's transaction
     * (the supplied {@link Connection} must have auto-commit disabled).
     *
     * @param con   an active connection with auto-commit disabled
     * @param email the user's email address
     * @return {@code true} if the user already existed or was successfully
     *         created; {@code false} if the insert failed
     * @throws SQLException if a database error occurs
     */
    private boolean ensureUserExists(Connection con, String email) throws SQLException {
        String checkSql = "SELECT COUNT(*) FROM users WHERE email = ?";

        try (PreparedStatement checkPs = con.prepareStatement(checkSql)) {
            checkPs.setString(1, email);

            try (ResultSet rs = checkPs.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    return true;
                }
            }
        }

        String insertSql = "INSERT INTO users (email, password, role) VALUES (?, ?, ?)";

        try (PreparedStatement ps = con.prepareStatement(insertSql)) {
            ps.setString(1, email);
            ps.setString(2, "auto_" + System.currentTimeMillis());
            ps.setString(3, "CLIENT");

            int rows = ps.executeUpdate();
            System.out.println("[DAO] Usuario auto-creado: " + email);
            return rows > 0;
        }
    }

    /**
     * Inserts a new reservation row with status {@code PENDING} and the
     * current timestamp as {@code created_at}.
     *
     * <p>This simpler overload does not attach equipment records. Use
     * {@link #createWithEquipment} when equipment is required.
     *
     * @param user     the email of the user making the reservation
     * @param date     the reservation date in {@code YYYY-MM-DD} format
     * @param start    the start time in {@code HH:mm} format
     * @param end      the end time in {@code HH:mm} format
     * @param quantity the number of seats/resources requested
     * @return {@code true} if the row was inserted; {@code false} on error
     */
    public boolean create(String user, String date, String start, String end, int quantity) {
        String sql = "INSERT INTO reservations "
                + "(user, date, start_time, end_time, quantity, status, created_at) "
                + "VALUES (?, ?, ?, ?, ?, 'PENDING', NOW())";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, user);
            ps.setString(2, date);
            ps.setString(3, start);
            ps.setString(4, end);
            ps.setInt(5, quantity);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("[DAO ERROR] create: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Inserts a reservation and its associated equipment records within a
     * single database transaction.
     *
     * <p>The operation proceeds as follows:
     * <ol>
     *   <li>Ensure the user row exists (auto-creating if necessary).</li>
     *   <li>Insert the reservation row and retrieve the generated ID.</li>
     *   <li>For each {@link ReservationEquipment}, set the reservation ID
     *       and delegate insertion to {@link ReservationEquipmentDAO#create}.</li>
     *   <li>Commit on full success; roll back on any failure.</li>
     * </ol>
     *
     * @param user       the email of the user making the reservation
     * @param date       the reservation date in {@code YYYY-MM-DD} format
     * @param start      the start time in {@code HH:mm} format
     * @param end        the end time in {@code HH:mm} format
     * @param quantity   the number of seats/resources requested
     * @param equipments list of equipment items to attach; may be {@code null}
     *                   or empty if no equipment is needed
     * @return {@code true} if the reservation and all equipment rows were
     *         committed successfully; {@code false} otherwise
     */
    public boolean createWithEquipment(String user, String date, String start, String end,
                                       int quantity, List<ReservationEquipment> equipments) {

        Connection con = null;

        try {
            con = DBConnection.getConnection();
            con.setAutoCommit(false);

            if (!ensureUserExists(con, user)) {
                System.err.println("[DAO ERROR] No se pudo crear/verificar el usuario: " + user);
                con.rollback();
                return false;
            }

            String sqlRes = "INSERT INTO reservations "
                    + "(user, date, start_time, end_time, quantity, status, created_at) "
                    + "VALUES (?, ?, ?, ?, ?, 'PENDING', NOW())";

            int reservationId;

            try (PreparedStatement ps = con.prepareStatement(sqlRes, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, user);
                ps.setString(2, date);
                ps.setString(3, start);
                ps.setString(4, end);
                ps.setInt(5, quantity);

                if (ps.executeUpdate() == 0) {
                    con.rollback();
                    return false;
                }

                try (ResultSet rs = ps.getGeneratedKeys()) {
                    reservationId = rs.next() ? rs.getInt(1) : -1;
                }
            }

            if (reservationId <= 0) {
                con.rollback();
                return false;
            }

            if (equipments != null && !equipments.isEmpty()) {
                for (ReservationEquipment re : equipments) {
                    re.setReservationId(reservationId);

                    if (!equipmentDAO.create(con, re)) {
                        con.rollback();
                        return false;
                    }
                }
            }

            con.commit();
            return true;

        } catch (SQLException e) {
            System.err.println("[DAO ERROR] createWithEquipment: " + e.getMessage());
            e.printStackTrace();

            try {
                if (con != null) {
                    con.rollback();
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }

            return false;

        } finally {
            try {
                if (con != null) {
                    con.setAutoCommit(true);
                    con.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Retrieves all non-expired reservations ordered by date and start time.
     *
     * <p>Rows with {@code status = 'EXPIRED'} are excluded. The returned
     * {@link Reservation} objects are not populated with equipment details;
     * use {@link #getByIdWithEquipment(int)} for that.
     *
     * @return a list of reservations; never {@code null}, but may be empty
     */
    public List<Reservation> getAll() {
        List<Reservation> list = new ArrayList<>();

        String sql = "SELECT * FROM reservations "
                + "WHERE status != 'EXPIRED' "
                + "ORDER BY date, start_time";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapReservation(rs));
            }

        } catch (SQLException e) {
            System.err.println("[DAO ERROR] getAll: " + e.getMessage());
        }

        return list;
    }

    /**
     * Retrieves all non-expired reservations for a specific date.
     *
     * @param date the date to filter by in {@code YYYY-MM-DD} format
     * @return a list of matching reservations; never {@code null}
     */
    public List<Reservation> getByDate(String date) {
        List<Reservation> list = new ArrayList<>();

        String sql = "SELECT * FROM reservations "
                + "WHERE date = ? "
                + "AND status != 'EXPIRED'";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, date);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapReservation(rs));
                }
            }

        } catch (SQLException e) {
            System.err.println("[DAO ERROR] getByDate: " + e.getMessage());
        }

        return list;
    }

    /**
     * Retrieves a single reservation by its primary key.
     *
     * @param id the reservation ID
     * @return the matching {@link Reservation}, or {@code null} if not found
     *         or a database error occurs
     */
    public Reservation getById(int id) {
        String sql = "SELECT * FROM reservations WHERE id = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapReservation(rs);
                }
            }

        } catch (SQLException e) {
            System.err.println("[DAO ERROR] getById: " + e.getMessage());
        }

        return null;
    }

    /**
     * Retrieves a reservation by ID together with its associated equipment
     * list.
     *
     * <p>Delegates to {@link #getById(int)} and then populates the
     * {@code equipments} field via
     * {@link ReservationEquipmentDAO#getByReservationId(int)}.
     *
     * @param id the reservation ID
     * @return the reservation with its equipment list set, or {@code null} if
     *         not found
     */
    public Reservation getByIdWithEquipment(int id) {
        Reservation res = getById(id);

        if (res != null) {
            res.setEquipments(equipmentDAO.getByReservationId(id));
        }

        return res;
    }

    /**
     * Confirms a pending reservation if it is within the 10-minute TTL
     * window.
     *
     * <p>The update sets {@code status = 'CONFIRMED'} only when the current
     * status is {@code 'PENDING'} and fewer than 10 minutes have elapsed
     * since {@code created_at}.
     *
     * @param id the reservation ID to confirm
     * @return {@code true} if the row was updated; {@code false} if the
     *         reservation was not found, was not pending, or the TTL had
     *         already expired
     */
    public boolean confirm(int id) {
        String sql = "UPDATE reservations "
                + "SET status = 'CONFIRMED' "
                + "WHERE id = ? "
                + "AND status = 'PENDING' "
                + "AND TIMESTAMPDIFF(MINUTE, created_at, NOW()) < 10";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, id);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("[DAO ERROR] confirm: " + e.getMessage());
            return false;
        }
    }

    /**
     * Updates the date, time range, quantity, and status of an existing
     * reservation.
     *
     * @param reservation the reservation object carrying the new values;
     *                    {@link Reservation#getId()} must correspond to an
     *                    existing row
     * @return {@code true} if at least one row was updated; {@code false}
     *         otherwise
     */
    public boolean update(Reservation reservation) {
        String sql = "UPDATE reservations "
                + "SET date = ?, start_time = ?, end_time = ?, quantity = ?, status = ? "
                + "WHERE id = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, reservation.getDate());
            ps.setString(2, reservation.getStartTime());
            ps.setString(3, reservation.getEndTime());
            ps.setInt(4, reservation.getQuantity());
            ps.setString(5, reservation.getStatus());
            ps.setInt(6, reservation.getId());

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("[DAO ERROR] update: " + e.getMessage());
            return false;
        }
    }

    /**
     * Updates only the {@code status} column of a reservation.
     *
     * @param id     the reservation ID
     * @param status the new status value (e.g. {@code "CONFIRMED"},
     *               {@code "EXPIRED"})
     * @return {@code true} if the row was updated; {@code false} otherwise
     */
    public boolean updateStatus(int id, String status) {
        String sql = "UPDATE reservations SET status = ? WHERE id = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, status);
            ps.setInt(2, id);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("[DAO ERROR] updateStatus: " + e.getMessage());
            return false;
        }
    }

    /**
     * Deletes a reservation and all its associated equipment records within a
     * single transaction.
     *
     * <p>Equipment rows are removed first (via
     * {@link ReservationEquipmentDAO#deleteByReservationId(Connection, int)})
     * to maintain referential integrity. The whole operation is rolled back if
     * either deletion fails.
     *
     * @param id the reservation ID to delete
     * @return {@code true} if the reservation was deleted; {@code false} if
     *         the reservation was not found or a database error occurred
     */
    public boolean delete(int id) {
        Connection con = null;

        try {
            con = DBConnection.getConnection();
            con.setAutoCommit(false);

            if (!equipmentDAO.deleteByReservationId(con, id)) {
                con.rollback();
                return false;
            }

            String sql = "DELETE FROM reservations WHERE id = ?";

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, id);

                int rows = ps.executeUpdate();

                if (rows > 0) {
                    con.commit();
                    return true;
                }
            }

            con.rollback();
            return false;

        } catch (SQLException e) {
            System.err.println("[DAO ERROR] delete: " + e.getMessage());

            try {
                if (con != null) {
                    con.rollback();
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }

            return false;

        } finally {
            try {
                if (con != null) {
                    con.setAutoCommit(true);
                    con.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Checks whether any non-expired reservation overlaps the given time
     * range on the specified date.
     *
     * <p>Two reservations overlap when one starts before the other ends and
     * ends after the other starts (standard interval overlap logic).
     *
     * @param date  the date to check in {@code YYYY-MM-DD} format
     * @param start the proposed start time in {@code HH:mm} format
     * @param end   the proposed end time in {@code HH:mm} format
     * @return {@code true} if at least one overlapping reservation exists;
     *         {@code false} otherwise
     */
    public boolean existsOverlap(String date, String start, String end) {
        String sql = "SELECT COUNT(*) FROM reservations "
                + "WHERE date = ? "
                + "AND status != 'EXPIRED' "
                + "AND start_time < ? "
                + "AND end_time > ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, date);
            ps.setString(2, end);
            ps.setString(3, start);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }

        } catch (SQLException e) {
            System.err.println("[DAO ERROR] existsOverlap: " + e.getMessage());
        }

        return false;
    }

    /**
     * Checks for overlapping reservations on a given date and time range,
     * excluding a specific reservation ID.
     *
     * <p>Useful when editing an existing reservation: the reservation being
     * edited is excluded from the overlap check so that it does not conflict
     * with itself.
     *
     * @param id    the reservation ID to exclude from the check
     * @param date  the date to check in {@code YYYY-MM-DD} format
     * @param start the proposed start time in {@code HH:mm} format
     * @param end   the proposed end time in {@code HH:mm} format
     * @return {@code true} if at least one other overlapping reservation
     *         exists; {@code false} otherwise
     */
    public boolean existsOverlapExcludingId(int id, String date, String start, String end) {
        String sql = "SELECT COUNT(*) FROM reservations "
                + "WHERE date = ? "
                + "AND status != 'EXPIRED' "
                + "AND id != ? "
                + "AND start_time < ? "
                + "AND end_time > ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, date);
            ps.setInt(2, id);
            ps.setString(3, end);
            ps.setString(4, start);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }

        } catch (SQLException e) {
            System.err.println("[DAO ERROR] existsOverlapExcludingId: " + e.getMessage());
        }

        return false;
    }

    /**
     * Marks all pending reservations whose age exceeds the given TTL as
     * expired.
     *
     * <p>Sets {@code status = 'EXPIRED'} for every row where
     * {@code status = 'PENDING'} and the difference between {@code NOW()} and
     * {@code created_at} is at least {@code ttlMinutes}.
     *
     * @param ttlMinutes the time-to-live threshold in minutes
     * @return the number of rows updated (expired), or {@code 0} on error
     */
    public int cleanExpired(int ttlMinutes) {
        String sql = "UPDATE reservations "
                + "SET status = 'EXPIRED' "
                + "WHERE status = 'PENDING' "
                + "AND TIMESTAMPDIFF(MINUTE, created_at, NOW()) >= ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, ttlMinutes);

            int rows = ps.executeUpdate();
            System.out.println("[DAO] Reservas expiradas: " + rows);
            return rows;

        } catch (SQLException e) {
            System.err.println("[DAO ERROR] cleanExpired: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Marks all pending reservations older than 10 minutes as expired.
     *
     * <p>Convenience overload of {@link #cleanExpired(int)} using the default
     * TTL of 10 minutes.
     *
     * @return the number of rows updated (expired)
     */
    public int cleanExpired() {
        return cleanExpired(10);
    }

    /**
     * Maps the current row of a {@link ResultSet} to a {@link Reservation}
     * object.
     *
     * @param rs an open {@link ResultSet} positioned on the row to map
     * @return a populated {@link Reservation} instance
     * @throws SQLException if a column cannot be read
     */
    private Reservation mapReservation(ResultSet rs) throws SQLException {
        Reservation r = new Reservation();

        r.setId(rs.getInt("id"));
        r.setUser(rs.getString("user"));
        r.setDate(rs.getString("date"));
        r.setStartTime(rs.getString("start_time"));
        r.setEndTime(rs.getString("end_time"));
        r.setQuantity(rs.getInt("quantity"));
        r.setStatus(rs.getString("status"));

        return r;
    }
}