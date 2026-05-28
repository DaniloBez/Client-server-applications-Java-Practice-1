package client;

import decryptor.IDecryptor;
import dto.Message;
import encryptor.IEncryptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;
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

            logger.info("UDP Client initialized and ready to send to {}:{}", address, port);
        } catch (IOException e) {
            logger.error("Failed to initialize UDP socket", e);
        }
    }

    @Override
    public void disconnect() {
        this.isConnected = false;
        if (socket != null && !socket.isClosed()) {
            socket.close();
            logger.info("UDP Client socket closed");
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

            socket.send(packet);

        } catch (IOException e) {
            logger.error("Failed to send UDP command!", e);
        }
    }
}
