package Controller;

import java.io.IOException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import service.ReservationService;

/**
 * Lists reservations for the admin panel.
 */
@WebServlet("/listReservations")
public class ListReservationsServlet extends HttpServlet {

    private final ReservationService reservationService = new ReservationService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        resp.setContentType("text/plain;charset=UTF-8");

        HttpSession session = req.getSession(false);

        if (session == null || session.getAttribute("role") == null) {
            resp.setStatus(401);
            resp.getWriter().write("ERROR: unauthorized");
            return;
        }

        String role = session.getAttribute("role").toString();

        if (!"ADMIN".equalsIgnoreCase(role)) {
            resp.setStatus(403);
            resp.getWriter().write("ERROR: forbidden");
            return;
        }

        try {
            int page = parseInt(req.getParameter("page"), 1);
            int size = parseInt(req.getParameter("size"), 10);
            String search = req.getParameter("search");
            String status = req.getParameter("status");
            String date = req.getParameter("date");

            page = Math.max(page, 1);
            size = Math.max(1, Math.min(size, 50));

            int total = reservationService.countActiveReservations(search, status, date);
            resp.setHeader("X-Total-Count", String.valueOf(total));
            resp.setHeader("X-Page", String.valueOf(page));
            resp.setHeader("X-Page-Size", String.valueOf(size));
            resp.getWriter().write(reservationService.listReservations(page, size, search, status, date));
        } catch (Exception e) {
            System.err.println("[ListReservations] ERROR: " + e.getMessage());
            e.printStackTrace();
            resp.setStatus(500);
            resp.getWriter().write("ERROR: server");
        }
    }

    private int parseInt(String value, int fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
