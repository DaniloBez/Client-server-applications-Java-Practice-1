package dto.response;

public record ActionStatusResponse(
        boolean success,
        String message
) {
}
