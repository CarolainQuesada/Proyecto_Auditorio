package util;

import java.io.*;
import java.net.Socket;

public class SocketClient {
    private static final String HOST = "localhost";
    private static final int PORT = 5000;

    public static String sendCommand(String command) {
        String response = "error";
        try (Socket socket = new Socket(HOST, PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            
            out.println(command);
            String serverResponse = in.readLine();
            if (serverResponse != null) {
                response = serverResponse;
            }
        } catch (IOException e) {
            System.err.println("Error Socket: " + e.getMessage());
        }
        return response;
    }
}