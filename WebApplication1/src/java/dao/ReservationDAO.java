package dao;

import model.Reservation;
import util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import model.ReservationEquipment;

public class ReservationDAO {
   
private boolean ensureUserExists(Connection con, String email) throws SQLException {
    String checkSql = "SELECT COUNT(*) FROM users WHERE email = ?";
    try (PreparedStatement checkPs = con.prepareStatement(checkSql)) {
        checkPs.setString(1, email);
        ResultSet rs = checkPs.executeQuery();
        if (rs.next() && rs.getInt(1) > 0) {
            return true; 
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

        try (Connection con = DBConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, user);
            ps.setString(2, date);
            ps.setString(3, start);
            ps.setString(4, end);
            ps.setInt(5, quantity);

            int rows = ps.executeUpdate();
            return rows > 0;

        } catch (SQLException e) {
            System.err.println("[DAO ERROR] create: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public List<Reservation> getAll() {
        List<Reservation> list = new ArrayList<>();
        String sql = "SELECT * FROM reservations WHERE status != 'EXPIRED' ORDER BY date, start_time";

        try (Connection con = DBConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapReservation(rs));
            }

        } catch (SQLException e) {
            System.err.println("[DAO ERROR] getAll: " + e.getMessage());
        }

        return list;
    }

    public List<Reservation> getByDate(String date) {
        List<Reservation> list = new ArrayList<>();
        String sql = "SELECT * FROM reservations WHERE date = ? AND status != 'EXPIRED'";

        try (Connection con = DBConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {

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

    public Reservation getById(int id) {
        String sql = "SELECT * FROM reservations WHERE id = ?";

        try (Connection con = DBConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {

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

    public boolean confirm(int id) {
        return updateStatus(id, "CONFIRMED");
    }

    public boolean update(Reservation reservation) {
        String sql = "UPDATE reservations SET date=?, start_time=?, end_time=?, quantity=?, status=? WHERE id=?";

        try (Connection con = DBConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, reservation.getDate());
            ps.setString(2, reservation.getStartTime());
            ps.setString(3, reservation.getEndTime());
            ps.setInt(4, reservation.getQuantity());
            ps.setString(5, reservation.getStatus());
            ps.setInt(6, reservation.getId());

            int rows = ps.executeUpdate();
            return rows > 0;

        } catch (SQLException e) {
            System.err.println("[DAO ERROR] update: " + e.getMessage());
            return false;
        }
    }

    public boolean updateStatus(int id, String status) {
        String sql = "UPDATE reservations SET status = ? WHERE id = ?";

        try (Connection con = DBConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, status);
            ps.setInt(2, id);

            int rows = ps.executeUpdate();
            return rows > 0;

        } catch (SQLException e) {
            System.err.println("[DAO ERROR] updateStatus: " + e.getMessage());
            return false;
        }
    }

    public boolean delete(int id) {
        Connection con = null;
        try {
            con = DBConnection.getConnection();
            con.setAutoCommit(false);

            equipmentDAO.deleteByReservationId(id);

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

    public boolean existsOverlap(String date, String start, String end) {
        String sql = "SELECT COUNT(*) FROM reservations "
                + "WHERE date = ? AND status != 'EXPIRED' "
                + "AND start_time < ? AND end_time > ?";

        try (Connection con = DBConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {

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

    public int cleanExpired(int ttlMinutes) {
        String sql = "UPDATE reservations "
                + "SET status = 'EXPIRED' "
                + "WHERE status = 'PENDING' "
                + "AND TIMESTAMPDIFF(MINUTE, created_at, NOW()) > ?";

        try (Connection con = DBConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, ttlMinutes);

            int rows = ps.executeUpdate();
            System.out.println("[DAO] Reservas expiradas: " + rows);
            return rows;

        } catch (SQLException e) {
            System.err.println("[DAO ERROR] cleanExpired: " + e.getMessage());
            return 0;
        }
    }

    public int cleanExpired() {
        return cleanExpired(10);
    }

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

    private ReservationEquipmentDAO equipmentDAO;

    public ReservationDAO() {
        this.equipmentDAO = new ReservationEquipmentDAO();
    }

public boolean createWithEquipment(String user, String date, String start, String end, 
                                   int quantity, List<ReservationEquipment> equipments) {
    Connection con = null;
    try {
        con = DBConnection.getConnection();
        con.setAutoCommit(false);
        
        if (!ensureUserExists(con, user)) {
            System.err.println("[DAO ERROR] No se pudo crear el usuario: " + user);
            con.rollback();
            return false;
        }
        
        String sqlRes = "INSERT INTO reservations (user, date, start_time, end_time, quantity, status, created_at) " +
                       "VALUES (?, ?, ?, ?, ?, 'PENDING', NOW())";
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
            
            ResultSet rs = ps.getGeneratedKeys();
            reservationId = rs.next() ? rs.getInt(1) : -1;
            rs.close();
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
        try { if (con != null) con.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
        return false;
    } finally {
        try {
            if (con != null) {
                con.setAutoCommit(true);
                con.close();
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }
}

    public Reservation getByIdWithEquipment(int id) {
        Reservation res = getById(id);
        if (res != null) {
            res.setEquipments(equipmentDAO.getByReservationId(id));
        }
        return res;
    }
}
