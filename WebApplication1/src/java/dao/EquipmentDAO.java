package dao;

import util.DBConnection;
import java.sql.*;

public class EquipmentDAO {

    public int getEquipmentIdByName(String name) {
        String sql = "SELECT id FROM equipamiento WHERE nombre = ?";
        
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

    public boolean updateAvailability(int equipmentId, int newQuantity) {
        String sql = "UPDATE equipamiento SET cantidad_disponible = ? WHERE id = ?";
        
        try (Connection con = DBConnection.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            
            stmt.setInt(1, newQuantity);
            stmt.setInt(2, equipmentId);
            
            int rows = stmt.executeUpdate();
            return rows > 0;
            
        } catch (SQLException e) {
            System.err.println("[DAO ERROR] updateAvailability: " + e.getMessage());
            return false;
        }
    }

    public boolean insertReservationEquipment(int reservationId, int equipmentId, int quantity) {
        String sql = "INSERT INTO reserva_equipamiento (id_reserva, id_equipamiento, cantidad) VALUES (?, ?, ?)";
        
        try (Connection con = DBConnection.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            
            stmt.setInt(1, reservationId);
            stmt.setInt(2, equipmentId);
            stmt.setInt(3, quantity);
            
            int rows = stmt.executeUpdate();
            return rows > 0;
            
        } catch (SQLException e) {
            System.err.println("[DAO ERROR] insertReservationEquipment: " + e.getMessage());
            return false;
        }
    }
}