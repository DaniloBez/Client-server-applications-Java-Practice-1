package dto.broadcast;

public record CategoryNameUpdatedBroadcast(
        int categoryId,
        String newName
) {}