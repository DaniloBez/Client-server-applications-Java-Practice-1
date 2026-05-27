package receiver;

import dto.NetworkMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.ConnectionManager;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;

public class TCPReceiver implements Runnable {
    private final static Logger logger = LoggerFactory.getLogger(TCPReceiver.class);

    private final Socket socket;
    private final String connectionId;
    private final BlockingQueue<NetworkMessage<byte[]>> rawInputQueue;
    private final ConnectionManager connectionManager;

    public TCPReceiver(Socket socket, String connectionId, BlockingQueue<NetworkMessage<byte[]>> rawInputQueue, ConnectionManager connectionManager) {
        this.socket = socket;
        this.connectionId = connectionId;
        this.rawInputQueue = rawInputQueue;
        this.connectionManager = connectionManager;
    }

    @Override
    public void run() {
        try (DataInputStream in = new DataInputStream(socket.getInputStream())) {
            while (!Thread.currentThread().isInterrupted()) {
                int length = in.readInt();
                byte[] data = new byte[length];
                in.readFully(data);

                NetworkMessage<byte[]> envelope = new NetworkMessage<>(connectionId, data);
                rawInputQueue.put(envelope);
            }
        } catch (IOException e) {
            String errorMessage = e.getMessage();

            if (errorMessage != null && (errorMessage.contains("Socket closed") || errorMessage.contains("Connection reset")))
                logger.info("TCP client {} disconnected gracefully ({})", connectionId, errorMessage);
            else
                logger.error("TCP client {} unexpected error: {}", connectionId, errorMessage);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("TCP client {} thread error: {}.", connectionId, e.getMessage());
        }
        finally {
            connectionManager.removeConnection(connectionId);
            try {
                socket.close();
            } catch (IOException ex) { /* ignore */ }
        }
    }
}
