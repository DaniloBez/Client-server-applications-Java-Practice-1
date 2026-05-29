package server;

import client.IClient;
import client.StoreClientTCP;
import client.StoreClientUDP;
import decryptor.MessageDecryptor;
import dto.Message;
import dto.request.DeductStockRequest;
import encryptor.MessageEncryptor;
import org.junit.jupiter.api.Test;
import processor.Processor;
import tools.jackson.databind.ObjectMapper;

import java.net.InetAddress;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ServerFunctionalTest extends BaseIntegrationTest{
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
            String expectedJson = String.format("{\"categories\":[{\"id\":%d,\"name\":\"Electronics\"}]}", testCategoryId);
            assertEquals(expectedJson, outputMessage.getData());
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
            String expectedJson = String.format("{\"categories\":[{\"id\":%d,\"name\":\"Electronics\"}]}", testCategoryId);
            assertEquals(expectedJson, outputMessage.getData());
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

        final DeductStockRequest request = new DeductStockRequest(testProductId, 5);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger blockedCount = new AtomicInteger(0);

        try(ExecutorService executor = Executors.newFixedThreadPool(clientCount)) {
            CountDownLatch readyLatch = new CountDownLatch(clientCount);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(clientCount);

            Consumer<Message> consumer = message -> {
                if (message.getUserId() == Processor.BROADCAST_USER_ID)
                    return;

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
    public void testBroadcastDelivery() throws InterruptedException {
        Server server = initServer(3, 3, 3, 3);
        server.start();
        Thread.sleep(200);

        CountDownLatch initiatorLatch = new CountDownLatch(1);
        CountDownLatch broadcastLatch = new CountDownLatch(2);

        AtomicInteger initiatorResponses = new AtomicInteger(0);
        AtomicInteger broadcastResponses = new AtomicInteger(0);

        IClient clientA = new StoreClientTCP(
                new MessageEncryptor(),
                new MessageDecryptor(),
                msg -> {
                    if (msg.getCommandId() == 200 && msg.getMessageId() == 201) {
                        initiatorResponses.incrementAndGet();
                        initiatorLatch.countDown();
                    }
                }
        );

        IClient clientB = new StoreClientUDP(
                new MessageEncryptor(),
                new MessageDecryptor(),
                msg -> {
                    if (msg.getCommandId() == 1) {
                        broadcastResponses.incrementAndGet();
                        broadcastLatch.countDown();
                    }
                }
        );


        IClient clientC = new StoreClientTCP(
                new MessageEncryptor(),
                new MessageDecryptor(),
                msg -> {
                    if (msg.getCommandId() == 1) {
                        broadcastResponses.incrementAndGet();
                        broadcastLatch.countDown();
                    }
                }
        );

        clientA.connect(InetAddress.getLoopbackAddress(), 10000);
        clientB.connect(InetAddress.getLoopbackAddress(), 10000);
        clientC.connect(InetAddress.getLoopbackAddress(), 10000);

        clientA.sendCommand(new Message((byte)0, 101, 10, 1, "{}"));
        clientB.sendCommand(new Message((byte)0, 102, 10, 2, "{}"));
        clientC.sendCommand(new Message((byte)0, 103, 10, 3, "{}"));

        Thread.sleep(500);

        Message createCategoryMsg = new Message((byte)0, 201, 1, 1, "{\"name\":\"New Category\"}");
        clientA.sendCommand(createCategoryMsg);

        assertTrue(initiatorLatch.await(3, TimeUnit.SECONDS));
        assertTrue(broadcastLatch.await(3, TimeUnit.SECONDS));
        assertEquals(1, initiatorResponses.get());
        assertEquals(2, broadcastResponses.get());

        clientA.disconnect();
        clientB.disconnect();
        clientC.disconnect();
        server.stop();
    }
}
