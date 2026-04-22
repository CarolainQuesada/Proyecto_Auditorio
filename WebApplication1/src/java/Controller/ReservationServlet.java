package Controller;

import util.SocketClient; 
import javax.servlet.*;
import javax.servlet.http.*;
import java.io.IOException;
import javax.servlet.annotation.WebServlet;

@WebServlet("/ReservationServlet") 
public class ReservationServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        HttpSession session = request.getSession();
        String user = (String) session.getAttribute("emailUsuario");
        
        if (user == null) {
            response.sendRedirect("index.html");
            return;
        }

        String date = request.getParameter("date");
        String start = request.getParameter("start_time");
        String end = request.getParameter("end_time");
        String quantity = request.getParameter("quantity");
        String equipment = request.getParameter("equipment");
        String eqQty = request.getParameter("eqQty");

        String command = String.format("RESERVAR;%s;%s;%s;%s;%s;%s;%s", 
                                       user, date, start, end, quantity, equipment, eqQty);
        
        System.out.println("[Servlet] Enviando: " + command);
        
        String resultado = SocketClient.sendCommand(command); 

        if (resultado.equals("created")) {
            response.sendRedirect("dashboard.html?msg=ok"); 
        } else {
            String msg = "error";
            if (resultado.equals("busy_time")) msg = "busy";
            if (resultado.equals("busy_capacity")) msg = "busy_capacity";
            if (resultado.equals("busy_equipment")) msg = "busy_equipment";
            if (resultado.equals("hour")) msg = "hour";
            
            response.sendRedirect("dashboard.html?msg=" + msg);
        }
    }
}