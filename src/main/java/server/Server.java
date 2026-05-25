package server;

import decryptor.DecryptorNode;
import decryptor.IDecryptor;
import dto.Message;
import encryptor.EncryptorNode;
import encryptor.IEncryptor;
import encryptor.MessageEncryptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import processor.IProcessor;
import processor.ProcessorNode;
import receiver.IReceiver;
import receiver.ReceiverNode;
import sender.ISender;
import sender.SenderNode;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class Server {
    private static final Logger logger = LoggerFactory.getLogger(Server.class);

    private final BlockingQueue<byte[]> rawInputQueue = new LinkedBlockingQueue<>();
    private final BlockingQueue<Message> decodedQueue = new LinkedBlockingQueue<>();
    private final BlockingQueue<Message> responseQueue = new LinkedBlockingQueue<>();
    private final BlockingQueue<byte[]> rawOutputQueue = new LinkedBlockingQueue<>();

    private final ExecutorService executorService;
    private final List<ReceiverNode> receiverNodes = new ArrayList<>();

    private final IProcessor processor;
    private final IDecryptor decryptor;
    private final IEncryptor encryptor;
    private final ISender sender;
    private final IReceiver receiver;

    private final int receiverCount;
    private final int senderCount;
    private final int decryptorCount;
    private final int encryptorCount;
    private final int processorCount;

    public Server(
            IReceiver receiver,
            int receiverCount,
            ISender sender,
            int senderCount,
            IDecryptor decryptor,
            int decryptorCount,
            MessageEncryptor encryptor,
            int encryptorCount,
            IProcessor processor,
            int processorCount
    ) {
        this.receiver = receiver;
        this.receiverCount = receiverCount;
        this.sender = sender;
        this.senderCount = senderCount;
        this.decryptor = decryptor;
        this.decryptorCount = decryptorCount;
        this.encryptor = encryptor;
        this.encryptorCount = encryptorCount;
        this.processor = processor;
        this.processorCount = processorCount;

        this.executorService = Executors.newFixedThreadPool(receiverCount + senderCount + decryptorCount + encryptorCount + processorCount);
    }

    public void start() {
        logger.info("Launching threads (Scale up)...");

        AtomicInteger activeReceivers = new AtomicInteger(receiverCount);
        for (int i = 0; i < receiverCount; i++) {
            ReceiverNode receiverNode = new ReceiverNode(rawInputQueue, receiver, activeReceivers);
            receiverNodes.add(receiverNode);
            executorService.submit(receiverNode);
        }

        AtomicInteger activeDecryptors = new AtomicInteger(decryptorCount);
        for (int i = 0; i < 2; i++)
            executorService.submit(new DecryptorNode(rawInputQueue, decodedQueue, decryptor, activeDecryptors));

        AtomicInteger activeProcessors = new AtomicInteger(processorCount);
        for (int i = 0; i < 4; i++)
            executorService.submit(new ProcessorNode(decodedQueue, responseQueue, processor, activeProcessors));

        AtomicInteger activeEncryptors = new AtomicInteger(encryptorCount);
        for (int i = 0; i < encryptorCount; i++)
            executorService.submit(new EncryptorNode(responseQueue, rawOutputQueue, encryptor, activeEncryptors));

        for (int i = 0; i < senderCount; i++)
            executorService.submit(new SenderNode(rawOutputQueue, sender));

        logger.info("The server has started successfully and is ready to go!");
    }

    public void stop() {
        logger.info("A shutdown signal has been received. Initiating a graceful shutdown...");

        for (ReceiverNode node : receiverNodes)
            node.stopNode();

        executorService.shutdown();

        logger.info("The server is shutting down. We are waiting for the remaining items in the queues to be processed...");

        try {
            if (!executorService.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS)) {
                logger.error("Some threads didn't have time to stop! We're performing a forced shutdown.");
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            logger.error("Unexpected interrupted while waiting for threads to shut down: {}", e.getMessage());
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
