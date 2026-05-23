package decryptor;

import dto.Message;

public interface IDecryptor {
    Message decrypt(byte[] message);
}
