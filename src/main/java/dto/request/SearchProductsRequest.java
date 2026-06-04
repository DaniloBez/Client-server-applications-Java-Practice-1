package dto.request;

public record SearchProductsRequest(
        ProductFilterDTO filter,
        PaginationDTO pagination,
        SortDTO sort
) {
}
