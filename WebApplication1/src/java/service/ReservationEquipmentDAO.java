package service;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class ReservationEquipmentDAO {

    public void save(int reservationId, int equipmentId, int quantity, Connection con) throws Exception {

        String sql = "INSERT INTO reservation_equipment (reservation_id, equipment_id, quantity) VALUES (?, ?, ?)";

        PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, reservationId);
        ps.setInt(2, equipmentId);
        ps.setInt(3, quantity);

        ps.executeUpdate();
    }
}