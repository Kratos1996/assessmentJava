package com.developer.test.exception;

/**
 * Signals that a requested user, task, or resource could not be found.
 * Services throw this when the API asks for something that does not exist.
 * The global handler maps it to a 404 response for the client.
 */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
