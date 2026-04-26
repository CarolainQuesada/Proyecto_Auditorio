package util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Utility class that provides JDBC {@link Connection} objects to the MySQL
 * database hosted on Aiven Cloud.
 *
 * <p>Every call to {@link #getConnection()} opens a <em>new</em> physical
 * connection. Callers are responsible for closing the connection (preferably
 * with a try-with-resources block) to avoid connection leaks.
 *
 * <p><b>Security note:</b> Credentials are currently hard-coded as class
 * constants. In a production environment they should be externalised to
 * environment variables or a secrets manager, and the constants replaced with
 * {@code System.getenv()} calls.
 *
 * <p>Usage example:
 * <pre>{@code
 * try (Connection con = DBConnection.getConnection();
 *      PreparedStatement ps = con.prepareStatement("SELECT ...")) {
 *     // use ps …
 * }
 * }</pre>
 *
 * @see java.sql.DriverManager
 */
public class DBConnection {

    /**
     * JDBC connection URL pointing to the remote MySQL instance.
     * SSL is required ({@code sslMode=REQUIRED}).
     */
    private static final String URL =
            "jdbc:mysql://mysql-11b2f5ad-db-so.g.aivencloud.com:28915/auditorio?sslMode=REQUIRED";

    /** Database username. */
    private static final String USER = "avnadmin";

    /** Database password (plain text — see security note in class Javadoc). */
    private static final String PASSWORD = "AVNS_BbW0UnDbv72t2jXued-";

    /**
     * Private constructor — this class is a static utility and should not be
     * instantiated.
     */
    private DBConnection() {}

    /**
     * Opens and returns a new JDBC connection to the configured MySQL database.
     *
     * <p>The MySQL Connector/J driver ({@code com.mysql.cj.jdbc.Driver}) is
     * loaded via reflection on each call. While this has negligible overhead
     * for modern JVMs (class loading is cached), it ensures the driver is
     * always registered before {@link DriverManager#getConnection} is invoked.
     *
     * @return a new, open {@link Connection}; never {@code null}
     * @throws SQLException if the driver class cannot be found, or if the
     *                      database refuses the connection (wrong credentials,
     *                      network issue, etc.)
     */
    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            if (URL == null || USER == null || PASSWORD == null) {
                throw new SQLException(
                        "Missing DB_URL, DB_USER, or DB_PASSWORD configuration");
            }

            return DriverManager.getConnection(URL, USER, PASSWORD);

        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL driver not found on the classpath", e);
        }
    }
}