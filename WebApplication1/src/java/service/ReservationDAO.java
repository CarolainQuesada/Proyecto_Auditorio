package service;

import model.Reservation;
import util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReservationDAO {

    public List<Reservation> getReservations() {

        List<Reservation> list = new ArrayList<>();

        try (Connection con = DBConnection.getConnection()) {

            String sql = "SELECT * FROM reservations";
            ResultSet rs = con.prepareStatement(sql).executeQuery();

            while (rs.next()) {
                Reservation r = new Reservation();

                r.setId(rs.getInt("id"));
                r.setUser(rs.getString("user"));
                r.setDate(rs.getString("date"));
                r.setStartTime(rs.getString("start_time"));
                r.setEndTime(rs.getString("end_time"));
                r.setQuantity(rs.getInt("quantity"));
                r.setStatus(rs.getString("status"));

                list.add(r);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    public List<Reservation> getAll() {
        return getReservations();
    }

    public List<Reservation> getByDate(String date) {

        List<Reservation> list = new ArrayList<>();

        try (Connection con = DBConnection.getConnection()) {

            String sql = "SELECT * FROM reservations WHERE date=? AND status != 'EXPIRED'";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, date);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Reservation r = new Reservation();

                r.setId(rs.getInt("id"));
                r.setUser(rs.getString("user"));
                r.setDate(rs.getString("date"));
                r.setStartTime(rs.getString("start_time"));
                r.setEndTime(rs.getString("end_time"));
                r.setQuantity(rs.getInt("quantity"));
                r.setStatus(rs.getString("status"));

                list.add(r);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    public void create(String user, String date, String start, String end, int quantity) {

        try (Connection con = DBConnection.getConnection()) {

            String sql = "INSERT INTO reservations (user, date, start_time, end_time, quantity, status) VALUES (?, ?, ?, ?, ?, 'PENDING')";

            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, user);
            ps.setString(2, date);
            ps.setString(3, start);
            ps.setString(4, end);
            ps.setInt(5, quantity);

            ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean existsOverlap(String date, String start, String end, Connection con) throws Exception {

        String sql = "SELECT * FROM reservations " +
                     "WHERE date=? AND status != 'EXPIRED' " +
                     "AND (start_time < ? AND end_time > ?) FOR UPDATE";

        PreparedStatement ps = con.prepareStatement(sql);
        ps.setString(1, date);
        ps.setString(2, end);
        ps.setString(3, start);

        ResultSet rs = ps.executeQuery();

        return rs.next();
    }

    public Reservation getById(int id) {

        try (Connection con = DBConnection.getConnection()) {

            String sql = "SELECT * FROM reservations WHERE id=?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, id);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                Reservation r = new Reservation();

                r.setId(id);
                r.setUser(rs.getString("user"));
                r.setDate(rs.getString("date"));
                r.setStartTime(rs.getString("start_time"));
                r.setEndTime(rs.getString("end_time"));
                r.setQuantity(rs.getInt("quantity"));
                r.setStatus(rs.getString("status"));

                return r;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public void confirm(int id) {
        updateStatus(id, "CONFIRMED");
    }

    public boolean update(Reservation reservation) {

        try (Connection con = DBConnection.getConnection()) {

            String sql = "UPDATE reservations SET date=?, start_time=?, end_time=?, quantity=? WHERE id=?";
            PreparedStatement ps = con.prepareStatement(sql);

            ps.setString(1, reservation.getDate());
            ps.setString(2, reservation.getStartTime());
            ps.setString(3, reservation.getEndTime());
            ps.setInt(4, reservation.getQuantity());
            ps.setInt(5, reservation.getId());

            int rows = ps.executeUpdate();
            return rows > 0;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void updateStatus(int id, String status) {

        try (Connection con = DBConnection.getConnection()) {

            String sql = "UPDATE reservations SET status=? WHERE id=?";
            PreparedStatement ps = con.prepareStatement(sql);

            ps.setString(1, status);
            ps.setInt(2, id);

            ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void delete(int id) {

        try (Connection con = DBConnection.getConnection()) {

            String sql = "DELETE FROM reservations WHERE id=?";
            PreparedStatement ps = con.prepareStatement(sql);

            ps.setInt(1, id);
            ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void cleanExpired(int ttlMinutes) {

        try (Connection con = DBConnection.getConnection()) {

            String sql = "UPDATE reservations " +
                         "SET status='EXPIRED' " +
                         "WHERE status='PENDING' " +
                         "AND TIMESTAMPDIFF(MINUTE, created_at, NOW()) > ?";

            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, ttlMinutes);

            int rows = ps.executeUpdate();

            if (rows > 0) {
                System.out.println("Expired reservations: " + rows);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void cleanExpired() {
        cleanExpired(10);
    }
}