package dao;

import model.Reservation;
import model.ReservationEquipment;
import util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Data Access Object for reservation-related database operations.
 */
public class ReservationDAO {

    private final ReservationEquipmentDAO equipmentDAO;

    public ReservationDAO() {
        this.equipmentDAO = new ReservationEquipmentDAO();
    }

    private boolean ensureUserExists(Connection con, String email) throws SQLException {
        String checkSql = "SELECT COUNT(*) FROM users WHERE email = ?";
        try (PreparedStatement checkPs = con.prepareStatement(checkSql)) {
            checkPs.setString(1, email);
            try (ResultSet rs = checkPs.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) return true;
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

    public boolean create(String user, String date, String start, String end, int quantity) {
        String sql = "INSERT INTO reservations (user, date, start_time, end_time, quantity, status, created_at) VALUES (?, ?, ?, ?, ?, 'PENDING', NOW())";
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
     * Inserts a reservation and its associated equipment records within a single transaction.
     * Returns: "created", "blocked_date", or "error".
     */
    public String createWithEquipment(String user, String date, String start, String end,
                                      int quantity, List<ReservationEquipment> equipments) {
        Connection con = null;
        try {
            con = DBConnection.getConnection();
            con.setAutoCommit(false);

            String checkBlockSql = "SELECT COUNT(*) FROM blocked_days WHERE fecha = ?";
            try (PreparedStatement psCheck = con.prepareStatement(checkBlockSql)) {
                psCheck.setString(1, date);
                ResultSet rs = psCheck.executeQuery();
                if (rs.next() && rs.getInt(1) > 0) {
                    System.out.println("[DAO] 🚫 Día bloqueado: " + date);
                    con.rollback();
                    return "blocked_date";
                }
            }

            if (!ensureUserExists(con, user)) {
                System.err.println("[DAO ERROR] Usuario no creado: " + user);
                con.rollback();
                return "error";
            }
            
            String sqlRes = "INSERT INTO reservations (user, date, start_time, end_time, quantity, status, created_at) VALUES (?, ?, ?, ?, ?, 'PENDING', NOW())";
            int reservationId;
            try (PreparedStatement ps = con.prepareStatement(sqlRes, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, user);
                ps.setString(2, date);
                ps.setString(3, start);
                ps.setString(4, end);
                ps.setInt(5, quantity);
                if (ps.executeUpdate() == 0) { con.rollback(); return "error"; }
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    reservationId = rs.next() ? rs.getInt(1) : -1;
                }
            }
            if (reservationId <= 0) { con.rollback(); return "error"; }

            if (equipments != null && !equipments.isEmpty()) {
                for (ReservationEquipment re : equipments) {
                    re.setReservationId(reservationId);
                    if (!equipmentDAO.create(con, re)) { con.rollback(); return "error"; }
                }
            }

            con.commit();
            return "created";

        } catch (SQLException e) {
            System.err.println("[DAO ERROR] createWithEquipment: " + e.getMessage());
            e.printStackTrace();
            try { if (con != null) con.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            return "error";
        } finally {
            try {
                if (con != null) { con.setAutoCommit(true); con.close(); }
            } catch (SQLException e) { e.printStackTrace(); }
        }
    }

    public List<Reservation> getAll() {
        List<Reservation> list = new ArrayList<>();
        String sql = "SELECT * FROM reservations ORDER BY id DESC LIMIT 10";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapReservation(rs));
        } catch (SQLException e) { System.err.println("[DAO ERROR] getAll: " + e.getMessage()); }
        return list;
    }

    public List<Reservation> getAllWithEquipment() {
        return getAllWithEquipment(1, 10);
    }

    public List<Reservation> getAllWithEquipment(int page, int pageSize) {
        return getAllWithEquipment(page, pageSize, "", "ALL", "");
    }

    public List<Reservation> getAllWithEquipment(int page, int pageSize, String search, String status, String date) {
        Map<Integer, Reservation> reservations = new LinkedHashMap<>();
        int safePage = Math.max(page, 1);
        int safePageSize = Math.max(1, Math.min(pageSize, 50));
        int offset = (safePage - 1) * safePageSize;
        List<Object> params = new ArrayList<>();

        String sql = "SELECT r.*, re.id AS re_id, re.equipment_id, re.quantity AS equipment_quantity "
                + "FROM ("
                + "  SELECT * FROM reservations ";

        StringBuilder filters = new StringBuilder();
        appendReservationFilters(filters, params, search, status, date);

        sql += filters
                + "  ORDER BY id DESC "
                + "  LIMIT ? OFFSET ?"
                + ") r "
                + "LEFT JOIN reservation_equipment re ON re.reservation_id = r.id "
                + "ORDER BY r.id DESC";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            int index = 1;
            for (Object param : params) {
                ps.setObject(index++, param);
            }
            ps.setInt(index++, safePageSize);
            ps.setInt(index, offset);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int reservationId = rs.getInt("id");
                    Reservation reservation = reservations.get(reservationId);

                    if (reservation == null) {
                        reservation = mapReservation(rs);
                        reservations.put(reservationId, reservation);
                    }

                    int equipmentRowId = rs.getInt("re_id");
                    if (!rs.wasNull()) {
                        ReservationEquipment equipment = new ReservationEquipment();
                        equipment.setId(equipmentRowId);
                        equipment.setReservationId(reservationId);
                        equipment.setEquipmentId(rs.getInt("equipment_id"));
                        equipment.setQuantity(rs.getInt("equipment_quantity"));
                        reservation.addEquipment(equipment);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[DAO ERROR] getAllWithEquipment: " + e.getMessage());
        }

        return new ArrayList<>(reservations.values());
    }

    public int countActiveReservations() {
        return countActiveReservations("", "ALL", "");
    }

    public int countActiveReservations(String search, String status, String date) {
        String sql = "SELECT COUNT(*) FROM reservations";
        List<Object> params = new ArrayList<>();
        StringBuilder filters = new StringBuilder();
        appendReservationFilters(filters, params, search, status, date);
        sql += filters;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            int index = 1;
            for (Object param : params) {
                ps.setObject(index++, param);
            }

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }

        } catch (SQLException e) {
            System.err.println("[DAO ERROR] countActiveReservations: " + e.getMessage());
        }

