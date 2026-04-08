package socket;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import service.ReservationService;
import service.UserService;

public class ClientHandler extends Thread {

    private final Socket socket;
    private String user;
    private final ServerGUI gui;

    public ClientHandler(Socket socket, ServerGUI gui) {
        this.socket = socket;
        this.gui = gui;
    }

    public String getUser() {
        return user;
    }

    @Override
    public void run() {
        try (
            DataInputStream in = new DataInputStream(socket.getInputStream());
            DataOutputStream out = new DataOutputStream(socket.getOutputStream())
        ) {
            String message = in.readUTF();
            String[] p = message.split(";");

            ReservationService reservationService = new ReservationService();
            UserService userService = new UserService();

            String response = "ERROR";

            if (p.length > 0) {
                switch (p[0]) {

                    case "LOGIN":
                        if (p.length >= 3) {
                            response = userService.loginOrRegisterUser(p[1], p[2]);
                            gui.log("Login attempt: " + p[1] + " -> " + response);
                        }
                        break;

                    case "RESERVE":
                        if (p.length >= 6) {
                            user = p[1];

                            gui.log("Client connected: " + user);
                            gui.log("Reservation: " + user + " | " + p[2] + " | " + p[3] + " - " + p[4]);

                            response = reservationService.createReservation(
                                    p[1],
                                    p[2],
                                    p[3],
                                    p[4],
                                    Integer.parseInt(p[5])
                            );
                        }
                        break;

                    case "EDIT":
                        if (p.length >= 6) {
                            response = reservationService.editReservation(
                                    Integer.parseInt(p[1]),
                                    p[2],
                                    p[3],
                                    p[4],
                                    Integer.parseInt(p[5])
                            );
                        }
                        break;

                    case "CONFIRM":
                        if (p.length >= 2) {
                            response = reservationService.confirmReservation(Integer.parseInt(p[1]));
                        }
                        break;

                    case "DELETE":
                        if (p.length >= 2) {
                            response = reservationService.deleteReservation(Integer.parseInt(p[1]));
                        }
                        break;

                    case "LIST":
                        response = reservationService.listReservations();
                        break;

                    default:
                        gui.log("Unknown command: " + message);
                        response = "ERROR";
                        break;
                }
            }

            out.writeUTF(response);
            out.flush();

        } catch (Exception e) {
            gui.log("Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (Exception e) {
                gui.log("Error closing socket: " + e.getMessage());
            }

            if (user != null) {
                gui.log("Client disconnected: " + user);
            }
        }
    }
}