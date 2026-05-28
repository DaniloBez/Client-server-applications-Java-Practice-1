package server;

import decryptor.DecryptorNode;
import decryptor.IDecryptor;
import dto.Message;
import dto.NetworkMessage;
import encryptor.EncryptorNode;
import encryptor.IEncryptor;
import encryptor.MessageEncryptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import processor.IProcessor;
import processor.ProcessorNode;
import sender.SenderNode;
import utils.ServerSignals;


import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class Server {
    private static final Logger logger = LoggerFactory.getLogger(Server.class);

    private final BlockingQueue<NetworkMessage<byte[]>> rawInputQueue = new LinkedBlockingQueue<>();
    private final BlockingQueue<NetworkMessage<Message>> decodedQueue = new LinkedBlockingQueue<>();
    private final BlockingQueue<NetworkMessage<Message>> responseQueue = new LinkedBlockingQueue<>();
    private final BlockingQueue<NetworkMessage<byte[]>> rawOutputQueue = new LinkedBlockingQueue<>();

    private final ExecutorService executorService;
    private final ConnectionManager connectionManager = new ConnectionManager();

    private final int port;

    private AtomicBoolean isTCPServerRun = new AtomicBoolean(false);
    private StoreServerTCP tcpServer;

    private AtomicBoolean isUDPServerRun = new AtomicBoolean(false);
    private StoreServerUDP udpServer;

    private final IProcessor processor;
    private final IDecryptor decryptor;
    private final IEncryptor encryptor;

    private final int senderCount;
    private final int decryptorCount;
    private final int encryptorCount;
    private final int processorCount;

    public Server(
            int senderCount,
            IDecryptor decryptor,
            int decryptorCount,
            MessageEncryptor encryptor,
            int encryptorCount,
            IProcessor processor,
            int processorCount,
            int port
    ) {
        validate(senderCount, decryptorCount, encryptorCount, processorCount, port);

        this.senderCount = senderCount;
        this.decryptor = decryptor;
        this.decryptorCount = decryptorCount;
        this.encryptor = encryptor;
        this.encryptorCount = encryptorCount;
        this.processor = processor;
        this.processorCount = processorCount;

        this.port = port;

        this.executorService = Executors.newFixedThreadPool(2 + senderCount + decryptorCount + encryptorCount + processorCount);
    }

    private void validate(
            int senderCount,
            int decryptorCount,
            int encryptorCount,
            int processorCount,
            int port
    ) throws IllegalArgumentException {
        if (senderCount <= 0)
            throw new IllegalArgumentException("Sender count must be greater than 0");
        if (decryptorCount <= 0)
            throw new IllegalArgumentException("Decryptor count must be greater than 0");
        if (processorCount <= 0)
            throw new IllegalArgumentException("Processor count must be greater than 0");
        if (encryptorCount <= 0)
            throw new IllegalArgumentException("TCP port must be greater than 0");

        if (port <= 1000)
            throw new IllegalArgumentException("TCP port must be greater than 1000");
    }

    public void start() {
        logger.info("Starting TCP Server");
        this.isTCPServerRun = new AtomicBoolean(true);
        this.tcpServer = new StoreServerTCP(port, connectionManager, isTCPServerRun, rawInputQueue);
        executorService.execute(tcpServer);

        logger.info("Starting UDP Server");
        this.isUDPServerRun = new AtomicBoolean(true);
        this.udpServer = new StoreServerUDP(port, connectionManager, isUDPServerRun, rawInputQueue);
        executorService.execute(udpServer);

        logger.info("Launching threads (Scale up)...");

        AtomicInteger activeDecryptors = new AtomicInteger(decryptorCount);
        for (int i = 0; i < decryptorCount; i++)
            executorService.submit(new DecryptorNode(rawInputQueue, decodedQueue, decryptor, activeDecryptors));

        AtomicInteger activeProcessors = new AtomicInteger(processorCount);
        for (int i = 0; i < processorCount; i++)
            executorService.submit(new ProcessorNode(decodedQueue, responseQueue, processor, activeProcessors));

        AtomicInteger activeEncryptors = new AtomicInteger(encryptorCount);
        for (int i = 0; i < encryptorCount; i++)
            executorService.submit(new EncryptorNode(responseQueue, rawOutputQueue, encryptor, activeEncryptors));

        for (int i = 0; i < senderCount; i++)
            executorService.submit(new SenderNode(rawOutputQueue, connectionManager));

        logger.info("The server has started successfully and is ready to go!");
    }

    public void stop() {
        logger.info("A shutdown signal has been received. Initiating a graceful shutdown...");

        logger.info("Shutting down TCP Server");
        isTCPServerRun.set(false);
        if (tcpServer != null)
            tcpServer.stop();

        logger.info("Shutting down UDP Server");
        isUDPServerRun.set(false);
        if (udpServer != null)
            udpServer.stop();


        logger.info("Shutting down connections");
        connectionManager.closeAllConnections();

        try {
            rawInputQueue.put(ServerSignals.POISON_PILL_BYTES);
        }
        catch (InterruptedException e) {
            logger.error("Interrupted while sending poison pills: ", e);
            Thread.currentThread().interrupt();
        }

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
