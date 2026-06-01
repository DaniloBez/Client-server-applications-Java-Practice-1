package dto.broadcast;

public record CategoryCreatedBroadcast(
        int categoryId,
        String name
) {}