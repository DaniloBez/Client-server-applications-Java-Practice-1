package client;

import decryptor.IDecryptor;
import dto.Message;
import dto.UnackedMessage;
import encryptor.IEncryptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.Constants;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class StoreClientUDP implements IClient {
    private static final Logger logger = LoggerFactory.getLogger(StoreClientUDP.class);

    private DatagramSocket socket;
    private InetAddress serverAddress;
    private int serverPort;

    private volatile boolean isConnected = false;

    private final IEncryptor encryptor;
    private final IDecryptor decryptor;
    private final Consumer<Message> onMessageReceived;

    private final ConcurrentHashMap<Long, UnackedMessage<byte[]>> unackedMessages = new ConcurrentHashMap<>();
    private ScheduledExecutorService resender;

    private final Set<Long> idempotencyCache = Collections.synchronizedSet(
            Collections.newSetFromMap(
                    new LinkedHashMap<Long, Boolean>(100, 0.75f, true) {
                        @Override
                        protected boolean removeEldestEntry(Map.Entry<Long, Boolean> eldest) {
                            return size() > 100;
                        }
                    }
            )
    );

    private final long TIMEOUT_MS = 100;
    private final int MAX_RETRIES = 5;


    public StoreClientUDP(IEncryptor encryptor, IDecryptor decryptor, Consumer<Message> onMessageReceived) {
        this.encryptor = encryptor;
        this.decryptor = decryptor;
        this.onMessageReceived = onMessageReceived;
    }

    @Override
    public void connect(InetAddress address, int port) {
        try {
            this.serverAddress = address;
            this.serverPort = port;

            this.socket = new DatagramSocket();
            this.isConnected = true;

            Thread listenerThread = new Thread(this::listenForServerMessages, "UDP-Client-Listener");
            listenerThread.setDaemon(true);
            listenerThread.start();


            this.resender = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "UDP-Client-resender");
                t.setDaemon(true);
                return t;
            });
            this.resender.scheduleAtFixedRate(
                    this::resendMessages,
                    100,
                    100,
                    TimeUnit.MILLISECONDS
            );

            logger.info("UDP Client initialized and ready to send to {}:{}", address, port);
        } catch (IOException e) {
            logger.error("Failed to initialize UDP socket", e);
        }
    }

    @Override
    public void disconnect() {
        this.isConnected = false;

        if (resender != null && !resender.isShutdown()) {
            resender.shutdownNow();
        }

        if (socket != null && !socket.isClosed()) {
            socket.close();
            logger.info("UDP Client socket closed");
        }
    }


    @Override
    public void sendCommand(Message message) {
        if (!isConnected || socket == null || socket.isClosed()) {
            logger.info("Socket is closed! Adding the message to the local queue...");
            return;
        }

        try {
            byte[] encryptedMessage = encryptor.encrypt(message);

            DatagramPacket packet = new DatagramPacket(
                    encryptedMessage,
                    encryptedMessage.length,
                    serverAddress,
                    serverPort
            );

            unackedMessages.put(
                    message.getMessageId(),
                    new UnackedMessage<>(encryptedMessage)
            );

            socket.send(packet);

        } catch (IOException e) {
            logger.error("Failed to send UDP command!", e);
        }
    }

    private void resendMessages() {
        long currentTime = System.currentTimeMillis();

        for (var entry : unackedMessages.entrySet()) {
            UnackedMessage<byte[]> unackedMessage = entry.getValue();
            if (currentTime - unackedMessage.getLastSendTime() > TIMEOUT_MS) {
                if (unackedMessage.getRetryCount() < MAX_RETRIES) {
                    unackedMessage.setRetryCount(unackedMessage.getRetryCount() + 1);
                    unackedMessage.setLastSendTime(currentTime);

                    DatagramPacket packet = new DatagramPacket(
                            unackedMessage.getMessage(),
                            unackedMessage.getMessage().length,
                            serverAddress,
                            serverPort
                    );

                    try {
                        socket.send(packet);
                    }
                    catch (IOException e) {
                        logger.error("Failed to send UDP message to server, {}", e.getMessage());
                    }
                }
                else {
                    unackedMessages.remove(entry.getKey());
                    logger.error("UDP connection timed out");
                }
            }
        }
    }

    private void listenForServerMessages() {
        byte[] buffer = new byte[8192];

        try {
            while (isConnected && !Thread.currentThread().isInterrupted() && !socket.isClosed()) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                socket.receive(packet);

                byte[] data = Arrays.copyOfRange(packet.getData(), 0, packet.getLength());

                Message message = decryptor.decrypt(data);

                if (message.getCommandId() == Constants.ACK_COMMAND_ID) {
                    unackedMessages.remove(message.getMessageId());
                    continue;
                }

                sendAck(message.getMessageId());

                boolean isNewMessage = idempotencyCache.add(message.getMessageId());
                if (!isNewMessage) {
                    logger.debug("Duplicate message from server ignored: {}", message.getMessageId());
                    continue;
                }

                if (onMessageReceived != null)
                    onMessageReceived.accept(message);

                logger.info("Received message from UDP Server {}:{}: {}", packet.getAddress().getAddress(), packet.getPort(), message);
            }
        } catch (IOException e) {
            if (!isConnected)
                logger.info("STOPPED LISTENING TO UDP SERVER! {}:{}", serverAddress, serverPort);
            else
                logger.error("CONNECTION TO THE SERVER HAS BEEN LOST! {}:{}, {}", serverAddress, serverPort, e.getMessage());
        }
    }

    private void sendAck(long messageId) {
        if (!isConnected || socket == null || socket.isClosed()) return;

        try {
            Message ackMessage = new Message((byte)0, messageId, Constants.ACK_COMMAND_ID, 0, "{}");
            byte[] encryptedAck = encryptor.encrypt(ackMessage);

            DatagramPacket packet = new DatagramPacket(
                    encryptedAck,
                    encryptedAck.length,
                    serverAddress,
                    serverPort
            );

            socket.send(packet);
        } catch (IOException e) {
            logger.error("Failed to send ACK to server", e);
        }
    }
}
