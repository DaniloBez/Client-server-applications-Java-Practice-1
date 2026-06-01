package sender;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class UdpSender implements ISender {
    private final static Logger logger = LoggerFactory.getLogger(UdpSender.class);

    private final DatagramSocket serverSocket;

    private final InetAddress clientAddress;
    private final int clientPort;

    public UdpSender(DatagramSocket serverSocket, InetAddress clientAddress, int clientPort) {
        this.serverSocket = serverSocket;
        this.clientAddress = clientAddress;
        this.clientPort = clientPort;
    }

    @Override
    public synchronized void send(byte[] message) {
        if (serverSocket == null ||  serverSocket.isClosed())
            return;

        try {
            DatagramPacket packet = new DatagramPacket(
                    message,
                    message.length,
                    clientAddress,
                    clientPort
            );

            serverSocket.send(packet);
        } catch (SocketException e) {
            logger.debug("Socket was closed while sending to {}:{}. Ignored.", clientAddress.getHostAddress(), clientPort);
        }
        catch (IOException e) {
            logger.error("Failed to send data via UDP to {}:{} - {}", clientAddress.getHostAddress(), clientPort, e.getMessage());
        }
    }

    @Override
    public void close() {
        logger.info("UDP sender for {}:{} removed from registry", clientAddress.getHostAddress(), clientPort);
    }
}
