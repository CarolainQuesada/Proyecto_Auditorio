package util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Config {
    private static final Properties props = new Properties();

    static {
        try (InputStream input = Config.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input != null) {
                props.load(input);
            }
        } catch (IOException e) {
            System.err.println("config.properties no encontrado, usando valores por defecto");
        }
    }

    public static String getDbUrl() {
        return props.getProperty("db.url");
    }

    public static String getDbUser() {
        return props.getProperty("db.user");
    }

    public static String getDbPassword() {
        return props.getProperty("db.password");
    }

    public static String getSocketHost() {
        return props.getProperty("socket.host", "127.0.0.1");
    }

    public static int getSocketPort() {
        return Integer.parseInt(props.getProperty("socket.port", "5000"));
    }
}
