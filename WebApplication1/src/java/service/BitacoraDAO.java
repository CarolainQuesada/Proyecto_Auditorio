package service;

import util.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;

public class BitacoraDAO {

    public void registrar(String usuario, String accion, String descripcion) {

        try (Connection con = DBConnection.getConnection()) {

            String sql = "INSERT INTO bitacora (usuario, accion, descripcion) VALUES (?, ?, ?)";

            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, usuario);
            ps.setString(2, accion);
            ps.setString(3, descripcion);

            ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}