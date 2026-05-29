package processor;

import dto.Message;
import dto.NetworkMessage;
import dto.UnackedMessage;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.Constants;
import utils.ServerSignals;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class ProcessorNode implements Runnable {
    private final static Logger logger = LoggerFactory.getLogger(ProcessorNode.class);

    private final LinkedTransferQueue<NetworkMessage<Message>> inputQueue;
    private final LinkedTransferQueue<NetworkMessage<Message>> outputQueue;
    private final IProcessor processor;
    private final AtomicInteger activeProcessorsCounter;

    private static final Map<String, Set<Long>> idempotencyCache = new ConcurrentHashMap<>();

    @Getter
    private static final Map<String, UnackedMessage<NetworkMessage<Message>>> unackedMessages = new ConcurrentHashMap<>();

    public ProcessorNode(LinkedTransferQueue<NetworkMessage<Message>> inputQueue, LinkedTransferQueue<NetworkMessage<Message>> outputQueue, IProcessor processor, AtomicInteger activeProcessorsCounter) {
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

                        idempotencyCache.clear();

                        outputQueue.put(ServerSignals.POISON_PILL_MSG);
                    } else
                        inputQueue.put(ServerSignals.POISON_PILL_MSG);

                    break;
                }

                if (inputMessage.data() == ServerSignals.DISCONNECT_PILL_MSG) {
                    idempotencyCache.remove(inputMessage.connectionId());
                    logger.info("Processor cleaned up cache for dead client: {}", inputMessage.connectionId());
                    continue;
                }

                boolean isUDPClient = inputMessage.connectionId().startsWith(Constants.UDP_HEADER);

                if (inputMessage.data().getCommandId() == Constants.ACK_COMMAND_ID) {
                    String key = inputMessage.connectionId() + ":" + inputMessage.data().getMessageId();
                    unackedMessages.remove(key);
                    continue;
                }

                if (isUDPClient && isDuplicate(
                        inputMessage.connectionId(),
                        inputMessage.data().getMessageId()
                )) {
                    sendAck(inputMessage);
                    continue;
                }

                Message message = processor.process(inputMessage.data());

                NetworkMessage<Message> outputMessage;
                if (message.getUserId() == Processor.BROADCAST_USER_ID)
                    outputMessage = new NetworkMessage<>("BROADCAST", message);
                else {
                    outputMessage = new NetworkMessage<>(inputMessage.connectionId(), message);

                    if (isUDPClient) {
                        String key = inputMessage.connectionId() + ":" + message.getMessageId();
                        unackedMessages.put(key, new UnackedMessage<>(outputMessage));
                    }
                }

                outputQueue.put(outputMessage);

                if (isUDPClient) {
                    sendAck(inputMessage);
                }
            }
        } catch (InterruptedException e) {
            logger.info("ProcessorNode thread {} interrupted: {}", Thread.currentThread().getName(), e.getMessage());
            Thread.currentThread().interrupt();
        }
    }

    private boolean isDuplicate(String clientId, long messageId) {
        Set<Long> userCache = idempotencyCache.computeIfAbsent(clientId, _ ->
                Collections.synchronizedSet(
                        Collections.newSetFromMap(
                                new LinkedHashMap<>(100, 0.75f, true) {
                                    @Override
                                    protected boolean removeEldestEntry(Map.Entry<Long, Boolean> eldest) {
                                        return size() > 100;
                                    }
                                }
                        )
                )
        );

        boolean isNewMessage = userCache.add(messageId);
        return !isNewMessage;
    }

    private void sendAck(NetworkMessage<Message> originalMessage) {
        NetworkMessage<Message> ackMessage = new NetworkMessage<>(
                originalMessage.connectionId(),
                new Message(
                        originalMessage.data().getClientApplicationId(),
                        originalMessage.data().getMessageId(),
                        Constants.ACK_COMMAND_ID,
                        originalMessage.data().getUserId(),
                        "{}"
                )
        );

        outputQueue.put(ackMessage);
    }
}
