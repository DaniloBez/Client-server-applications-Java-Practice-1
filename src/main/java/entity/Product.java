package entity;

import java.math.BigDecimal;

public record Product(
        int id,
        String name,
        int countInStock,
        BigDecimal price,
        int productCategoryId
) {}
