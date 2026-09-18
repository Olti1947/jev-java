package io.github.Olti1947.jev.exception;

/**
 * Thrown when JSON serialization or deserialization fails.
 */
public class JevSerializationException extends JevException {
    public JevSerializationException(String message, Throwable cause) {
        super(message, cause);
    }
}