package Controller;

import util.DBConnection;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.*;

@WebServlet("/stats")
public class StatsServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        
        try (Connection con = DBConnection.getConnection()) {
            int[] counts = new int[7];
            String[] labels = {"Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom"};
            
            PreparedStatement ps = con.prepareStatement(
                "SELECT DAYOFWEEK(date) as dow, COUNT(*) as total FROM "
                        + "reservations GROUP BY dow"
            );
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int dow = rs.getInt("dow"); 
                int idx = (dow == 1) ? 6 : dow - 2; 
                if (idx >= 0 && idx < 7) counts[idx] = rs.getInt("total");
            }

            PreparedStatement ps2 = con.prepareStatement(
                "SELECT status, COUNT(*) as total FROM reservations GROUP BY status"
            );
            ResultSet rs2 = ps2.executeQuery();
            StringBuilder statusLabels = new StringBuilder("[");
            StringBuilder statusCounts = new StringBuilder("[");
            boolean first = true;
            while (rs2.next()) {
                if (!first) { statusLabels.append(","); statusCounts.append(","); }
                statusLabels.append("\"").append(rs2.getString("status")).append("\"");
                statusCounts.append(rs2.getInt("total"));
                first = false;
            }
            statusLabels.append("]");
            statusCounts.append("]");

            String json = String.format(
                "{\"labels\": %s, \"counts\": %s, \"statusLabels\": %s, \"statusCounts\": %s}",
                java.util.Arrays.toString(labels).replace("[", "[").replace("]", "]"),
                java.util.Arrays.toString(counts),
                statusLabels,
                statusCounts
            );
            resp.getWriter().write(json);

        } catch (SQLException e) {
            System.err.println("[StatsServlet] Error DB: " + e.getMessage());
            resp.getWriter().write("{\"labels\":[\"Lun\",\"Mar\",\"Mié\",\"Jue\""
                    + ",\"Vie\",\"Sáb\",\"Dom\"],\"counts\":[0,0,0,0,0,0,0],\""
                    + "statusLabels\":[],\"statusCounts\":[]}");
            
            
        }
    }
}