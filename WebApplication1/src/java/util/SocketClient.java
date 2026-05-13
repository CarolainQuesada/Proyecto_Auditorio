package util;

import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;

/**
 * TCP client for servlets to communicate with the reservation server.
 * 
 * <p>Supports configurable host and retry logic for distributed deployment.
 */
public class SocketClient {

    private static final String HOST = Config.getSocketHost();
    private static final int PORT = Config.getSocketPort();
    
    /** Connection timeout in milliseconds */
    private static final int CONNECT_TIMEOUT = 10000;
    
    /** Socket read timeout in milliseconds */
    private static final int READ_TIMEOUT = 30000;
    
    /** Max retries for transient failures */
    private static final int MAX_RETRIES = 3;
    
    /** Delay between retries in milliseconds */
    private static final int RETRY_DELAY = 2000;

    private SocketClient() {}

    public static String sendCommand(String command) {
    Socket socket = new Socket();
    try {
        // 1. Primero conectar
        socket.connect(new java.net.InetSocketAddress(HOST, PORT), CONNECT_TIMEOUT);
        socket.setSoTimeout(READ_TIMEOUT);

        // 2. Luego abrir los flujos
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        // 3. Enviar y recibir
        out.println(command);
        String response = in.readLine();
        return (response != null) ? response : "error";

    } catch (IOException e) {
        System.err.println(" Socket error: " + e.getMessage());
        return retryOrError(command, MAX_RETRIES, "io");
    } finally {
        // 4. Cerrar siempre el socket
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
    }
}
    
    private static String sendCommandWithRetries(String command, int retriesLeft) {
        try (Socket socket = new Socket();
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(
                     new InputStreamReader(socket.getInputStream()))) {
            
            // Configurar timeouts
            socket.connect(
                new java.net.InetSocketAddress(HOST, PORT), 
                CONNECT_TIMEOUT
            );
            socket.setSoTimeout(READ_TIMEOUT);

            out.println(command);
            String response = in.readLine();
            
            return (response != null) ? response : "error";

        } catch (SocketTimeoutException e) {
            System.err.println("⏱️  Timeout connecting to server: " + e.getMessage());
            return retryOrError(command, retriesLeft, "timeout");
            
        } catch (IOException e) {
            System.err.println("🔌 Socket error: " + e.getMessage());
            return retryOrError(command, retriesLeft, "io");
            
        } catch (Exception e) {
            System.err.println("❌ Unexpected error: " + e.getMessage());
            return "error";
        }
    }
    
    private static String retryOrError(String command, int retriesLeft, String errorType) {
        if (retriesLeft > 1) {
            System.out.println("🔄 Retrying... (" + (MAX_RETRIES - retriesLeft + 1) + "/" + MAX_RETRIES + ")");
            try {
                Thread.sleep(RETRY_DELAY);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
            return sendCommandWithRetries(command, retriesLeft - 1);
        }
        return "error";
    }
    
    /** Utility method to test connection */
    public static boolean isServerAvailable() {
        try (Socket socket = new Socket()) {
            socket.connect(new java.net.InetSocketAddress(HOST, PORT), 5000);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
    
    /** Getter for debugging */
    public static String getServerHost() {
        return HOST;
    }
}