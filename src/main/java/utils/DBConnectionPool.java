package utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TransferQueue;

public class DBConnectionPool {
    private static final Logger logger = LoggerFactory.getLogger(DBConnectionPool.class);

    private final TransferQueue<Connection> pool;

    public DBConnectionPool(int poolSize, String dbUrl, String username, String password) {
        pool = new LinkedTransferQueue<>();

        try {
            for (int i = 0; i < poolSize; i++)
                pool.add(DriverManager.getConnection(dbUrl, username, password));

            logger.info("Connection pool initialized with {} connections.", poolSize);
        } catch (SQLException e) {
            logger.error("Failed to initialize connection pool", e);
        }
    }

    public Connection getConnection() throws InterruptedException {
        return pool.take();
    }

    public void releaseConnection(Connection connection) {
        if (connection != null)
            pool.offer(connection);
    }

    public void closeAll() {
        for (Connection conn : pool) {
            try { conn.close(); } catch (SQLException ignored) {}
        }
    }
}
