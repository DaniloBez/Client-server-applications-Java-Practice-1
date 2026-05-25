package dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class Message {
    private byte clientApplicationId;

    private long messageId;

    private int commandId;

    private int userId;

    private String data;
}
