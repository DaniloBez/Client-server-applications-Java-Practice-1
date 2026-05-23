package sender;

import java.net.InetAddress;

public interface ISender {
    void send(byte[] message, InetAddress target);
}
