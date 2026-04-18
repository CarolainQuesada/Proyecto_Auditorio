package dao;

import model.Reservation;
import util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReservationDAO {

    /**
     * Crea una reserva. Retorna true si se insertó correctamente.
     */
    public boolean create(String user, String date, String start, String end, int quantity) {
        String sql = "INSERT INTO reservations (user, date, start_time, end_time, quantity, status, created_at) VALUES (?, ?, ?, ?, ?, 'PENDING', NOW())";
        
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
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

    /**
     * Obtiene todas las reservas activas.
     */
    public List<Reservation> getAll() {
        List<Reservation> list = new ArrayList<>();
        String sql = "SELECT * FROM reservations WHERE status != 'EXPIRED' ORDER BY date, start_time";
        
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
     * Obtiene reservas por fecha (excluye expiradas).
     */
    public List<Reservation> getByDate(String date) {
        List<Reservation> list = new ArrayList<>();
        String sql = "SELECT * FROM reservations WHERE date = ? AND status != 'EXPIRED'";
        
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
     * Obtiene reserva por ID.
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
     * Confirma una reserva pendiente.
     */
    public boolean confirm(int id) {
        return updateStatus(id, "CONFIRMED");
    }

    /**
     * Actualiza una reserva completa.
     */
    public boolean update(Reservation reservation) {
        String sql = "UPDATE reservations SET date=?, start_time=?, end_time=?, quantity=? WHERE id=?";
        
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            ps.setString(1, reservation.getDate());
            ps.setString(2, reservation.getStartTime());
            ps.setString(3, reservation.getEndTime());
            ps.setInt(4, reservation.getQuantity());
            ps.setInt(5, reservation.getId());
            
            int rows = ps.executeUpdate();
            return rows > 0;
            
        } catch (SQLException e) {
            System.err.println("[DAO ERROR] update: " + e.getMessage());
            return false;
        }
    }

    /**
     * Actualiza estado de una reserva.
     */
    public boolean updateStatus(int id, String status) {
        String sql = "UPDATE reservations SET status = ? WHERE id = ?";
        
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            ps.setString(1, status);
            ps.setInt(2, id);
            
            int rows = ps.executeUpdate();
            return rows > 0;
            
        } catch (SQLException e) {
            System.err.println("[DAO ERROR] updateStatus: " + e.getMessage());
            return false;
        }
    }

    /**
     * Elimina una reserva.
     */
    public boolean delete(int id) {
        String sql = "DELETE FROM reservations WHERE id = ?";
        
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            ps.setInt(1, id);
            
            int rows = ps.executeUpdate();
            return rows > 0;
            
        } catch (SQLException e) {
            System.err.println("[DAO ERROR] delete: " + e.getMessage());
            return false;
        }
    }

    /**
     * Verifica si existe solapamiento de horario para una fecha.
     * Usa FOR UPDATE para locking pesimista (opcional, según tu motor BD).
     */
    public boolean existsOverlap(String date, String start, String end) {
        String sql = "SELECT COUNT(*) FROM reservations " +
                     "WHERE date = ? AND status != 'EXPIRED' " +
                     "AND start_time < ? AND end_time > ?";
        
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
        
        return false; // Asume que no hay conflicto si hay error
    }

    /**
     * Marca reservas pendientes como EXPIRADAS según TTL.
     */
    public int cleanExpired(int ttlMinutes) {
        String sql = "UPDATE reservations " +
                     "SET status = 'EXPIRED' " +
                     "WHERE status = 'PENDING' " +
                     "AND TIMESTAMPDIFF(MINUTE, created_at, NOW()) > ?";
        
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
     * Versión por defecto: 10 minutos TTL.
     */
    public int cleanExpired() {
        return cleanExpired(10);
    }

    /**
     * Mapea ResultSet a objeto Reservation.
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