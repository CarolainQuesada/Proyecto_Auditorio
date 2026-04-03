package service;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class ReservaEquipamientoDAO {

    public void guardar(int idReserva, int idEquipamiento, int cantidad, Connection con) throws Exception {

        String sql = "INSERT INTO reserva_equipamiento (id_reserva, id_equipamiento, cantidad) VALUES (?, ?, ?)";

        PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, idReserva);
        ps.setInt(2, idEquipamiento);
        ps.setInt(3, cantidad);

        ps.executeUpdate();
    }
}