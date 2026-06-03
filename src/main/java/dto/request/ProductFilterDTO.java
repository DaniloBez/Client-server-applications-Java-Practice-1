package dto.request;

import java.math.BigDecimal;

public record ProductFilterDTO(
        String nameLike,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        Integer categoryId
) {
}
