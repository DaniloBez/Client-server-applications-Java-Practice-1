package dto.broadcast;

import java.math.BigDecimal;

public record ProductPriceUpdatedBroadcast(
        int productId,
        BigDecimal newPrice
) {}
