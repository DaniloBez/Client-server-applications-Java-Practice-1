package sender;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class TcpSender implements ISender {
    private final static Logger logger = LoggerFactory.getLogger(TcpSender.class);

    private final DataOutputStream out;

    public TcpSender(Socket socket) throws IOException {
        this.out = new DataOutputStream(socket.getOutputStream());
    }

    @Override
    public void send(byte[] message) {
        try {
            out.writeInt(message.length);
            out.write(message);
            out.flush();
        } catch (IOException e) {
            logger.error("Failed to send data to TCP socket");
        }
    }

    @Override
    public void close() {
        try {
            out.close();
            logger.info("TCP socket closed successfully");
        }
        catch (IOException e) {
            logger.error("Error closing TCP socket", e);
        }
    }
}
