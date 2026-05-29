package dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UnackedMessage<T> {
    private final T message;
    private long lastSendTime;
    private int retryCount;

    public UnackedMessage(T message) {
        this.message = message;
        lastSendTime = System.currentTimeMillis();
        retryCount = 0;
    }
}
