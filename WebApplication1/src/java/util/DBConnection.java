package util;

import java.sql.Connection;
import java.sql.DriverManager;

public class DBConnection {

    private static final String URL = "jdbc:mysql://mysql-11b2f5ad-db-so.g.aivencloud.com:28915/auditorio?sslMode=REQUIRED";
    private static final String USER = "avnadmin";
    private static final String PASSWORD = "";

    public static Connection getConnection() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection con = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("Connected to MySQL");
            return con;
        } catch (Exception e) {
            System.out.println("Database connection error");
            e.printStackTrace();
            return null;
        }
    }
}
