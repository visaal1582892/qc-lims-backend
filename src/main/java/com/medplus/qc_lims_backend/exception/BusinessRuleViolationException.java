package com.medplus.qc_lims_backend.exception;

/**
 * Thrown when a business rule validation fails (e.g., minLimit >= maxLimit).
 * Handled by GlobalExceptionHandler to return 400 Bad Request.
 */
public class BusinessRuleViolationException extends RuntimeException {

    public BusinessRuleViolationException(String message) {
        super(message);
    }
}
