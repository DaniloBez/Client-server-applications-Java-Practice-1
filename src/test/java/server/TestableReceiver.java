package server;

import dto.Message;
import encryptor.IEncryptor;
import encryptor.MessageEncryptor;
import receiver.IReceiver;

import java.util.function.Supplier;

public class TestableReceiver implements IReceiver {
    private final IEncryptor encryptor;
    private final Supplier<Message> messageGenerator;
    private final int delayMs;

    public TestableReceiver(Supplier<Message> messageGenerator, int delayMs) {
        this.messageGenerator = messageGenerator;
        this.delayMs = delayMs;
        this.encryptor = new MessageEncryptor();
    }

    @Override
    public byte[] receiveMessage() {
        try {
            if (delayMs > 0) Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new byte[0];
        }
        Message nextMessage = messageGenerator.get();
        return encryptor.encrypt(nextMessage);
    }
}
