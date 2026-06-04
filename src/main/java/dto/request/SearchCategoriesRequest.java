package dto.request;

public record SearchCategoriesRequest(
        CategoryFilterDTO filter,
        PaginationDTO pagination,
        SortDTO sort
)  {
}
