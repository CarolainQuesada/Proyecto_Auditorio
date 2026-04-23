package dao;

import model.ReservationEquipment;
import util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReservationEquipmentDAO {

    public boolean create(Connection con, ReservationEquipment re) {
        String sql = "INSERT INTO reservation_equipment (reservation_id, equipment_id, quantity) VALUES (?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            ps.setInt(1, re.getReservationId());
            ps.setInt(2, re.getEquipmentId());
            ps.setInt(3, re.getQuantity());
            
            int rows = ps.executeUpdate();
            if (rows > 0) {
                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) re.setId(rs.getInt(1));
                rs.close();
                return true;
            }
            return false;
        } catch (SQLException e) {
            System.err.println("[DAO ERROR] ReservationEquipment create: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public List<ReservationEquipment> getByReservationId(int reservationId) {
        List<ReservationEquipment> list = new ArrayList<>();
        String sql = "SELECT * FROM reservation_equipment WHERE reservation_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            ps.setInt(1, reservationId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            System.err.println("[DAO ERROR] getByReservationId: " + e.getMessage());
        }
        return list;
    }

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

    public boolean delete(int reservationId, int equipmentId) {
        String sql = "DELETE FROM reservation_equipment WHERE reservation_id = ? AND equipment_id = ?";
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

    public boolean updateQuantity(int reservationId, int equipmentId, int quantity) {
        String sql = "UPDATE reservation_equipment SET quantity = ? WHERE reservation_id = ? AND equipment_id = ?";
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

    private ReservationEquipment map(ResultSet rs) throws SQLException {
        ReservationEquipment re = new ReservationEquipment();
        re.setId(rs.getInt("id"));
        re.setReservationId(rs.getInt("reservation_id"));
        re.setEquipmentId(rs.getInt("equipment_id"));
        re.setQuantity(rs.getInt("quantity"));
        return re;
    }
}