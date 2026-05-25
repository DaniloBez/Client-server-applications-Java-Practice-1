package receiver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.ServerSignals;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ReceiverNode implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(ReceiverNode.class);

    private final BlockingQueue<byte[]> outputQueue;
    private final IReceiver receiver;
    private final AtomicBoolean isRunning = new AtomicBoolean(true);
    private final AtomicInteger activeReceiversCounter;

    public ReceiverNode(BlockingQueue<byte[]> outputQueue, IReceiver receiver, AtomicInteger counter) {
        this.outputQueue = outputQueue;
        this.receiver = receiver;
        this.activeReceiversCounter = counter;
    }

    public void stopNode() {
        isRunning.set(false);
    }

    @Override
    public void run() {
        try {
            while (isRunning.get() && !Thread.currentThread().isInterrupted()) {
                byte[] output = receiver.receiveMessage();
                outputQueue.put(output);
            }
        }
        catch (InterruptedException e) {
            logger.error("ReceiverNode thread {} interrupted: {}", Thread.currentThread().getName(), e.getMessage());
            Thread.currentThread().interrupt();
        }
        finally {
            logger.info("ReceiverNode thread {} stopped", Thread.currentThread().getName());
            if (activeReceiversCounter.decrementAndGet() == 0) {
                try {
                    logger.info("The last receiver has stopped working. Passing the poison pill to the Decryptors");
                    outputQueue.put(ServerSignals.POISON_PILL_BYTES);
                } catch (InterruptedException e) {
                    logger.error("ReceiverNode thread {} interrupted while passing the poison pill: {}",  Thread.currentThread().getName(), e.getMessage());
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}
