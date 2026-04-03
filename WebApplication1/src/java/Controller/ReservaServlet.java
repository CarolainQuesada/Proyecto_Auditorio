package Controller;

import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.WebServlet;
import java.net.*;

@WebServlet("/reservar")
public class ReservaServlet extends HttpServlet {

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        String usuario = (String) request.getSession().getAttribute("usuario"); 
        String fecha = request.getParameter("fecha");
        String horaInicio = request.getParameter("hora_inicio");
        String horaFin = request.getParameter("hora_fin");
        String cantidad = request.getParameter("cantidad");

        try {
            Socket socket = new Socket("localhost", 5000);

            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream());

            String comando = "RESERVAR;" + usuario + ";" + fecha + ";" + horaInicio + ";" + horaFin + ";" + cantidad;

            out.writeUTF(comando);

            String respuesta = in.readUTF();

            socket.close();

            String rol = (String) request.getSession().getAttribute("rol");
            String pagina = "dashboard.html";

            if ("ADMIN".equals(rol)) {
                pagina = "admin.html";
            }

            if (respuesta.contains("creada")) {
                response.sendRedirect(pagina + "?msg=ok");
            } else if (respuesta.contains("hora")) {
                response.sendRedirect(pagina + "?msg=hora");
            } else if (respuesta.contains("cantidad")) {
                response.sendRedirect(pagina + "?msg=cantidad");
            } else {
                response.sendRedirect(pagina + "?msg=error");
            }

        } catch (Exception e) {
            response.sendRedirect("dashboard.html?msg=server");
        }
    }
}