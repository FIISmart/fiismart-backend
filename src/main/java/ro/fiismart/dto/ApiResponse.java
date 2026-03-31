package ro.fiismart.dto;

import java.util.List;
import java.util.Map;

public record ApiResponse<T>(
        boolean success,
        T data,
        String message,
        Map<String, List<String>> errors
) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null, null);
    }

    public static <T> ApiResponse<T> ok(T data, String message) {
        return new ApiResponse<>(true, data, message, null);
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, null, message, null);
    }

    public static <T> ApiResponse<T> fieldError(String field, String message) {
        return new ApiResponse<>(false, null, "Validation failed",
                Map.of(field, List.of(message)));
    }
}
