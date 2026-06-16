package server;

import client.IClient;
import client.StoreClientHTTP;
import dto.Message;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PerformanceAndChaosTest extends BaseIntegrationTest {
    private static int messageId = 1;
    private Message getMessage(int requestId, int clientId) {
        int cmdChoice = requestId % 5;
        int commandId;
        String payload;

        if (cmdChoice == 0) {
            commandId = 10;
            payload = "{}";
        } else if (cmdChoice == 1) {
            commandId = 3;
            payload = "{\"productId\":" + testProductId + "}";
        } else if (cmdChoice == 2) {
            commandId = 7;
            payload = "{\"id\":" + testCategoryId + "}";
        } else if (cmdChoice == 3) {
            commandId = 11;
            payload = "{\"id\":" + testProductId + "}";
        } else {
            commandId = 13;
            payload = "{\"filter\":{\"categoryId\":" + testCategoryId + "}}";
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
    public void highLoadTest() throws InterruptedException {
        int tcpUdpClientCount = 300;
        int httpClientCount = 200;
        int clientCount = tcpUdpClientCount + httpClientCount;
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
            for (int i = 1; i <= tcpUdpClientCount; i++) {
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
            
            for (int i = 1; i <= httpClientCount; i++) {
                executor.submit(() -> {
                    try {
                        StoreClientHTTP httpClient = new StoreClientHTTP("localhost", 8080);
                        httpClient.login("testuser", "testpass");

                        readyLatch.countDown();
                        startLatch.await();

                        for (int requestCount = 0; requestCount < requestsPerClient; requestCount++) {
                            try {
                                if (httpClient.getProduct(testProductId))
                                    successfulResponses.incrementAndGet();
                                else
                                    errorResponses.incrementAndGet();
                            } catch (Exception e) {
                                errorResponses.incrementAndGet();
                            } finally {
                                allResponsesLatch.countDown();
                            }

                            Thread.sleep(5);
                        }
                    } catch (Exception e) {
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
    public void testConcurrentDeleteRaceCondition() throws InterruptedException {
        int threadCount = 10;

        Server server = initServer(5, 2, 2, 2);
        server.start();
        Thread.sleep(200);

        client.StoreClientHTTP setupClient = new client.StoreClientHTTP("localhost", 8080);
        setupClient.login("testuser", "testpass");
        int newProductId = setupClient.createProduct("TargetToKill", 100, new java.math.BigDecimal("99.99"), testCategoryId);

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        try (ExecutorService executor = Executors.newFixedThreadPool(threadCount)) {
            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        client.StoreClientHTTP httpClient = new client.StoreClientHTTP("localhost", 8080);
                        httpClient.login("testuser", "testpass");

                        startLatch.await();

                        boolean deleted = httpClient.deleteProduct(newProductId);
                        if (deleted)
                            successCount.incrementAndGet();
                        else
                            failCount.incrementAndGet();
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            startLatch.countDown();

            doneLatch.await(10, TimeUnit.SECONDS);

            System.out.println("Concurrent Delete Test Results:");
            System.out.println("Successful deletes: " + successCount.get());
            System.out.println("Failed deletes: " + failCount.get());

            assertEquals(1, successCount.get());
            assertEquals(threadCount - 1, failCount.get());
        } finally {
            server.stop();
        }
    }
}
