package com.medplus.qc_lims_backend.exception;

/**
 * Thrown when attempting to create a resource that already exists.
 * Handled by GlobalExceptionHandler to return 409.
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
