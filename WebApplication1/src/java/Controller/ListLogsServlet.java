package Controller;

import dao.LogDAO;
import model.Log;
import java.io.IOException;
import java.util.List;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

@WebServlet("/listLogs")
public class ListLogsServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        resp.setContentType("text/plain;charset=UTF-8");

        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("role") == null) {
            resp.setStatus(401);
            resp.getWriter().write("unauthorized");
            return;
        }

        String role = (String) session.getAttribute("role");
        if (!"ADMIN".equals(role)) {
            resp.setStatus(403);
            resp.getWriter().write("forbidden");
            return;
        }

        try {
            LogDAO dao = new LogDAO();
            List<Log> logs = dao.getAll();
            StringBuilder sb = new StringBuilder();

            for (Log log : logs) {
                sb.append(log.getId()).append("~")
                  .append(log.getCreatedAt()).append("~")
                  .append(log.getUser()).append("~")
                  .append(log.getAction()).append("~")
                  .append(log.getDescription())
                  .append("|");
            }

            resp.getWriter().write(sb.toString());

        } catch (Exception e) {
            e.printStackTrace();
            resp.getWriter().write("ERROR:" + e.getMessage());
        }
    }
}