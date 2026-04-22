package Controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/listReservations")
public class ListReservationsServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        resp.setContentType("text/plain;charset=UTF-8");

        try (
            Socket socket = new Socket("localhost", 5000);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()))
        ) {
            out.println("LIST");
            String responseServer = in.readLine();

            if (responseServer == null) {
                responseServer = "";
            }

            resp.getWriter().write(responseServer);

        } catch (Exception e) {
            e.printStackTrace();
            resp.getWriter().write("");
        }
    }
}