package decryptor;

import dto.Message;
import dto.NetworkMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.ServerSignals;

import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class DecryptorNode implements Runnable{
    private static final Logger logger = LoggerFactory.getLogger(DecryptorNode.class);

    private final LinkedTransferQueue<NetworkMessage<byte[]>> inputQueue;
    private final LinkedTransferQueue<NetworkMessage<Message>> outputQueue;
    private final IDecryptor decryptor;
    private final AtomicInteger activeEncryptorsCounter;

    public DecryptorNode(LinkedTransferQueue<NetworkMessage<byte[]>> inputQueue, LinkedTransferQueue<NetworkMessage<Message>> outputQueue, IDecryptor decryptor, AtomicInteger activeEncryptorsCounter) {
        this.inputQueue = inputQueue;
        this.outputQueue = outputQueue;
        this.decryptor = decryptor;
        this.activeEncryptorsCounter = activeEncryptorsCounter;
    }

    @Override
    public void run() {
        try{
            while(!Thread.currentThread().isInterrupted()){
                NetworkMessage<byte[]> message = inputQueue.take();

                if (message == ServerSignals.POISON_PILL_BYTES) {
                    logger.info("DecryptorNode thread {} stopped", Thread.currentThread().getName());

                    if (activeEncryptorsCounter.decrementAndGet() == 0) {
                        logger.info("The last decryptor has finished its work. Passing the poison pill to the Processors.");
                        outputQueue.put(ServerSignals.POISON_PILL_MSG);
                    }
                    else
                        inputQueue.put(ServerSignals.POISON_PILL_BYTES);

                    break;
                }

                Message payload;
                try {
                    payload = decryptor.decrypt(message.data());
                } catch (Exception e) {
                    logger.warn("Security/Format error! Dropped invalid packet from {}: {}",
                            message.connectionId(), e.getMessage());
                    continue;
                }

                NetworkMessage<Message> decryptedMessage = new NetworkMessage<>(
                        message.connectionId(),
                        payload
                );

                outputQueue.put(decryptedMessage);
            }
        }
        catch (InterruptedException e){
            Thread.currentThread().interrupt();
        }
    }
}
