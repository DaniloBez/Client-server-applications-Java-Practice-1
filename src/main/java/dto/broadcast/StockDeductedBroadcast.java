package dto.broadcast;

public record StockDeductedBroadcast(
        int productId,
        int deductedAmount
) {}