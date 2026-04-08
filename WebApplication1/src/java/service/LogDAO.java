package service;

import util.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;

public class LogDAO {

    public void register(String user, String action, String description) {

        try (Connection con = DBConnection.getConnection()) {

            String sql = "INSERT INTO log (user, action, description) VALUES (?, ?, ?)";

            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, user);
            ps.setString(2, action);
            ps.setString(3, description);

            ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}