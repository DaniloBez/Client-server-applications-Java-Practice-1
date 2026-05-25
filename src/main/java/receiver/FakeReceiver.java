package receiver;

import dto.Message;
import encryptor.IEncryptor;
import encryptor.MessageEncryptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class FakeReceiver implements IReceiver {
    private static final Logger logger = LoggerFactory.getLogger(FakeReceiver.class);

    private final List<Message> messagePool;
    private final Random random;

    private final IEncryptor clientEncryptor;

    public FakeReceiver() {
        this.clientEncryptor = new MessageEncryptor();
        this.random = new Random();

        this.messagePool = new ArrayList<>(List.of(
                new Message((byte) 1, 1001L, 1, 42, "{\"name\":\"Electronics\"}"),
                new Message((byte) 1, 1002L, 2, 42, "{\"name\":\"Laptop\",\"initialStock\":50,\"price\":1500.00,\"categoryId\":1}"),
                new Message((byte) 1, 1003L, 3, 42, "{\"productId\":1}"),
                new Message((byte) 1, 1004L, 4, 42, "{\"productId\":1,\"amount\":20}"),
                new Message((byte) 1, 1005L, 5, 42, "{\"productId\":1,\"amount\":5}"),
                new Message((byte) 1, 1006L, 6, 42, "{\"productId\":1,\"newPrice\":1400.00}"),
                new Message((byte) 1, 1007L, 10, 42, "{}")
        ));
    }

    @Override
    public byte[] receiveMessage() {
        Message msg = messagePool.get(random.nextInt(messagePool.size()));
        logger.info("Receiving message: {}", msg);
        return clientEncryptor.encrypt(msg);
    }
}
