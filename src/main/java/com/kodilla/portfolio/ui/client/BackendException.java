package com.kodilla.portfolio.ui.client;

/** Raised when the backend rejects a call or cannot be reached. */
public class BackendException extends RuntimeException {

    private final int status;

    public BackendException(int status, String message) {
        super(message);
        this.status = status;
    }

    public BackendException(String message, Throwable cause) {
        super(message, cause);
        this.status = 0;
    }

    public int getStatus() {
        return status;
    }

    /** True when the backend could not be reached at all. */
    public boolean isConnectionFailure() {
        return status == 0;
    }
}
