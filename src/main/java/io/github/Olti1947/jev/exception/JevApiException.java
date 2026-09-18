package io.github.Olti1947.jev.exception;

/**
 * Thrown when the Jev REST API returns a non-200 HTTP status code.
 */
public class JevApiException extends JevException {
    private final int statusCode;
    private final String responseBody;

    public JevApiException(int statusCode, String responseBody) {
        super("Jev API Error [" + statusCode + "]: " + responseBody);
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getResponseBody() {
        return responseBody;
    }
}