        return 0;
    }

    private void appendReservationFilters(StringBuilder sql, List<Object> params, String search, String status, String date) {
        List<String> conditions = new ArrayList<>();

        if (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status.trim())) {
            conditions.add("status = ?");
            params.add(status.trim().toUpperCase());
        }

        if (date != null && !date.trim().isEmpty()) {
            conditions.add("date = ?");
            params.add(date.trim());
        }

        if (search != null && !search.trim().isEmpty()) {
            String term = "%" + search.trim() + "%";
            conditions.add("("
                    + "CAST(id AS CHAR) LIKE ? OR "
                    + "CAST(date AS CHAR) LIKE ? OR "
                    + "user LIKE ? OR "
                    + "status LIKE ? OR "
                    + "EXISTS ("
                    + "  SELECT 1 FROM reservation_equipment re "
                    + "  LEFT JOIN equipment e ON e.id = re.equipment_id "
                    + "  WHERE re.reservation_id = reservations.id "
                    + "  AND (CAST(re.equipment_id AS CHAR) LIKE ? OR e.name LIKE ?)"
                    + ")"
                    + ")");
            params.add(term);
            params.add(term);
            params.add(term);
            params.add(term);
            params.add(term);
            params.add(term);
        }

        if (!conditions.isEmpty()) {
            sql.append(" WHERE ").append(String.join(" AND ", conditions)).append(" ");
        }
    }

    public List<Reservation> getByDate(String date) {
        List<Reservation> list = new ArrayList<>();
        String sql = "SELECT * FROM reservations WHERE date = ? AND status != 'EXPIRED'";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, date);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapReservation(rs));
            }
        } catch (SQLException e) { System.err.println("[DAO ERROR] getByDate: " + e.getMessage()); }
        return list;
    }

    public List<Reservation> getByUser(String email) {
        List<Reservation> list = new ArrayList<>();
        String sql = "SELECT * FROM reservations WHERE user = ? ORDER BY date DESC, start_time DESC";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapReservation(rs));
            }
        } catch (SQLException e) { System.err.println("[DAO ERROR] getByUser: " + e.getMessage()); }
        return list;
    }

    public List<Reservation> getByUserWithEquipment(String email) {
        Map<Integer, Reservation> reservations = new LinkedHashMap<>();
        String sql = "SELECT r.*, re.id AS re_id, re.equipment_id, re.quantity AS equipment_quantity "
                + "FROM reservations r "
                + "LEFT JOIN reservation_equipment re ON re.reservation_id = r.id "
                + "WHERE r.user = ? "
                + "ORDER BY r.date DESC, r.start_time DESC, r.id DESC";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, email);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int reservationId = rs.getInt("id");
                    Reservation reservation = reservations.get(reservationId);

                    if (reservation == null) {
                        reservation = mapReservation(rs);
                        reservations.put(reservationId, reservation);
                    }

                    int equipmentRowId = rs.getInt("re_id");
                    if (!rs.wasNull()) {
                        ReservationEquipment equipment = new ReservationEquipment();
                        equipment.setId(equipmentRowId);
                        equipment.setReservationId(reservationId);
                        equipment.setEquipmentId(rs.getInt("equipment_id"));
                        equipment.setQuantity(rs.getInt("equipment_quantity"));
                        reservation.addEquipment(equipment);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[DAO ERROR] getByUserWithEquipment: " + e.getMessage());
        }

        return new ArrayList<>(reservations.values());
    }

    public Reservation getById(int id) {
        String sql = "SELECT * FROM reservations WHERE id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapReservation(rs);
            }
        } catch (SQLException e) { System.err.println("[DAO ERROR] getById: " + e.getMessage()); }
        return null;
    }

    public Reservation getByIdWithEquipment(int id) {
        Reservation res = getById(id);
        if (res != null) res.setEquipments(equipmentDAO.getByReservationId(id));
        return res;
    }

    public boolean confirm(int id) {
        String sql = "UPDATE reservations SET status = 'CONFIRMED' WHERE id = ? AND status = 'PENDING' AND TIMESTAMPDIFF(MINUTE, created_at, NOW()) < 10";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { System.err.println("[DAO ERROR] confirm: " + e.getMessage()); return false; }
    }

    public boolean update(Reservation reservation) {
        String sql = "UPDATE reservations SET date = ?, start_time = ?, end_time = ?, quantity = ?, status = ? WHERE id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, reservation.getDate());
            ps.setString(2, reservation.getStartTime());
            ps.setString(3, reservation.getEndTime());
            ps.setInt(4, reservation.getQuantity());
            ps.setString(5, reservation.getStatus());
            ps.setInt(6, reservation.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { System.err.println("[DAO ERROR] update: " + e.getMessage()); return false; }
    }

    public boolean updateStatus(int id, String status) {
        String sql = "UPDATE reservations SET status = ? WHERE id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { System.err.println("[DAO ERROR] updateStatus: " + e.getMessage()); return false; }
    }

    public boolean delete(int id) {
        Connection con = null;
        try {
            con = DBConnection.getConnection();
            con.setAutoCommit(false);
            if (!equipmentDAO.deleteByReservationId(con, id)) { con.rollback(); return false; }
            String sql = "DELETE FROM reservations WHERE id = ?";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, id);
                int rows = ps.executeUpdate();
                if (rows > 0) { con.commit(); return true; }
            }
            con.rollback();
            return false;
        } catch (SQLException e) {
            System.err.println("[DAO ERROR] delete: " + e.getMessage());
            try { if (con != null) con.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            return false;
        } finally {
            try { if (con != null) { con.setAutoCommit(true); con.close(); } } catch (SQLException e) { e.printStackTrace(); }
        }
    }

    public boolean existsOverlap(String date, String start, String end) {
        String sql = "SELECT COUNT(*) FROM reservations WHERE date = ? AND status != 'EXPIRED' AND start_time < ? AND end_time > ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, date);
            ps.setString(2, end);
            ps.setString(3, start);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        } catch (SQLException e) { System.err.println("[DAO ERROR] existsOverlap: " + e.getMessage()); }
        return false;
    }

    public boolean existsOverlapExcludingId(int id, String date, String start, String end) {
        String sql = "SELECT COUNT(*) FROM reservations WHERE date = ? AND status != 'EXPIRED' AND id != ? AND start_time < ? AND end_time > ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, date);
            ps.setInt(2, id);
            ps.setString(3, end);
            ps.setString(4, start);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        } catch (SQLException e) { System.err.println("[DAO ERROR] existsOverlapExcludingId: " + e.getMessage()); }
        return false;
    }

    public int cleanExpired(int ttlMinutes) {
        String selectSql = "SELECT id FROM reservations "
                + "WHERE status = 'PENDING' "
                + "AND TIMESTAMPDIFF(MINUTE, created_at, NOW()) >= ?";
        String updateSql = "UPDATE reservations SET status = 'EXPIRED' "
                + "WHERE status = 'PENDING' "
                + "AND TIMESTAMPDIFF(MINUTE, created_at, NOW()) >= ?";

        Connection con = null;
        try {
            con = DBConnection.getConnection();
            con.setAutoCommit(false);

            List<Integer> expiredIds = new ArrayList<>();
            try (PreparedStatement ps = con.prepareStatement(selectSql)) {
                ps.setInt(1, ttlMinutes);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        expiredIds.add(rs.getInt("id"));
                    }
                }
            }

            if (expiredIds.isEmpty()) {
                con.commit();
                return 0;
            }

            int rows;
            try (PreparedStatement ps = con.prepareStatement(updateSql)) {
                ps.setInt(1, ttlMinutes);
                rows = ps.executeUpdate();
            }

            if (!equipmentDAO.deleteByReservationIds(con, expiredIds)) {
                con.rollback();
                return 0;
            }

            con.commit();
            System.out.println("[DAO] Reservas expiradas: " + rows
                    + "; equipamiento liberado: " + expiredIds.size());
            return rows;
        } catch (SQLException e) {
            System.err.println("[DAO ERROR] cleanExpired: " + e.getMessage());
            try {
                if (con != null) {
                    con.rollback();
                }
            } catch (SQLException ex) {
                System.err.println("[DAO ERROR] cleanExpired rollback: " + ex.getMessage());
            }
            return 0;
        } finally {
            try {
                if (con != null) {
                    con.setAutoCommit(true);
                    con.close();
                }
            } catch (SQLException e) {
                System.err.println("[DAO ERROR] cleanExpired close: " + e.getMessage());
            }
        }
    }

    public int cleanExpired() { return cleanExpired(10); }

    private Reservation mapReservation(ResultSet rs) throws SQLException {
        Reservation r = new Reservation();
        r.setId(rs.getInt("id"));
        r.setUser(rs.getString("user"));
        r.setDate(rs.getString("date"));
        r.setStartTime(rs.getString("start_time"));
        r.setEndTime(rs.getString("end_time"));
        r.setQuantity(rs.getInt("quantity"));
        r.setStatus(rs.getString("status"));
        java.sql.Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) r.setTimestamp(createdAt.getTime());
        return r;
    }
}
