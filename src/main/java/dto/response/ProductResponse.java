package dto.response;

import java.math.BigDecimal;

public record ProductResponse(
        int id,
        String name,
        int countInStock,
        BigDecimal price,
        int categoryId
) {
}
