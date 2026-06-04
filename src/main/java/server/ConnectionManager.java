package server;

import dto.Message;
import dto.NetworkMessage;
import dto.UnackedMessage;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import processor.ProcessorNode;
import sender.ISender;
import utils.Constants;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class ConnectionManager {
    private static final Logger logger = LoggerFactory.getLogger(ConnectionManager.class);

    private static final long UDP_TIMEOUT_MS = 15000;

    private final ConcurrentHashMap<String, ISender> activeConnections = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, Long> udpActivityTracker = new ConcurrentHashMap<>();
    private final ScheduledExecutorService reaperExecutor;

    private final long TIMEOUT_MS = 100;
    private final int MAX_RETRIES = 5;
    private final ScheduledExecutorService resender;

    @Setter
    private Consumer<String> onClientDisconnected;

    @Setter
    private Consumer<NetworkMessage<Message>> onResendMessage;

    public ConnectionManager() {
        this.reaperExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "UDP-Reaper");
            t.setDaemon(true);
            return t;
        });

        this.reaperExecutor.scheduleAtFixedRate(this::reapDeadClients, 5, 5, TimeUnit.SECONDS);

        this.resender = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "UDP-Resender");
            t.setDaemon(true);
            return t;
        });

        this.resender.scheduleWithFixedDelay(this::resendUnackedMessages, 100, 100, TimeUnit.MILLISECONDS);
    }

    public void updateUdpActivity(String connectionId) {
        if (connectionId.startsWith(Constants.UDP_HEADER))
            udpActivityTracker.put(connectionId, System.currentTimeMillis());
    }

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
        if (reaperExecutor != null && !reaperExecutor.isShutdown())
            reaperExecutor.shutdownNow();

        if (resender != null && !resender.isShutdown())
            resender.shutdownNow();

        for (ISender sender : activeConnections.values())
            if (sender != null)
                sender.close();

        activeConnections.clear();
        udpActivityTracker.clear();
        logger.info("All connections closed.");
    }

    private void reapDeadClients() {
        long currentTime = System.currentTimeMillis();
        for (var entry : udpActivityTracker.entrySet()) {
            if (currentTime - entry.getValue() > UDP_TIMEOUT_MS) {
                String deadConnectionId = entry.getKey();
                logger.info("Reaper: UDP Client {} timed out. Kicking...", deadConnectionId);

                removeConnection(deadConnectionId);

                if (onClientDisconnected != null)
                    onClientDisconnected.accept(deadConnectionId);
            }
        }
    }

    private void resendUnackedMessages() {
        long currentTime = System.currentTimeMillis();

        for (var entry : ProcessorNode.getUnackedMessages().entrySet()) {
            String key = entry.getKey();
            UnackedMessage<NetworkMessage<Message>> message = entry.getValue();

            if (currentTime - message.getLastSendTime() > TIMEOUT_MS) {
                if (message.getRetryCount() < MAX_RETRIES) {
                    message.setRetryCount(message.getRetryCount() + 1);
                    message.setLastSendTime(currentTime);

                    if (onResendMessage != null)
                        onResendMessage.accept(message.getMessage());

                    logger.info("Resending message to {}", message.getMessage().connectionId());
                } else {
                    ProcessorNode.getUnackedMessages().remove(key);
                    logger.debug("Failed to deliver message {} to client after 5 retries", key);
                }
            }
        }
    }

    public Iterable<String> getActiveConnectionIds() {
        return activeConnections.keySet();
    }
}
