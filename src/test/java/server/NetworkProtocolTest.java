package server;

import client.StoreClientTCP;
import client.StoreClientUDP;
import decryptor.IDecryptor;
import decryptor.MessageDecryptor;
import dto.Message;
import encryptor.IEncryptor;
import encryptor.MessageEncryptor;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import utils.Constants;

import java.net.InetAddress;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class NetworkProtocolTest extends BaseIntegrationTest{
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
    public void testUdpReaperAndCacheClearing() throws InterruptedException {
        Server server = initServer(3, 3, 3, 3);
        server.start();
        Thread.sleep(200);

        MessageDecryptor realDecryptor = new MessageDecryptor();
        IDecryptor spyDecryptor = Mockito.spy(realDecryptor);

        BlockingQueue<Message> rawReceivedMessages = new LinkedBlockingQueue<>();

        Mockito.doAnswer(invocation -> {
            Message msg = (Message) invocation.callRealMethod();
            if (msg != null && msg.getCommandId() != Constants.ACK_COMMAND_ID)
                rawReceivedMessages.add(msg);

            return msg;
        }).when(spyDecryptor).decrypt(Mockito.any(byte[].class));

        StoreClientUDP client = new StoreClientUDP(
                new MessageEncryptor(),
                spyDecryptor,
                _ -> {}
        );

        client.connect(InetAddress.getLoopbackAddress(), 10000);

        Message msg1 = new Message((byte)0, 777, 10, 1, "{}");
        client.sendCommand(msg1);

        Message resp1 = rawReceivedMessages.poll(3, TimeUnit.SECONDS);
        assertNotNull(resp1);
        assertEquals(200, resp1.getCommandId());

        client.sendCommand(msg1);
        Message resp2 = rawReceivedMessages.poll(2, TimeUnit.SECONDS);
        assertNull(resp2);

        Thread.sleep(20000);

        client.sendCommand(msg1);
        Message resp3 = rawReceivedMessages.poll(3, TimeUnit.SECONDS);

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
