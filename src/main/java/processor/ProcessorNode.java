package processor;

import dto.Message;
import dto.NetworkMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.ServerSignals;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class ProcessorNode implements Runnable {
    private final static Logger logger = LoggerFactory.getLogger(ProcessorNode.class);

    private final BlockingQueue<NetworkMessage<Message>> inputQueue;
    private final BlockingQueue<NetworkMessage<Message>> outputQueue;
    private final IProcessor processor;
    private final AtomicInteger activeProcessorsCounter;

    public ProcessorNode(BlockingQueue<NetworkMessage<Message>> inputQueue, BlockingQueue<NetworkMessage<Message>> outputQueue, IProcessor processor,  AtomicInteger activeProcessorsCounter) {
        this.inputQueue = inputQueue;
        this.outputQueue = outputQueue;
        this.processor = processor;
        this.activeProcessorsCounter = activeProcessorsCounter;
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                NetworkMessage<Message> inputMessage = inputQueue.take();

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

                Message message = processor.process(inputMessage.data());

                NetworkMessage<Message> outputMessage;
                if (message.getUserId() == Processor.BROADCAST_USER_ID)
                    outputMessage = new NetworkMessage<>("BROADCAST", message);
                else
                    outputMessage = new NetworkMessage<>(inputMessage.connectionId(), message);

                outputQueue.put(outputMessage);
            }
        }
        catch (InterruptedException e) {
            logger.info("ProcessorNode thread {} interrupted: {}", Thread.currentThread().getName(), e.getMessage());
            Thread.currentThread().interrupt();
        }
    }
}
