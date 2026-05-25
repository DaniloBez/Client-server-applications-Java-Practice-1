package server;

import decryptor.DecryptorNode;
import decryptor.IDecryptor;
import decryptor.MessageDecryptor;
import dto.Message;
import encryptor.EncryptorNode;
import encryptor.MessageEncryptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import processor.IProcessor;
import processor.Processor;
import processor.ProcessorNode;
import receiver.FakeReceiver;
import receiver.ReceiverNode;
import repository.ProductCategoryRepository;
import repository.ProductRepository;
import sender.FakeSender;
import sender.ISender;
import sender.SenderNode;
import service.ProductCategoryService;
import service.ProductService;

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

    public Server() {
        this.executorService = Executors.newFixedThreadPool(16);
    }

    public void start() {
        logger.info("Initializing server components...");

        ProductCategoryRepository categoryRepository = new ProductCategoryRepository();
        ProductRepository productRepository = new ProductRepository();

        ProductCategoryService categoryService = new ProductCategoryService(categoryRepository, productRepository);
        ProductService productService = new ProductService(productRepository, categoryRepository);

        IProcessor processor = new Processor(productService, categoryService);
        IDecryptor decryptor = new MessageDecryptor();
        MessageEncryptor encryptor = new MessageEncryptor();
        ISender sender = new FakeSender();

        logger.info("Launching threads (Scale up)...");

        int receiverCount = 2;
        AtomicInteger activeReceivers = new AtomicInteger(receiverCount);
        for (int i = 0; i < receiverCount; i++) {
            ReceiverNode receiverNode = new ReceiverNode(rawInputQueue, new FakeReceiver(), activeReceivers);
            receiverNodes.add(receiverNode);
            executorService.submit(receiverNode);
        }

        int decryptorCount = 2;
        AtomicInteger activeDecryptors = new AtomicInteger(decryptorCount);
        for (int i = 0; i < 2; i++)
            executorService.submit(new DecryptorNode(rawInputQueue, decodedQueue, decryptor, activeDecryptors));

        int processorCount = 4;
        AtomicInteger activeProcessors = new AtomicInteger(processorCount);
        for (int i = 0; i < 4; i++)
            executorService.submit(new ProcessorNode(decodedQueue, responseQueue, processor, activeProcessors));

        int encryptorCount = 3;
        AtomicInteger activeEncryptors = new AtomicInteger(encryptorCount);
        for (int i = 0; i < encryptorCount; i++)
            executorService.submit(new EncryptorNode(responseQueue, rawOutputQueue, encryptor, activeEncryptors));

        int senderCount = 5;
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
    }
}
