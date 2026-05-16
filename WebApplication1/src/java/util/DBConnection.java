package util;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayDeque;
import java.util.Queue;

public class DBConnection {

    private static final String URL_BASE = Config.getDbUrl();
    private static final String USER = Config.getDbUser();
    private static final String PASSWORD = Config.getDbPassword();

    private static final String URL = URL_BASE
            + "&connectTimeout=10000"
            + "&socketTimeout=30000"
            + "&autoReconnect=true"
            + "&failOverReadOnly=false";

    private static final int MAX_RETRIES = 2;
    private static final int RETRY_DELAY_MS = 500;
    private static final int MAX_POOL_SIZE = 8;
    private static final Queue<Connection> POOL = new ArrayDeque<>();

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("MySQL driver not found: " + e.getMessage());
        }
    }

    private DBConnection() {}

    public static Connection getConnection() throws SQLException {
        Connection pooled = borrowFromPool();
        if (pooled != null) {
            return wrap(pooled);
        }

        int attempts = 0;
        SQLException lastError = null;

        while (attempts < MAX_RETRIES) {
            try {
                Connection con = DriverManager.getConnection(URL, USER, PASSWORD);
                if (attempts > 0) {
                    System.out.println("DB connection restored after " + attempts + " attempts");
                }
                return wrap(con);
            } catch (SQLException e) {
                lastError = e;
                attempts++;

                String msg = e.getMessage().toLowerCase();
                if (msg.contains("access denied")
                        || msg.contains("unknown database")
                        || msg.contains("configuration")) {
                    throw e;
                }

                System.err.println("DB connection error (attempt " + attempts + "/" + MAX_RETRIES + "): " + e.getMessage());

                if (attempts >= MAX_RETRIES) {
                    System.err.println("Max DB connection retries reached");
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

    private static synchronized Connection borrowFromPool() {
        while (!POOL.isEmpty()) {
            Connection con = POOL.poll();
            try {
                if (con != null && !con.isClosed() && con.isValid(2)) {
                    return con;
                }
            } catch (SQLException ignored) {
                closePhysical(con);
            }
        }
        return null;
    }

    private static synchronized void returnToPool(Connection con) throws SQLException {
        if (con == null || con.isClosed()) {
            return;
        }

        if (!con.getAutoCommit()) {
            con.rollback();
            con.setAutoCommit(true);
        }
        con.clearWarnings();

        if (POOL.size() >= MAX_POOL_SIZE || !con.isValid(2)) {
            con.close();
            return;
        }

        POOL.offer(con);
    }

    private static Connection wrap(Connection con) {
        InvocationHandler handler = new PooledConnectionHandler(con);
        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                handler
        );
    }

    private static void closePhysical(Connection con) {
        try {
            if (con != null) {
                con.close();
            }
        } catch (SQLException ignored) {
        }
    }

    private static class PooledConnectionHandler implements InvocationHandler {
        private final Connection delegate;
        private boolean closed;

        PooledConnectionHandler(Connection delegate) {
            this.delegate = delegate;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String name = method.getName();

            if ("close".equals(name)) {
                if (!closed) {
                    closed = true;
                    returnToPool(delegate);
                }
                return null;
            }

            if ("isClosed".equals(name)) {
                return closed || delegate.isClosed();
            }

            if ("unwrap".equals(name)) {
                Class<?> target = (Class<?>) args[0];
                if (target.isInstance(delegate)) {
                    return delegate;
                }
            }

            if ("isWrapperFor".equals(name)) {
                Class<?> target = (Class<?>) args[0];
                return target.isInstance(delegate);
            }

            if (closed) {
                throw new SQLException("Connection is closed");
            }

            try {
                return method.invoke(delegate, args);
            } catch (InvocationTargetException e) {
                throw e.getTargetException();
            }
        }
    }

    public static boolean testConnection() {
        try (Connection con = getConnection()) {
            return con != null && !con.isClosed();
        } catch (SQLException e) {
            System.err.println("Connection test failed: " + e.getMessage());
            return false;
        }
    }

    public static String getConnectionInfo() {
        return "URL: " + URL_BASE + "... | User: " + USER;
    }
}
