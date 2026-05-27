package server;

import client.StoreClientTCP;
import decryptor.IDecryptor;
import decryptor.MessageDecryptor;
import dto.Message;
import dto.request.DeductStockRequest;
import encryptor.MessageEncryptor;
import org.junit.jupiter.api.Test;
import processor.IProcessor;
import processor.Processor;
import repository.ProductCategoryRepository;
import repository.ProductRepository;
import service.ProductCategoryService;
import service.ProductService;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.net.InetAddress;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class ServerIntegrationTest {

    private Server initServer(
            int senderCount,
            int decryptorCount,
            int encryptorCount,
            int processorCount
            ) {
        ProductCategoryRepository categoryRepository = new ProductCategoryRepository();
        int id = categoryRepository.create("Electronics");

        ProductRepository productRepository = new ProductRepository();
        productRepository.create(
                "Product",
                10,
                new BigDecimal(10),
                id
        );

        ProductCategoryService categoryService = new ProductCategoryService(categoryRepository, productRepository);
        ProductService productService = new ProductService(productRepository, categoryRepository);

        IDecryptor serverDecryptor = new MessageDecryptor();
        MessageEncryptor serverEncryptor = new MessageEncryptor();
        IProcessor serverProcessor = new Processor(productService, categoryService);

        return new Server(
                senderCount,
                serverDecryptor,
                decryptorCount,
                serverEncryptor,
                encryptorCount,
                serverProcessor,
                processorCount,
                10000
        );
    }

    @Test
    public void smokeTCPClientTest() throws InterruptedException, ExecutionException, TimeoutException {
        CompletableFuture<Message> futureResponse = new CompletableFuture<>();

        Message message = new Message(
                (byte)0,
                1,
                10,
                1,
                "{}"
        );

        StoreClientTCP client = new StoreClientTCP(
                new MessageEncryptor(),
                new MessageDecryptor(),
                futureResponse::complete
        );

        Server server = initServer(5, 5, 5, 5);
        server.start();

        client.connect(InetAddress.getLoopbackAddress(), 10000);
        client.sendCommand(message);

        try {

            Message outputMessage = futureResponse.get(5, TimeUnit.SECONDS);

            assertNotNull(outputMessage);
            assertEquals(message.getClientApplicationId(), outputMessage.getClientApplicationId());
            assertEquals(message.getMessageId(), outputMessage.getMessageId());
            assertEquals(message.getUserId(), outputMessage.getUserId());
            assertEquals(200, outputMessage.getCommandId());
            assertEquals("{\"categories\":[{\"id\":1,\"name\":\"Electronics\"}]}", outputMessage.getData());

        } finally {
            server.stop();
        }
    }

    @Test
    public void testTCPRaceCondition() throws InterruptedException {
        int clientCount = 10;

        Server server = initServer(5, 5, 5, 5);
        server.start();

        ObjectMapper objectMapper = new ObjectMapper();

        final DeductStockRequest request = new DeductStockRequest(1, 5);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger blockedCount = new AtomicInteger(0);

        try(ExecutorService executor = Executors.newFixedThreadPool(clientCount)) {
            CountDownLatch readyLatch = new CountDownLatch(clientCount);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(clientCount);

            for (int i = 1; i <= clientCount; i++) {
                final int finalI = i;
                executor.submit(() -> {
                    try {
                        Message input = new Message(
                                (byte)0,
                                finalI,
                                5,
                                finalI,
                                objectMapper.writeValueAsString(request)
                        );

                        StoreClientTCP client = new StoreClientTCP(
                                new MessageEncryptor(),
                                new MessageDecryptor(),
                                output -> {
                                    if (output.getCommandId() == 200)
                                        successCount.incrementAndGet();
                                    else
                                        blockedCount.incrementAndGet();

                                    doneLatch.countDown();
                                }
                        );

                        client.connect(InetAddress.getLoopbackAddress(), 10000);

                        readyLatch.countDown();
                        startLatch.await();

                        client.sendCommand(input);
                    }
                    catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            }

            readyLatch.await();
            startLatch.countDown();
            doneLatch.await();
        }

        server.stop();
        assertEquals(clientCount, successCount.get() +  blockedCount.get());
        assertEquals(2, successCount.get());
        assertEquals(clientCount - 2, blockedCount.get());
    }

    @Test
    public void highLoadTest() throws InterruptedException {
        int clientCount = 1000;
        int requestsPerClient = 500;
        int totalRequests = clientCount * requestsPerClient;
        int senderCount = 50;
        int decryptorCount = 10;
        int encryptorCount = 10;
        int processorCount = 10;

        Server server = initServer(senderCount, decryptorCount, encryptorCount, encryptorCount);
        server.start();
        Thread.sleep(200);

        AtomicInteger successfulResponses = new AtomicInteger(0);
        AtomicInteger errorResponses = new AtomicInteger(0);

        CountDownLatch readyLatch = new CountDownLatch(clientCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch allResponsesLatch = new CountDownLatch(totalRequests);

        try(ExecutorService executor = Executors.newFixedThreadPool(clientCount)) {
            for (int i = 1; i <= clientCount; i++) {
                final int clientId = i;

                executor.submit(() -> {
                    try {
                        StoreClientTCP client = new StoreClientTCP(
                                new MessageEncryptor(),
                                new MessageDecryptor(),
                                output -> {
                                    if (output.getCommandId() == 200)
                                        successfulResponses.incrementAndGet();
                                    else
                                        errorResponses.incrementAndGet();

                                    allResponsesLatch.countDown();
                                }
                        );

                        client.connect(InetAddress.getLoopbackAddress(), 10000);

                        readyLatch.countDown();
                        startLatch.await();

                        for (int requestCount = 0; requestCount < requestsPerClient; requestCount++) {
                            Message input = getMessage(requestCount, clientId);
                            client.sendCommand(input);
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            }

            readyLatch.await();
            long startTime = System.currentTimeMillis();
            startLatch.countDown();
            boolean finishedInTime = allResponsesLatch.await(30, TimeUnit.SECONDS);
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            System.out.println("Characteristics: ");
            System.out.println("Sender count: " + senderCount);
            System.out.println("Decryptor count: " + decryptorCount);
            System.out.println("Encryptor count: " + encryptorCount);
            System.out.println("Processor count: " + processorCount);
            System.out.println();
            System.out.println("LOAD TEST RESULTS");
            System.out.println("Total client at the same time: " + clientCount);
            System.out.println("Total requests processed: " + totalRequests);
            System.out.println("Successful (200 OK): " + successfulResponses.get());
            System.out.println("Errors (400/500): " + errorResponses.get());
            System.out.println("Time taken: " + duration + " ms");
            if (duration > 0)
                System.out.println("Throughput (TPS): " + (totalRequests * 1000L / duration) + " req/sec");


            server.stop();

            assertTrue(finishedInTime, "Timeout! The server was unable to process all requests in time.");
            assertEquals(totalRequests, successfulResponses.get() + errorResponses.get());

        }
    }

    private static int messageId = 1;
    private static Message getMessage(int requestId, int clientId) {
        int cmdChoice = requestId % 5;
        int commandId;
        String payload;

        if (cmdChoice == 0) {
            commandId = 10;
            payload = "{}";
        } else if (cmdChoice == 1) {
            commandId = 3;
            payload = "{\"productId\":1}";
        } else if (cmdChoice == 2) {
            commandId = 4;
            payload = "{\"productId\":1, \"amount\":2}";
        } else if (cmdChoice == 3) {
            commandId = 5;
            payload = "{\"productId\":1, \"amount\":3}";
        } else {
            commandId = 7;
            payload = "{\"id\":1}";
        }

        return new Message(
                (byte)0,
                messageId++,
                commandId,
                clientId,
                payload
        );
    }

    @Test
    public void testTCPChaosAndResilience() throws InterruptedException {
        int totalClients = 50;
        int clientsToKill = 25;

        Server server = initServer(3, 3, 3, 3);
        server.start();
        Thread.sleep(200);

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch firstWaveLatch = new CountDownLatch(totalClients);
        CountDownLatch secondWaveLatch = new CountDownLatch(totalClients - clientsToKill);

        AtomicInteger successfulSecondWave = new AtomicInteger(0);
        StoreClientTCP[] clients = new StoreClientTCP[totalClients];

        try(ExecutorService executor = Executors.newFixedThreadPool(totalClients)) {
            for (int i = 0; i < totalClients; i++) {
                final int index = i;
                executor.submit(() -> {
                    try {
                        StoreClientTCP client = new StoreClientTCP(
                                new MessageEncryptor(),
                                new MessageDecryptor(),
                                output -> {
                                    if (output.getMessageId() == 999) {
                                        successfulSecondWave.incrementAndGet();
                                        secondWaveLatch.countDown();
                                    } else
                                        firstWaveLatch.countDown();
                                }
                        );

                        clients[index] = client;
                        client.connect(InetAddress.getLoopbackAddress(), 10000);

                        startLatch.await();

                        Message input = new Message((byte)0, index, 10, index, "{}");
                        client.sendCommand(input);

                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            }

            startLatch.countDown();
            firstWaveLatch.await(10, TimeUnit.SECONDS);

            for (int i = 0; i < clientsToKill; i++)
                clients[i].disconnect();

            for (int i = clientsToKill; i < totalClients; i++) {
                Message input2 = new Message((byte)0, i, 10, i, "{}");
                input2.setMessageId(999);
                clients[i].sendCommand(input2);
            }

            boolean serverSurvived = secondWaveLatch.await(10, TimeUnit.SECONDS);

            server.stop();

            org.junit.jupiter.api.Assertions.assertTrue(serverSurvived, "The server froze after the connections were lost!");
            assertEquals(totalClients - clientsToKill, successfulSecondWave.get());
        }
    }
}
