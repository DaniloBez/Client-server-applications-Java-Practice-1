package server;

import dto.NetworkMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import receiver.UDPReceiver;

import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class StoreServerUDP implements Runnable {
    private final static Logger logger = LoggerFactory.getLogger(StoreServerUDP.class);

    private final int port;
    private final ConnectionManager connectionManager;
    private final AtomicBoolean isRunning;
    private final LinkedTransferQueue<NetworkMessage<byte[]>> rawInputQueue;

    private DatagramSocket serverSocket;
    private Thread receiverThread;

    public StoreServerUDP(int port, ConnectionManager connectionManager, AtomicBoolean isRunning, LinkedTransferQueue<NetworkMessage<byte[]>> rawInputQueue) {
        this.port = port;
        this.connectionManager = connectionManager;
        this.isRunning = isRunning;
        this.rawInputQueue = rawInputQueue;
    }

    @Override
    public void run() {
        try {
            serverSocket = new DatagramSocket(port);
            logger.info("Store Server UDP listening on port {}", port);

            UDPReceiver udpReceiver = new UDPReceiver(serverSocket, connectionManager, rawInputQueue, isRunning);
            receiverThread = new Thread(udpReceiver, "UDP-Receiver-Thread");
            receiverThread.start();

        } catch (SocketException e) {
            logger.error("Store Server UDP Initialization Error", e);
        }
    }

    public void stop() {
        if (serverSocket != null && !serverSocket.isClosed())
            serverSocket.close();

        if (receiverThread != null)
            receiverThread.interrupt();
    }
}
