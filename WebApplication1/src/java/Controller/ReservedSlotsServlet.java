package Controller;

import dao.ReservationDAO;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import model.Reservation;

@WebServlet("/reservedSlots")
public class ReservedSlotsServlet extends HttpServlet {

    private final ReservationDAO reservationDAO = new ReservationDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        resp.setContentType("text/plain;charset=UTF-8");

        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("emailUsuario") == null) {
            resp.setStatus(401);
            resp.getWriter().write("ERROR: unauthorized");
            return;
        }

        String date = req.getParameter("date");
        if (date == null || date.trim().isEmpty()) {
            resp.setStatus(400);
            resp.getWriter().write("ERROR: date");
            return;
        }

        date = date.trim();
        try {
            LocalDate.parse(date);
        } catch (Exception e) {
            resp.setStatus(400);
            resp.getWriter().write("ERROR: date");
            return;
        }

        List<Reservation> reservations = reservationDAO.getByDate(date);
        StringBuilder sb = new StringBuilder();

        for (Reservation reservation : reservations) {
            sb.append(reservation.getStartTime()).append(",")
              .append(reservation.getEndTime()).append(",")
              .append(reservation.getStatus()).append("|");
        }

        resp.getWriter().write(sb.toString());
    }
}
