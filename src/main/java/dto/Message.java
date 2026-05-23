package dto;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

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
