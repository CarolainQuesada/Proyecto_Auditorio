package util;

import java.io.*;
import java.net.Socket;

/**
 * Lightweight TCP client used by web-layer servlets to communicate with the
 * {@link socket.ServerGUI} reservation server running on localhost.
 *
 * <p>The server listens on port {@value #PORT}. Each call to
 * {@link #sendCommand(String)} opens a fresh socket connection, sends a
 * single text command, reads one line of response, and then closes the
 * socket. This connection-per-request model is simple and stateless, but
 * carries per-call overhead; it is suitable for the low-to-medium throughput
 * expected in an academic auditorium management system.
 *
 * <p>All methods are {@code static} because no per-instance state is
 * maintained.
 *
 * <p>Command protocol — commands are semicolon-delimited strings, for
 * example:
 * <pre>
 *   RESERVE;user@una.ac.cr;2026-05-01;08:00;10:00;80;1,2;1,3
 *   CONFIRM;42
 *   DELETE;42
 *   LIST
 * </pre>
 *
 * @see socket.ClientHandler
 */
public class SocketClient {

    /** Hostname of the reservation server. */
    private static final String HOST = "localhost";

    /** TCP port on which the reservation server accepts connections. */
    private static final int PORT = 5000;

    /**
     * Private constructor — this class is a static utility and should not be
     * instantiated.
     */
    private SocketClient() {}

    /**
     * Sends a text command to the reservation server and returns its
     * single-line response.
     *
     * <p>The method opens a new {@link Socket}, writes {@code command}
     * followed by a newline, reads the first line of the server's reply, and
     * closes the socket — all within a single try-with-resources block.
     *
     * <p>If an {@link IOException} occurs (e.g., the server is down), the
     * error is logged to {@code stderr} and the string {@code "error"} is
     * returned so callers can handle it uniformly.
     *
     * @param command the protocol command to send (must not be {@code null}
     *                or empty)
     * @return the server's response line, or {@code "error"} if an I/O
     *         error occurred or the server returned no data
     */
    public static String sendCommand(String command) {
        String response = "error";
        try (Socket socket = new Socket(HOST, PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(
                     new InputStreamReader(socket.getInputStream()))) {

            out.println(command);

            String serverResponse = in.readLine();
            if (serverResponse != null) {
                response = serverResponse;
            }

        } catch (IOException e) {
            System.err.println("Socket error: " + e.getMessage());
        }
        return response;
    }
}