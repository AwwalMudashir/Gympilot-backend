package com.project.gympilot_backend.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        String message,
        T data,
        List<ApiError> errors,
        Instant timestamp,
        String path
) {
    public static <T> ApiResponse<T> success(String message, T data, String path) {
        return new ApiResponse<>(true, message, data, null, Instant.now(), path);
    }

    public static ApiResponse<Void> error(String message, List<ApiError> errors, String path) {
        return new ApiResponse<>(false, message, null, errors, Instant.now(), path);
    }
}
