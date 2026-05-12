package data;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

@Getter
@Setter
@AllArgsConstructor
@EqualsAndHashCode
public class Message {
    private byte clientApplicationId;

    private long messageId;

    private int commandId;

    private int userId;

    private String data;
}
