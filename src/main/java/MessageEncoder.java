import data.Message;
import exception.EncryptionException;
import utils.Crc16;
import utils.CryptoUtils;

import java.nio.ByteBuffer;

public class MessageEncoder {
    private static final byte MAGIC_BYTE = 0x13;
    private static final byte HEADER_LENGTH = 14;

    public byte[] encode(Message message){
        byte[] rawData = message.getData().getBytes();
        byte[] encryptedData;

        try {
            encryptedData = CryptoUtils.encrypt(rawData);
        } catch (Exception e) {
            throw new EncryptionException(e.getMessage());
        }

        int payloadSize = 4 + 4 + encryptedData.length;
        int totalSize = HEADER_LENGTH + 2 + payloadSize + 2;

        ByteBuffer buffer = ByteBuffer.allocate(totalSize);

        buffer.put(MAGIC_BYTE);
        buffer.put(message.getClientApplicationId());
        buffer.putLong(message.getMessageId());
        buffer.putInt(payloadSize);

        byte[] headerForCrc = new byte[HEADER_LENGTH];
        buffer.position(0);
        buffer.get(headerForCrc);
        buffer.putShort(Crc16.calculateCrc(headerForCrc));

        int payloadStart = buffer.position();
        buffer.putInt(message.getCommandId());
        buffer.putInt(message.getUserId());
        buffer.put(encryptedData);

        byte[] payloadForCrc = new byte[payloadSize];
        buffer.position(payloadStart);
        buffer.get(payloadForCrc);
        buffer.putShort(Crc16.calculateCrc(payloadForCrc));

        return buffer.array();
    }
}
