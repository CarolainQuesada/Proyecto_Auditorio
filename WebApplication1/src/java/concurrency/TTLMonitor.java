package concurrency;

import service.ReservaDAO;
import socket.ServidorGUI;

public class TTLMonitor extends Thread {

    private ServidorGUI gui;
    private static final int TTL_MINUTOS = 10;

    public TTLMonitor(ServidorGUI gui) {
        this.gui = gui;
    }

    @Override
    public void run() {

        while (true) {
            try {

                gui.log("🕒 TTL ejecutándose...");

                new ReservaDAO().limpiarExpiradas(TTL_MINUTOS);

                Thread.sleep(60000);

            } catch (Exception e) {
                gui.log("❌ TTL error: " + e.getMessage());
            }
        }
    }
}