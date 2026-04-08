package Controller;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.annotation.WebServlet;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        String email = req.getParameter("email");
        String password = req.getParameter("password");

        if (email == null || email.trim().isEmpty() || !email.trim().endsWith("@una.ac.cr")) {
            resp.sendRedirect("index.html?error=email");
            return;
        }

        if (password == null || password.trim().isEmpty()) {
            resp.sendRedirect("index.html?error=login");
            return;
        }

        try {
            Socket socket = new Socket("localhost", 5000);

            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream());

            out.writeUTF("LOGIN;" + email.trim() + ";" + password.trim());
            out.flush();

            String serverResponse = in.readUTF().trim();

            socket.close();

            if ("ERROR".equalsIgnoreCase(serverResponse)) {
                resp.sendRedirect("index.html?error=login");
                return;
            }

            HttpSession session = req.getSession(true);
            session.setAttribute("user", email.trim());
            session.setAttribute("role", serverResponse.toUpperCase());

            if ("ADMIN".equalsIgnoreCase(serverResponse)) {
                resp.sendRedirect("admin.html");
            } else {
                resp.sendRedirect("dashboard.html");
            }

        } catch (Exception e) {
            e.printStackTrace();
            resp.sendRedirect("index.html?error=server");
        }
    }
}