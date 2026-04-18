package concurrency;

/**
 * Bitácora mínima - Solo para que compile ClientHandler.
 * Thread-safe básico con synchronized.
 */
public class SystemLog {
    
    /**
     * Registra evento (mínimo: imprime en consola)
     */
    public synchronized void log(String action, String details) {
        // Mínimo: imprimir en consola del servidor
        System.out.println("[LOG] " + action + ": " + details);
    }
}
