package service;

import model.Reserva;
import util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReservaDAO {

    public List<Reserva> obtenerReservas() {

        List<Reserva> lista = new ArrayList<>();

        try (Connection con = DBConnection.getConnection()) {

            String sql = "SELECT * FROM reservas";
            ResultSet rs = con.prepareStatement(sql).executeQuery();

            while (rs.next()) {
                Reserva r = new Reserva();

                r.setId(rs.getInt("id"));
                r.setFecha(rs.getString("fecha"));
                r.setHoraInicio(rs.getString("hora_inicio"));
                r.setHoraFin(rs.getString("hora_fin"));
                r.setCantidad(rs.getInt("cantidad"));
                r.setEstado(rs.getString("estado"));

                lista.add(r);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return lista;
    }

    public List<Reserva> obtenerTodas() {
        return obtenerReservas();
    }

    public List<Reserva> obtenerPorFecha(String fecha) {

        List<Reserva> lista = new ArrayList<>();

        try (Connection con = DBConnection.getConnection()) {

            String sql = "SELECT * FROM reservas WHERE fecha=? AND estado != 'EXPIRADA'";
            PreparedStatement ps = con.prepareStatement(sql);

            ps.setString(1, fecha);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Reserva r = new Reserva();

                r.setId(rs.getInt("id"));
                r.setFecha(rs.getString("fecha"));
                r.setHoraInicio(rs.getString("hora_inicio"));
                r.setHoraFin(rs.getString("hora_fin"));
                r.setCantidad(rs.getInt("cantidad"));
                r.setEstado(rs.getString("estado"));

                lista.add(r);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return lista;
    }

    public void crear(String fecha, String inicio, String fin, int cantidad) {

        try (Connection con = DBConnection.getConnection()) {

            String sql = "INSERT INTO reservas (fecha, hora_inicio, hora_fin, cantidad, estado) VALUES (?, ?, ?, ?, 'PENDIENTE')";

            PreparedStatement ps = con.prepareStatement(sql);

            ps.setString(1, fecha);
            ps.setString(2, inicio);
            ps.setString(3, fin);
            ps.setInt(4, cantidad);

            ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean existeSolapamiento(String fecha, String inicio, String fin, Connection con) throws Exception {

        String sql = "SELECT * FROM reservas " +
                     "WHERE fecha=? AND estado != 'EXPIRADA' " +
                     "AND (hora_inicio < ? AND hora_fin > ?) FOR UPDATE";

        PreparedStatement ps = con.prepareStatement(sql);
        ps.setString(1, fecha);
        ps.setString(2, fin);
        ps.setString(3, inicio);

        ResultSet rs = ps.executeQuery();

        return rs.next();
    }

    public Reserva obtenerPorId(int id) {

        try (Connection con = DBConnection.getConnection()) {

            String sql = "SELECT * FROM reservas WHERE id=?";
            PreparedStatement ps = con.prepareStatement(sql);

            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                Reserva r = new Reserva();

                r.setId(id);
                r.setFecha(rs.getString("fecha"));
                r.setHoraInicio(rs.getString("hora_inicio"));
                r.setHoraFin(rs.getString("hora_fin"));
                r.setCantidad(rs.getInt("cantidad"));
                r.setEstado(rs.getString("estado"));

                return r;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public void confirmar(int id) {
        actualizarEstado(id, "CONFIRMADA");
    }

    public void actualizar(Reserva r) {

        try (Connection con = DBConnection.getConnection()) {

            String sql = "UPDATE reservas SET fecha=?, hora_inicio=?, hora_fin=?, cantidad=? WHERE id=?";

            PreparedStatement ps = con.prepareStatement(sql);

            ps.setString(1, r.getFecha());
            ps.setString(2, r.getHoraInicio());
            ps.setString(3, r.getHoraFin());
            ps.setInt(4, r.getCantidad());
            ps.setInt(5, r.getId());

            ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void actualizarEstado(int id, String estado) {

        try (Connection con = DBConnection.getConnection()) {

            String sql = "UPDATE reservas SET estado=? WHERE id=?";
            PreparedStatement ps = con.prepareStatement(sql);

            ps.setString(1, estado);
            ps.setInt(2, id);

            ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void eliminar(int id) {

        try (Connection con = DBConnection.getConnection()) {

            String sql = "DELETE FROM reservas WHERE id=?";
            PreparedStatement ps = con.prepareStatement(sql);

            ps.setInt(1, id);
            ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void limpiarExpiradas(int minutosTTL) {

        try (Connection con = DBConnection.getConnection()) {

            String sql = "UPDATE reservas " +
                         "SET estado='EXPIRADA' " +
                         "WHERE estado='PENDIENTE' " +
                         "AND TIMESTAMPDIFF(MINUTE, fecha_creacion, NOW()) > ?";

            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, minutosTTL);

            int filas = ps.executeUpdate();

            if (filas > 0) {
                System.out.println("🧹 Expiradas: " + filas);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void limpiarExpiradas() {
        limpiarExpiradas(10);
    }
}