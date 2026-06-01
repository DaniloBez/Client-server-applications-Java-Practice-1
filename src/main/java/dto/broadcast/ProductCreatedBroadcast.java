package dto.broadcast;

import java.math.BigDecimal;

public record ProductCreatedBroadcast(
        int productId,
        String name,
        int initialStock,
        BigDecimal price,
        int categoryId
) {}