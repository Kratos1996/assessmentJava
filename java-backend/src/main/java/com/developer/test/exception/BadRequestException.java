package com.developer.test.exception;

/**
 * Signals that the client sent input that does not pass validation.
 * Services throw this when a request is structurally valid but the values are wrong.
 * The global handler turns it into a 400 response with a clear message.
 */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
