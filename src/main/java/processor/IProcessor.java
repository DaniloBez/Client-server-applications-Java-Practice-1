package processor;

import dto.Message;

import java.util.List;

public interface IProcessor {
    List<Message> process(Message message);
}
