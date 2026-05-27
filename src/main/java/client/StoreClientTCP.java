package client;

import decryptor.IDecryptor;
import dto.Message;
import encryptor.IEncryptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.function.Consumer;

public class StoreClientTCP {
    private static final Logger logger = LoggerFactory.getLogger(StoreClientTCP.class);

    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;
    private volatile boolean isConnected = false;
    private final IEncryptor encryptor;
    private final IDecryptor decryptor;
    private final Consumer<Message> onMessageReceived;

    public StoreClientTCP(IEncryptor encryptor, IDecryptor decryptor, Consumer<Message> onMessageReceived) {
        this.encryptor = encryptor;
        this.decryptor = decryptor;
        this.onMessageReceived = onMessageReceived;
    }

    public void connect(InetAddress address, int port) {
        try {
            socket = new Socket(address, port);
            out = new DataOutputStream(socket.getOutputStream());
            in = new DataInputStream(socket.getInputStream());
            isConnected = true;

            Thread listenerThread = new Thread(this::listenForServerMessages);
            listenerThread.setDaemon(true);
            listenerThread.start();

            logger.info("Successfully connected to {}:{}", address, port);
        } catch (IOException e) {
            logger.error("Failed to connect to {}:{}", address, port, e);
        }
    }

    private void listenForServerMessages() {
        try {
            while (isConnected && !Thread.currentThread().isInterrupted()) {
                int length = in.readInt();
                byte[] data = new byte[length];
                in.readFully(data);

                Message message = decryptor.decrypt(data);
                if (onMessageReceived != null)
                    onMessageReceived.accept(message);

                logger.info("Received message from {}:{}: {}", socket.getInetAddress().getHostName(), socket.getPort(), message);
            }
        } catch (IOException e) {
            isConnected = false;
            logger.info("CONNECTION TO THE SERVER HAS BEEN LOST! {}:{}", socket.getInetAddress().getHostName(), socket.getPort());
        }
    }

    public void sendCommand(Message message) {
        if (!isConnected) {
            logger.info("No connection! Adding the message to the local queue...");
            return;
        }

        try {
            byte[] encryptedMessage = encryptor.encrypt(message);

            out.writeInt(encryptedMessage.length);
            out.write(encryptedMessage);
            out.flush();
        } catch (IOException e) {
            logger.error("Failed to send command!", e);
        }
    }
}
