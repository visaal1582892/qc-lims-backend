package com.medplus.qc_lims_backend.dto.response;

import java.time.Instant;

/**
 * Standardized API envelope per architecture Pattern B.
 * All successful responses should use ApiResponse.ok(data).
 */
public record ApiResponse<T>(
    boolean success,
    String message,
    T data,
    Instant timestamp
) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, "Success", data, Instant.now());
    }

    public static <T> ApiResponse<T> error(String message, T data) {
        return new ApiResponse<>(false, message, data, Instant.now());
    }
}
