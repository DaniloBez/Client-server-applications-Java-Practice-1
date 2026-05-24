package dto.request;

import java.math.BigDecimal;

public record CreateProductRequest(
        String name,
        int initialStock,
        BigDecimal price,
        int categoryId
) {
}
