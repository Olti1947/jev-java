package io.github.Olti1947.jev.exception;

/**
 * Thrown when local client-side input validation fails before sending an API request.
 */
public class JevValidationException extends JevException {
    public JevValidationException(String message) {
        super(message);
    }
}