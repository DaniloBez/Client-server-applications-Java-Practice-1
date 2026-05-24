package dto.request;

public record DeductStockRequest(
        int productId,
        int amount
) {
}
