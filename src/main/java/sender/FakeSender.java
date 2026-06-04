package sender;

import decryptor.IDecryptor;
import decryptor.MessageDecryptor;
import dto.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FakeSender implements ISender {
    private static final Logger logger = LoggerFactory.getLogger(FakeSender.class);

    private final IDecryptor clientDecryptor = new MessageDecryptor();

    @Override
    public void send(byte[] message) {
        try {
            Message responseMessage = clientDecryptor.decrypt(message);

            logger.info("Sending message: {} ", responseMessage);
        } catch (Exception e) {
            logger.error("Decryption error on the client: {}", e.getMessage());
        }
    }

    @Override
    public void close() {}
}
