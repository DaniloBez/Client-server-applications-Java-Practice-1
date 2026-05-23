package encryptor;

import dto.Message;

public interface IEncryptor {
    byte[] encrypt(Message message);
}
