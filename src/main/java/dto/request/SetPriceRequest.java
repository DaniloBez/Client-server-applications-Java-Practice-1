package dto.request;

import java.math.BigDecimal;

public record SetPriceRequest(
        int productId,
        BigDecimal newPrice
) {
}
