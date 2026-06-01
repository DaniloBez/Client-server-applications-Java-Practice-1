package receiver;

import dto.NetworkMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sender.UdpSender;
import server.ConnectionManager;
import utils.Constants;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class UDPReceiver implements Runnable {
    private final static Logger logger = LoggerFactory.getLogger(UDPReceiver.class);

    private final DatagramSocket serverSocket;
    private final ConnectionManager connectionManager;
    private final LinkedTransferQueue<NetworkMessage<byte[]>> rawInputQueue;
    private final AtomicBoolean isRunning;

    public UDPReceiver(DatagramSocket serverSocket, ConnectionManager connectionManager, LinkedTransferQueue<NetworkMessage<byte[]>> rawInputQueue, AtomicBoolean isRunning) {
        this.serverSocket = serverSocket;
        this.connectionManager = connectionManager;
        this.rawInputQueue = rawInputQueue;
        this.isRunning = isRunning;
    }

    @Override
    public void run() {
        byte[] buffer = new byte[8192];

        while (isRunning.get() && !Thread.currentThread().isInterrupted() && !serverSocket.isClosed()) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                serverSocket.receive(packet);

                InetAddress clientAddress = packet.getAddress();
                int clientPort = packet.getPort();

                String connectionId = Constants.UDP_HEADER + "-" + clientAddress.getHostAddress() + ":" + clientPort;
                connectionManager.updateUdpActivity(connectionId);

                if (connectionManager.getSender(connectionId) == null) {
                    UdpSender sender = new UdpSender(serverSocket, clientAddress, clientPort);
                    connectionManager.addConnection(connectionId, sender);
                }

                byte[] data = Arrays.copyOfRange(packet.getData(), 0, packet.getLength());

                NetworkMessage<byte[]> message = new NetworkMessage<>(connectionId, data);
                rawInputQueue.put(message);

            } catch (IOException e) {
                if (!isRunning.get() || serverSocket.isClosed()) {
                    logger.info("UDP Receiver socket closed gracefully.");
                    break;
                } else
                    logger.error("UDP Receiver unexpected error: {}", e.toString());
            }
        }
    }
}