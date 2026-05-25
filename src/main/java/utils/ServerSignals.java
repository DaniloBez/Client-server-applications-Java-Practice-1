package utils;

import dto.Message;

public class ServerSignals {
    public static final byte[] POISON_PILL_BYTES = new byte[0];
    public static final Message POISON_PILL_MSG = new Message((byte)0, 0L, -1, 0, "");
}
