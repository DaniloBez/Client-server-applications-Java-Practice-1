package client;

import dto.Message;

import java.net.InetAddress;

public interface IClient {
    void connect(InetAddress address, int port);
    void disconnect();
    void sendCommand(Message message);
}
