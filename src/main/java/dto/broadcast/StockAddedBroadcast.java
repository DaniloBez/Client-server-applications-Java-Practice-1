package dto.broadcast;

public record StockAddedBroadcast(
        int productId,
        int addedAmount
) {}