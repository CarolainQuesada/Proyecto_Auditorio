package dao;

import util.DBConnection;
import model.Log;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO de la Bitácora del Sistema.
 *
 * Maneja la persistencia de eventos en la tabla `log` de MySQL.
 * La sincronización se gestiona desde SystemLog; aquí solo se
 * garantiza el acceso correcto a la base de datos.
 */
public class LogDAO {

    /**
     * Inserta un nuevo registro en la bitácora.
     */
    public void register(String user, String action, String description) {
        String sql = "INSERT INTO log (user, action, description, created_at) VALUES (?, ?, ?, NOW())";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, user != null ? user : "SYSTEM");
            ps.setString(2, action != null ? action : "UNKNOWN");
            ps.setString(3, description != null ? description : "");
            ps.executeUpdate();

        } catch (Exception e) {
            System.err.println("[LogDAO ERROR] register: " + e.getMessage());
        }
    }

    /**
     * Recupera todos los registros ordenados por fecha descendente.
     */
    public List<Log> getAll() {
        List<Log> list = new ArrayList<>();
        String sql = "SELECT * FROM log ORDER BY created_at DESC";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(map(rs));
            }

        } catch (SQLException e) {
            System.err.println("[LogDAO ERROR] getAll: " + e.getMessage());
        }
        return list;
    }

    /**
     * Recupera registros filtrados por usuario.
     */
    public List<Log> getByUser(String user) {
        List<Log> list = new ArrayList<>();
        String sql = "SELECT * FROM log WHERE user = ? ORDER BY created_at DESC";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, user);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }

        } catch (SQLException e) {
            System.err.println("[LogDAO ERROR] getByUser: " + e.getMessage());
        }
        return list;
    }

    /**
     * Recupera registros filtrados por tipo de acción.
     */
    public List<Log> getByAction(String action) {
        List<Log> list = new ArrayList<>();
        String sql = "SELECT * FROM log WHERE action = ? ORDER BY created_at DESC";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, action);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }

        } catch (SQLException e) {
            System.err.println("[LogDAO ERROR] getByAction: " + e.getMessage());
        }
        return list;
    }

    private Log map(ResultSet rs) throws SQLException {
        Log log = new Log();
        log.setId(rs.getInt("id"));
        log.setUser(rs.getString("user"));
        log.setAction(rs.getString("action"));
        log.setDescription(rs.getString("description"));
        log.setCreatedAt(rs.getString("created_at"));
        return log;
    }
}