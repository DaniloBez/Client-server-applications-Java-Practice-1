package dto.request;

public record AddStockRequest(
        int productId,
        int amount
) {
}
