package sender;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.ServerSignals;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.BlockingQueue;

public class SenderNode implements Runnable{
    private static final Logger logger = LoggerFactory.getLogger(SenderNode.class);

    private final BlockingQueue<byte[]> inputQueue;
    private final ISender sender;

    public SenderNode(BlockingQueue<byte[]> inputQueue, ISender sender) {
        this.inputQueue = inputQueue;
        this.sender = sender;
    }

    @Override
    public void run() {
        try {
            InetAddress localTarget = InetAddress.getLocalHost();

            while (!Thread.currentThread().isInterrupted()) {
                byte[] dataToSend = inputQueue.take();

                if (dataToSend == ServerSignals.POISON_PILL_BYTES) {
                    inputQueue.put(ServerSignals.POISON_PILL_BYTES);
                    logger.info("SenderNode thread {} stopped", Thread.currentThread().getName());
                    break;
                }

                sender.send(dataToSend, localTarget);
            }

        } catch (InterruptedException e) {
            logger.error("SenderNode thread {} interrupted: {}", Thread.currentThread().getName(), e.getMessage());
            Thread.currentThread().interrupt();
        } catch (UnknownHostException e) {
            logger.error("Error creating a local IP address: {}", e.getMessage());
        }
    }
}
