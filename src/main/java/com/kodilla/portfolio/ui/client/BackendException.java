package com.kodilla.portfolio.ui.client;

/** Raised when the backend rejects a call or cannot be reached. */
public class BackendException extends RuntimeException {

    public BackendException(String message) {
        super(message);
    }

    public BackendException(String message, Throwable cause) {
        super(message, cause);
    }
}
