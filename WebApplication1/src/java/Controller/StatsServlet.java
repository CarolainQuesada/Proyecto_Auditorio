package Controller;

import util.DBConnection;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.*;
import java.util.Arrays;

@WebServlet("/stats")
public class StatsServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");

        String range = normalizeRange(req.getParameter("range"));
        try (Connection con = DBConnection.getConnection()) {
            int[] counts = new int[7];
            String[] labels = {"Lun", "Mar", "Mie", "Jue", "Vie", "Sab", "Dom"};
            int[] metrics = loadMetrics(con);

            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT DAYOFWEEK(date) AS dow, COUNT(*) AS total "
                            + "FROM reservations " + rangeWhere(range)
                            + " GROUP BY dow")) {

                bindRange(ps, range);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        int dow = rs.getInt("dow");
                        int idx = (dow == 1) ? 6 : dow - 2;
                        if (idx >= 0 && idx < 7) {
                            counts[idx] = rs.getInt("total");
                        }
                    }
                }
            }

            String[] statusData = loadStatusData(con, range);
            String json = String.format(
                    "{\"labels\":%s,\"counts\":%s,\"statusLabels\":%s,\"statusCounts\":%s,"
                            + "\"metrics\":{\"total\":%d,\"pending\":%d,\"confirmed\":%d,\"expired\":%d}}",
                    toJsonArray(labels),
                    Arrays.toString(counts),
                    statusData[0],
                    statusData[1],
                    metrics[0],
                    metrics[1],
                    metrics[2],
                    metrics[3]
            );
            resp.getWriter().write(json);
        } catch (SQLException e) {
            System.err.println("[StatsServlet] Error DB: " + e.getMessage());
            resp.getWriter().write("{\"labels\":[\"Lun\",\"Mar\",\"Mie\",\"Jue\","
                    + "\"Vie\",\"Sab\",\"Dom\"],\"counts\":[0,0,0,0,0,0,0],"
                    + "\"statusLabels\":[],\"statusCounts\":[],"
                    + "\"metrics\":{\"total\":0,\"pending\":0,\"confirmed\":0,\"expired\":0}}");
        }
    }

    private int[] loadMetrics(Connection con) throws SQLException {
        int[] metrics = new int[4];
        String sql = "SELECT COUNT(*) AS total, "
                + "SUM(status = 'PENDING') AS pending, "
                + "SUM(status = 'CONFIRMED') AS confirmed, "
                + "SUM(status = 'EXPIRED') AS expired "
                + "FROM reservations";

        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                metrics[0] = rs.getInt("total");
                metrics[1] = rs.getInt("pending");
                metrics[2] = rs.getInt("confirmed");
                metrics[3] = rs.getInt("expired");
            }
        }

        return metrics;
    }

    private String[] loadStatusData(Connection con, String range) throws SQLException {
        StringBuilder statusLabels = new StringBuilder("[");
        StringBuilder statusCounts = new StringBuilder("[");
        boolean first = true;

        try (PreparedStatement ps = con.prepareStatement(
                "SELECT status, COUNT(*) AS total FROM reservations "
                        + rangeWhere(range) + " GROUP BY status")) {

            bindRange(ps, range);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    if (!first) {
                        statusLabels.append(",");
                        statusCounts.append(",");
                    }
                    statusLabels.append("\"").append(escapeJson(rs.getString("status"))).append("\"");
                    statusCounts.append(rs.getInt("total"));
                    first = false;
                }
            }
        }

        statusLabels.append("]");
        statusCounts.append("]");
        return new String[]{statusLabels.toString(), statusCounts.toString()};
    }

    private String normalizeRange(String range) {
        if ("week".equalsIgnoreCase(range)
                || "month".equalsIgnoreCase(range)
                || "year".equalsIgnoreCase(range)) {
            return range.toLowerCase();
        }
        return "all";
    }

    private String rangeWhere(String range) {
        if ("week".equals(range)) {
            return "WHERE YEARWEEK(date, 1) = YEARWEEK(CURDATE(), 1)";
        }
        if ("month".equals(range)) {
            return "WHERE YEAR(date) = YEAR(CURDATE()) AND MONTH(date) = MONTH(CURDATE())";
        }
        if ("year".equals(range)) {
            return "WHERE YEAR(date) = YEAR(CURDATE())";
        }
        return "";
    }

    private void bindRange(PreparedStatement ps, String range) throws SQLException {
        // Range filters are expressed with CURDATE() in SQL, so no parameters are needed.
    }

    private String toJsonArray(String[] values) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                json.append(",");
            }
            json.append("\"").append(escapeJson(values[i])).append("\"");
        }
        return json.append("]").toString();
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
