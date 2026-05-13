package Controller;

import util.DBConnection;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;

@WebServlet("/blockDay")
public class BlockDayServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String fecha = req.getParameter("date");
        String motivo = req.getParameter("reason");

        try (Connection con = DBConnection.getConnection()) {
            String sql = "INSERT INTO blocked_days (fecha, motivo) VALUES (?, ?)";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, fecha);
            ps.setString(2, motivo);
            ps.executeUpdate();
            resp.sendRedirect("admin.html?msg=day_blocked");
        } catch (Exception e) {
            e.printStackTrace();
            resp.sendRedirect("admin.html?msg=error");
        }
    }
}