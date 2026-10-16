package com.medplus.qc_lims_backend.exception;

import java.util.Map;

/**
 * Error response DTO used by GlobalExceptionHandler.
 */
public record ApiErrorResponse(
    String code,
    String message,
    Map<String, String> details
) {
    public ApiErrorResponse(String code, String message) {
        this(code, message, null);
    }
}
