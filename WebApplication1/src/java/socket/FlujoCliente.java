package socket;

import java.io.*;
import java.net.*;
import service.ReservaService;

public class FlujoCliente extends Thread {

    private Socket socket;
    private String usuario;
    private ServidorGUI gui;

    public FlujoCliente(Socket socket, ServidorGUI gui) {
        this.socket = socket;
        this.gui = gui;
    }

    public String getUsuario() {
        return usuario;
    }

    @Override
public void run() {

    try {
        DataInputStream in = new DataInputStream(socket.getInputStream());
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());

        String mensaje = in.readUTF();
        String[] p = mensaje.split(";");

        ReservaService service = new ReservaService();

        String respuesta = "ERROR";

        switch (p[0]) {

            case "RESERVAR":

                usuario = p[1];

                gui.log("👤 Cliente conectado: " + usuario);

                gui.log("📅 Reserva: " + usuario +
                        " | " + p[2] +
                        " | " + p[3] + " - " + p[4]);

                respuesta = service.crearReserva(
                        p[2],
                        p[3],
                        p[4],
                        Integer.parseInt(p[5])
                );

                break;

            case "CONFIRMAR":
                respuesta = service.confirmarReserva(Integer.parseInt(p[1]));
                break;

            case "ELIMINAR":
                respuesta = service.eliminarReserva(Integer.parseInt(p[1]));
                break;

            case "LISTAR":
                respuesta = service.listarReservas();
                break;
        }

        out.writeUTF(respuesta);
        socket.close();

    } catch (Exception e) {
        gui.log("❌ Error: " + e.getMessage());
    } finally {
        if (usuario != null) {
            gui.log("❌ Cliente desconectado: " + usuario);
        }
    }
}
}
