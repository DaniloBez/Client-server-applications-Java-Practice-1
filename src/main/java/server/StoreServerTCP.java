package server;

import dto.NetworkMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import receiver.TCPReceiver;
import sender.TcpSender;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class StoreServerTCP implements Runnable {
    private final static Logger logger = LoggerFactory.getLogger(StoreServerTCP.class);
    private final int port;
    private final ConnectionManager connectionManager;
    private final AtomicBoolean isRunning;
    private final BlockingQueue<NetworkMessage<byte[]>> rawInputQueue;
    private final ExecutorService clientPool;

    private ServerSocket serverSocket;

    public StoreServerTCP(int port, ConnectionManager connectionManager, AtomicBoolean isRunning, BlockingQueue<NetworkMessage<byte[]>> rawInputQueue) {
        this.port = port;
        this.connectionManager = connectionManager;
        this.isRunning = isRunning;
        this.rawInputQueue = rawInputQueue;
        this.clientPool = Executors.newCachedThreadPool();
    }

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(port);
            logger.info("Store Server TCP listening on port {}", port);

            while (isRunning.get() && !serverSocket.isClosed()) {
                Socket clientSocket = serverSocket.accept();
                String connectionId = "TCP-" + UUID.randomUUID();

                TcpSender sender = new TcpSender(clientSocket);
                connectionManager.addConnection(connectionId, sender);

                clientPool.execute(new TCPReceiver(clientSocket, connectionId, rawInputQueue, connectionManager));
            }
        }
        catch (IOException e) {
            if (!isRunning.get())
                logger.info("TCP Server socket closed gracefully.");
            else
                logger.error("Store Server TCP Error", e);
        }
    }

    public void stop() {
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            logger.error("Error closing TCP server socket", e);
        }

        if (clientPool != null)
            clientPool.shutdownNow();
    }
}
