package processor;

import dto.Message;

public interface IProcessor {
    Message process(Message message);
}
