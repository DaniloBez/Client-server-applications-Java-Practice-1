package sender;

import dto.NetworkMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.ServerSignals;

import java.util.concurrent.BlockingQueue;

import server.ConnectionManager;

public class SenderNode implements Runnable{
    private static final Logger logger = LoggerFactory.getLogger(SenderNode.class);

    private final BlockingQueue<NetworkMessage<byte[]>> inputQueue;
    private final ConnectionManager connectionManager;

    public SenderNode(BlockingQueue<NetworkMessage<byte[]>> inputQueue, ConnectionManager connectionManager) {
        this.inputQueue = inputQueue;
        this.connectionManager = connectionManager;
    }

    @Override
    public void run() {
        try {

            while (!Thread.currentThread().isInterrupted()) {
                NetworkMessage<byte[]> message = inputQueue.take();

                if (message == ServerSignals.POISON_PILL_BYTES) {
                    inputQueue.put(ServerSignals.POISON_PILL_BYTES);
                    logger.info("SenderNode thread {} stopped", Thread.currentThread().getName());
                    break;
                }

                if (ConnectionManager.BROADCAST_ID.equals(message.connectionId()))
                    for(ISender sender : connectionManager.getAllSenders())
                        if (sender != null)
                            sender.send(message.data());
                        else
                            logger.info("Target connection not found while sending broadcast: {}", message.connectionId());
                else {
                    ISender sender = connectionManager.getSender(message.connectionId());
                    if (sender != null)
                        sender.send(message.data());
                    else
                            logger.info("Target connection not found: {}", message.connectionId());
                }
            }

        } catch (InterruptedException e) {
            logger.error("SenderNode thread {} interrupted: {}", Thread.currentThread().getName(), e.getMessage());
            Thread.currentThread().interrupt();
        }
    }
}
