package server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sender.ISender;

import java.util.concurrent.ConcurrentHashMap;

public class ConnectionManager {
    private static final Logger logger = LoggerFactory.getLogger(ConnectionManager.class);

    public static final String BROADCAST_ID = "BROADCAST";

    private final ConcurrentHashMap<String, ISender> activeConnections = new ConcurrentHashMap<>();

    public void addConnection(String connectionId, ISender sender) {
        activeConnections.put(connectionId, sender);
        logger.info("Added connection with id {}", connectionId);
    }

    public void removeConnection(String connectionId) {
        activeConnections.remove(connectionId);
        logger.info("Removed connection with id {}", connectionId);
    }

    public ISender getSender(String connectionId) {
        return activeConnections.get(connectionId);
    }

    public Iterable<ISender> getAllSenders() {
        return activeConnections.values();
    }

    public void closeAllConnections() {
        logger.info("Closing all active connections...");
        for (ISender sender : activeConnections.values())
            if (sender != null)
                sender.close();

        logger.info("All connections closed.");
    }
}
