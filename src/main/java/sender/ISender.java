package sender;

public interface ISender {
    void send(byte[] message);
    void close();
}
