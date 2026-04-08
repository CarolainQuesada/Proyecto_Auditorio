package service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class EquipmentDAO {

    public boolean reduceAvailable(int id, int quantity, Connection con) throws Exception {

        String checkSql = "SELECT available_quantity FROM equipment WHERE id=? FOR UPDATE";
        PreparedStatement ps = con.prepareStatement(checkSql);
        ps.setInt(1, id);

        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            int available = rs.getInt("available_quantity");

            if (available < quantity) {
                return false;
            }
        }

        String updateSql = "UPDATE equipment SET available_quantity = available_quantity - ? WHERE id=?";
        ps = con.prepareStatement(updateSql);
        ps.setInt(1, quantity);
        ps.setInt(2, id);

        ps.executeUpdate();

        return true;
    }
}