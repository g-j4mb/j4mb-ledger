package com.j4mb.ledger.shared.api;

import java.time.Instant;
import java.util.List;

public record ApiResponse<T>(
        boolean success,
        T data,
        ApiError error,
        Instant timestamp,
        String traceId
) {
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null, Instant.now(), null);
    }

    public static <T> ApiResponse<T> error(String code, String message) {
        return new ApiResponse<>(false, null, new ApiError(code, message, List.of()), Instant.now(), null);
    }

    public static <T> ApiResponse<T> validationError(List<FieldError> details) {
        return new ApiResponse<>(false, null,
                new ApiError("VALIDATION_ERROR", "Validation failed", details), Instant.now(), null);
    }

    public record ApiError(String code, String message, List<FieldError> details) {}
    public record FieldError(String field, String message) {}
}
