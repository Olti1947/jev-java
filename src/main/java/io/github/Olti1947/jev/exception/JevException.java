package io.github.Olti1947.jev.exception;

public class JevException extends RuntimeException {

    public JevException(String message) {
        super(message);
    }

    public JevException(String message, Throwable cause) {
        super(message, cause);
    }

    public JevException(Throwable cause) {
        super(cause);
    }
}