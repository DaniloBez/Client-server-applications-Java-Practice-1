package processor;

import dto.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.ServerSignals;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class ProcessorNode implements Runnable {
    private final static Logger logger = LoggerFactory.getLogger(ProcessorNode.class);

    private final BlockingQueue<Message> inputQueue;
    private final BlockingQueue<Message> outputQueue;
    private final IProcessor processor;
    private final AtomicInteger activeProcessorsCounter;

    public ProcessorNode(BlockingQueue<Message> inputQueue, BlockingQueue<Message> outputQueue, IProcessor processor,  AtomicInteger activeProcessorsCounter) {
        this.inputQueue = inputQueue;
        this.outputQueue = outputQueue;
        this.processor = processor;
        this.activeProcessorsCounter = activeProcessorsCounter;
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                Message inputMessage = inputQueue.take();

                if (inputMessage == ServerSignals.POISON_PILL_MSG) {
                    logger.info("ProcessorNode thread {} stopped", Thread.currentThread().getName());

                    if (activeProcessorsCounter.decrementAndGet() == 0) {
                        logger.info("The last processor has finished its work. Passing the poison pill to the Encryptors.");
                        outputQueue.put(ServerSignals.POISON_PILL_MSG);
                    }
                    else
                        inputQueue.put(ServerSignals.POISON_PILL_MSG);

                    break;
                }

                Message outputMessage = processor.process(inputMessage);
                outputQueue.put(outputMessage);
            }
        }
        catch (InterruptedException e) {
            logger.info("ProcessorNode thread {} interrupted: {}", Thread.currentThread().getName(), e.getMessage());
            Thread.currentThread().interrupt();
        }
    }
}
