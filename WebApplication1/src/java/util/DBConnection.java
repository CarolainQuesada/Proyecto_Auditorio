package util;

import java.sql.Connection;
import java.sql.DriverManager;

public class DBConnection {

    private static final String URL = "jdbc:mysql://localhost:3306/auditorio";
    private static final String USER = "root";
    private static final String PASSWORD = ""; // pon tu contraseña si tienes

    public static Connection getConnection() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection con = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("✅ Conectado a MySQL");
            return con;
        } catch (Exception e) {
            System.out.println("❌ Error al conectar");
            e.printStackTrace();
            return null;
        }
    }
}