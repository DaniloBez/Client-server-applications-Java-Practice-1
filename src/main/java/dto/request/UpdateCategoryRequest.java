package dto.request;

public record UpdateCategoryRequest(
        int id,
        String newName
) {
}
