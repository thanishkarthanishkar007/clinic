package com.devsclinic.backend.exception;

/**
 * Thrown when an appointment (or other resource) cannot be found by its id.
 * Mapped to HTTP 404 by the GlobalExceptionHandler.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
