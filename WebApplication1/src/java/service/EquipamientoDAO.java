package service;

import java.sql.*;

public class EquipamientoDAO {

    public boolean reducirDisponibles(int id, int cantidad, Connection con) throws Exception {

        String check = "SELECT cantidad_disponible FROM equipamiento WHERE id=? FOR UPDATE";
        PreparedStatement ps = con.prepareStatement(check);
        ps.setInt(1, id);

        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            int disponible = rs.getInt("cantidad_disponible");

            if (disponible < cantidad) {
                return false;
            }
        }

        String update = "UPDATE equipamiento SET cantidad_disponible = cantidad_disponible - ? WHERE id=?";
        ps = con.prepareStatement(update);
        ps.setInt(1, cantidad);
        ps.setInt(2, id);

        ps.executeUpdate();

        return true;
    }
}