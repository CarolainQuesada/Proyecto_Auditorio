package concurrency;

import dao.ReservationDAO;
import socket.ServerGUI;

public class TTLMonitor extends Thread {

    private final ServerGUI gui;
    private static final int TTL_MINUTES = 10;

    public TTLMonitor(ServerGUI gui) {
        this.gui = gui;
    }

    @Override
    public void run() {

        while (true) {
            try {
                gui.log("TTL running... Expiración configurada: " + TTL_MINUTES + " minutos");

                int expired = new ReservationDAO().cleanExpired(TTL_MINUTES);

                if (expired > 0) {
                    gui.log("TTL expired reservations: " + expired);

                    SystemLog.getInstance().log(
                            "SYSTEM",
                            "TTL_EXPIRED",
                            "Reservas expiradas automáticamente: " + expired
                    );
                }

                Thread.sleep(60000);

            } catch (InterruptedException e) {
                gui.log("TTL detenido");
                Thread.currentThread().interrupt();
                break;

            } catch (Exception e) {
                gui.log("TTL error: " + e.getMessage());
            }
        }
    }
}