package util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {

    private static final String URL_BASE = Config.getDbUrl();
    private static final String USER = Config.getDbUser();
    private static final String PASSWORD = Config.getDbPassword();
    
    private static final String URL = URL_BASE + 
        "&connectTimeout=10000" +
        "&socketTimeout=30000" +
        "&autoReconnect=true" +
        "&failOverReadOnly=false";
    
    // Retry configuration
    private static final int MAX_RETRIES = 5;
    private static final int RETRY_DELAY_MS = 2000;

    private DBConnection() {}

    public static Connection getConnection() throws SQLException {
        int attempts = 0;
        SQLException lastError = null;
        
        while (attempts < MAX_RETRIES) {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                
                Connection con = DriverManager.getConnection(URL, USER, PASSWORD);
                
                if (attempts > 0) {
                    System.out.println("✅ DB connection restored after " + attempts + " attempts");
                }
                return con;
                
            } catch (ClassNotFoundException e) {
                throw new SQLException("MySQL driver not found", e);
                
            } catch (SQLException e) {
                lastError = e;
                attempts++;
                
                String msg = e.getMessage().toLowerCase();
                if (msg.contains("access denied") || 
                    msg.contains("unknown database") ||
                    msg.contains("configuration")) {
                    throw e;
                }
                
                System.err.println("⚠️  DB connection error (attempt " + attempts + "/" + MAX_RETRIES + "): " + e.getMessage());
                
                if (attempts >= MAX_RETRIES) {
                    System.err.println("❌ Max DB connection retries reached");
                    throw e;
                }
                
                try {
                    Thread.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new SQLException("Connection interrupted", e);
                }
            }
        }
        
        throw lastError != null ? lastError : new SQLException("Unknown connection error");
    }
    
    public static boolean testConnection() {
        try (Connection con = getConnection()) {
            return con != null && !con.isClosed();
        } catch (SQLException e) {
            System.err.println("❌ Connection test failed: " + e.getMessage());
            return false;
        }
    }
    
    public static String getConnectionInfo() {
        return "URL: " + URL_BASE + "... | User: " + USER;
    }
}