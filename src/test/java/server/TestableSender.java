package server;

import decryptor.IDecryptor;
import decryptor.MessageDecryptor;
import dto.Message;
import lombok.Getter;
import sender.ISender;

import java.net.InetAddress;
import java.util.concurrent.ConcurrentLinkedQueue;

public class TestableSender implements ISender {
    private final IDecryptor decryptor;

    @Getter
    private final ConcurrentLinkedQueue<Message> receivedMessages;

    public TestableSender() {
        this.decryptor = new MessageDecryptor();
        this.receivedMessages = new ConcurrentLinkedQueue<>();
    }

    @Override
    public void send(byte[] message, InetAddress target) {
        try {
            Message response = decryptor.decrypt(message);

            receivedMessages.add(response);

        } catch (Exception e) {
            System.err.println("Помилка дешифрування в TestableSender: " + e.getMessage());
        }
    }
}
