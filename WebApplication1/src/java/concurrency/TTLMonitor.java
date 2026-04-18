package concurrency;

import dao.ReservationDAO;
import socket.ServerGUI;

public class TTLMonitor extends Thread {

    private ServerGUI gui;
    private static final int TTL_MINUTES = 10;

    public TTLMonitor(ServerGUI gui) {
        this.gui = gui;
    }

    @Override
    public void run() {

        while (true) {
            try {

                gui.log("TTL running...");

                new ReservationDAO().cleanExpired(TTL_MINUTES);

                Thread.sleep(60000);

            } catch (Exception e) {
                gui.log("TTL error: " + e.getMessage());
            }
        }
    }
}