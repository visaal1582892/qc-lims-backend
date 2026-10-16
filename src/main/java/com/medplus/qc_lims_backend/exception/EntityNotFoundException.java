package com.medplus.qc_lims_backend.exception;

/**
 * Thrown when a requested entity is not found in the database.
 * Handled by GlobalExceptionHandler to return 404.
 */
public class EntityNotFoundException extends RuntimeException {

    public EntityNotFoundException(String message) {
        super(message);
    }

    public EntityNotFoundException(String entityName, Object id) {
        super(String.format("%s not found with id: %s", entityName, id));
    }
}
