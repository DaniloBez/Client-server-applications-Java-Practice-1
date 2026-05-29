package server;

import client.IClient;
import client.StoreClientTCP;
import client.StoreClientUDP;
import decryptor.IDecryptor;
import decryptor.MessageDecryptor;
import dto.Message;
import dto.request.DeductStockRequest;
import encryptor.IEncryptor;
import encryptor.MessageEncryptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import processor.IProcessor;
import processor.Processor;
import repository.ProductCategoryRepository;
import repository.ProductRepository;
import service.ProductCategoryService;
import service.ProductService;
import tools.jackson.databind.ObjectMapper;
import utils.Constants;

import java.math.BigDecimal;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

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

    private IClient getClient(int id, Consumer<Message> consumer) {
        if (id % 2 == 0)
            return new StoreClientTCP(
                    new MessageEncryptor(),
                    new MessageDecryptor(),
                    consumer
            );
        else
            return new StoreClientUDP(
                    new MessageEncryptor(),
                    new MessageDecryptor(),
                    consumer
            );

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

    @BeforeEach
    public void setup() {
        messageId = 1;
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
    public void smokeUDPClientTest() throws InterruptedException, ExecutionException, TimeoutException {
        CompletableFuture<Message> futureResponse = new CompletableFuture<>();

        Message message = new Message(
                (byte)0,
                1,
                10,
                1,
                "{}"
        );

        StoreClientUDP client = new StoreClientUDP(
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
    public void testRaceCondition() throws InterruptedException {
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

            Consumer<Message> consumer = message -> {
                if (message.getCommandId() == 200)
                    successCount.incrementAndGet();
                else
                    blockedCount.incrementAndGet();

                doneLatch.countDown();
            };

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


                        IClient client = getClient(finalI, consumer);

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
        int clientCount = 500;
        int requestsPerClient = 100;
        int totalRequests = clientCount * requestsPerClient;
        int senderCount = 25;
        int decryptorCount = 5;
        int encryptorCount = 5;
        int processorCount = 5;

        Server server = initServer(senderCount, decryptorCount, encryptorCount, encryptorCount);
        server.start();
        Thread.sleep(200);

        AtomicInteger successfulResponses = new AtomicInteger(0);
        AtomicInteger errorResponses = new AtomicInteger(0);

        CountDownLatch readyLatch = new CountDownLatch(clientCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch allResponsesLatch = new CountDownLatch(totalRequests);

        Consumer<Message> consumer = message -> {
            if (message.getCommandId() == 200)
                successfulResponses.incrementAndGet();
            else
                errorResponses.incrementAndGet();

            allResponsesLatch.countDown();
        };

        List<IClient> clients = Collections.synchronizedList(new ArrayList<>());

        try(ExecutorService executor = Executors.newFixedThreadPool(clientCount)) {
            for (int i = 1; i <= clientCount; i++) {
                final int clientId = i;

                executor.submit(() -> {
                    try {
                        IClient client = getClient(clientId, consumer);
                        clients.add(client);

                        client.connect(InetAddress.getLoopbackAddress(), 10000);

                        readyLatch.countDown();
                        startLatch.await();

                        for (int requestCount = 0; requestCount < requestsPerClient; requestCount++) {
                            Message input = getMessage(requestCount, clientId);
                            client.sendCommand(input);

                            Thread.sleep(5);
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
                System.out.println("Throughput (TPS + UDP): " + (totalRequests * 1000L / duration) + " req/sec");

            Thread.sleep(500);
            for (IClient c : clients)
                c.disconnect();

            Thread.sleep(500);
            server.stop();

            assertTrue(finishedInTime, "Timeout! The server was unable to process all requests in time.");
            assertEquals(totalRequests, successfulResponses.get() + errorResponses.get());
        }
    }

    @Test
    public void testChaosAndResilience() throws InterruptedException {
        int totalClients = 50;
        int clientsToKill = 25;

        Server server = initServer(3, 3, 3, 3);
        server.start();
        Thread.sleep(200);

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch firstWaveLatch = new CountDownLatch(totalClients);
        CountDownLatch secondWaveLatch = new CountDownLatch(totalClients - clientsToKill);

        AtomicInteger successfulSecondWave = new AtomicInteger(0);
        IClient[] clients = new IClient[totalClients];

        Consumer<Message> consumer = message -> {
            if (message.getMessageId() == 999) {
                successfulSecondWave.incrementAndGet();
                secondWaveLatch.countDown();
            } else
                firstWaveLatch.countDown();
        };

        try(ExecutorService executor = Executors.newFixedThreadPool(totalClients)) {
            for (int i = 0; i < totalClients; i++) {
                final int index = i;
                executor.submit(() -> {
                    try {
                        IClient client = getClient(index, consumer);

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

    @Test
    void testTCPClientReconnectAndOfflineQueue() throws InterruptedException {
        CountDownLatch firstMessageLatch = new CountDownLatch(1);
        CountDownLatch offlineMessagesLatch = new CountDownLatch(2);

        StoreClientTCP client = new StoreClientTCP(
                new MessageEncryptor(),
                new MessageDecryptor(),
                message ->  {
                    if (message.getMessageId() == 1)
                        firstMessageLatch.countDown();
                    else if (message.getMessageId() == 2 || message.getMessageId() == 3)
                        offlineMessagesLatch.countDown();
                }
        );

        Server server = initServer(3, 3, 3, 3);
        server.start();
        client.connect(InetAddress.getLoopbackAddress(), 10000);
        Thread.sleep(500);

        Message normalMessage = new Message((byte)0, 1, 10, 1, "{}");
        client.sendCommand(normalMessage);

        assertTrue(firstMessageLatch.await(2, TimeUnit.SECONDS));

        server.stop();

        Thread.sleep(1000);

        client.sendCommand(new Message((byte)0, 2, 10, 1, "{}"));
        client.sendCommand(new Message((byte)0, 3, 10, 1, "{}"));

        server = initServer(3, 3, 3, 3);
        server.start();

        assertTrue(offlineMessagesLatch.await(5, TimeUnit.SECONDS));

        client.disconnect();
        server.stop();
    }

    @Test
    public void testUdpReaperAndCacheClearing() throws InterruptedException{
        Server server = initServer(3, 3, 3, 3);
        server.start();
        Thread.sleep(200);

        BlockingQueue<Message> receivedMessages = new LinkedBlockingQueue<>();

        StoreClientUDP client = new StoreClientUDP(
                new MessageEncryptor(),
                new MessageDecryptor(),
                receivedMessages::add
        );

        client.connect(InetAddress.getLoopbackAddress(), 10000);

        Message msg1 = new Message((byte)0, 777, 10, 1, "{}");
        client.sendCommand(msg1);

        Message resp1 = receivedMessages.poll(3, TimeUnit.SECONDS);
        assertNotNull(resp1);
        assertEquals(200, resp1.getCommandId());

        client.sendCommand(msg1);
        Message resp2 = receivedMessages.poll(2, TimeUnit.SECONDS);
        assertNull(resp2);

        Thread.sleep(20000);

        client.sendCommand(msg1);
        Message resp3 = receivedMessages.poll(3, TimeUnit.SECONDS);
        assertNotNull(resp3);
        assertEquals(200, resp3.getCommandId());

        client.disconnect();
        server.stop();
    }

    @Test
    public void testUdpClientIdempotencyWithMock() throws Exception {
        Server server = initServer(3, 3, 3, 3);
        server.start();
        Thread.sleep(200);

        MessageEncryptor realEncryptor = new MessageEncryptor();
        IEncryptor spyEncryptor = Mockito.spy(realEncryptor);

        Mockito.doAnswer(invocation -> {
            Message msg = invocation.getArgument(0);
            if (msg.getCommandId() == Constants.ACK_COMMAND_ID) {
                return new byte[]{1, 2, 3};
            }
            return realEncryptor.encrypt(msg);
        }).when(spyEncryptor).encrypt(Mockito.any(Message.class));

        MessageDecryptor realDecryptor = new MessageDecryptor();
        IDecryptor spyDecryptor = Mockito.spy(realDecryptor);

        AtomicInteger rawMessagesCount = new AtomicInteger(0);
        CountDownLatch rawMessagesLatch = new CountDownLatch(6);

        Mockito.doAnswer(invocation -> {
            Message msg = (Message) invocation.callRealMethod();
            
            if (msg != null && msg.getCommandId() != Constants.ACK_COMMAND_ID) {
                rawMessagesCount.incrementAndGet();
                rawMessagesLatch.countDown();
            }
            return msg;
        }).when(spyDecryptor).decrypt(Mockito.any(byte[].class));

        AtomicInteger processedMessagesCount = new AtomicInteger(0);
        CountDownLatch firstMessageLatch = new CountDownLatch(1);

        StoreClientUDP client = new StoreClientUDP(
                spyEncryptor,
                spyDecryptor,
                _ -> {
                    processedMessagesCount.incrementAndGet();
                    firstMessageLatch.countDown();
                }
        );

        client.connect(InetAddress.getLoopbackAddress(), 10000);

        Message request = new Message((byte)0, 777, 10, 1, "{}");
        client.sendCommand(request);

        assertTrue(firstMessageLatch.await(3, TimeUnit.SECONDS));


        boolean serverSentAllRetries = rawMessagesLatch.await(3, TimeUnit.SECONDS);

        Thread.sleep(300);

        assertTrue(serverSentAllRetries);
        assertEquals(6, rawMessagesCount.get());

        assertEquals(1, processedMessagesCount.get());

        client.disconnect();
        server.stop();
    }
}
