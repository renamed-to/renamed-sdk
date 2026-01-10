package to.renamed.sdk;

import java.util.Map;

/**
 * Base exception class for all renamed.to SDK errors.
 *
 * <p>All SDK exceptions extend this class, providing a consistent error handling
 * interface with error codes, HTTP status codes, and additional details.</p>
 */
public class RenamedError extends RuntimeException {

    private final String code;
    private final int statusCode;
    private final Map<String, Object> details;

    /**
     * Creates a new RenamedError with the specified message and code.
     *
     * @param message the error message
     * @param code the error code (e.g., "API_ERROR", "NETWORK_ERROR")
     */
    public RenamedError(String message, String code) {
        this(message, code, 0, null);
    }

    /**
     * Creates a new RenamedError with all properties.
     *
     * @param message the error message
     * @param code the error code
     * @param statusCode the HTTP status code (0 if not applicable)
     * @param details additional error details
     */
    public RenamedError(String message, String code, int statusCode, Map<String, Object> details) {
        super(message);
        this.code = code;
        this.statusCode = statusCode;
        this.details = details;
    }

    /**
     * Creates a new RenamedError with a cause.
     *
     * @param message the error message
     * @param code the error code
     * @param cause the underlying cause
     */
    public RenamedError(String message, String code, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.statusCode = 0;
        this.details = null;
    }

    /**
     * Returns the error code.
     *
     * @return the error code (e.g., "AUTHENTICATION_ERROR", "RATE_LIMIT_ERROR")
     */
    public String getCode() {
        return code;
    }

    /**
     * Returns the HTTP status code associated with this error.
     *
     * @return the HTTP status code, or 0 if not applicable
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * Returns additional error details.
     *
     * @return a map of additional details, or null if none available
     */
    public Map<String, Object> getDetails() {
        return details;
    }

    @Override
    public String toString() {
        if (statusCode > 0) {
            return String.format("%s (status %d): %s", code, statusCode, getMessage());
        }
        return String.format("%s: %s", code, getMessage());
    }

    /**
     * Creates an appropriate error from an HTTP status code.
     *
     * @param statusCode the HTTP status code
     * @param statusText the HTTP status text
     * @param payload the error response payload
     * @return the appropriate RenamedError subclass
     */
    public static RenamedError fromHttpStatus(int statusCode, String statusText, Map<String, Object> payload) {
        String message = statusText;
        if (payload != null && payload.containsKey("error")) {
            Object errorValue = payload.get("error");
            if (errorValue instanceof String) {
                message = (String) errorValue;
            }
        }

        switch (statusCode) {
            case 401:
                return new AuthenticationError(message);
            case 402:
                return new InsufficientCreditsError(message);
            case 400:
            case 422:
                return new ValidationError(message, payload);
            case 429:
                int retryAfter = 0;
                if (payload != null && payload.containsKey("retryAfter")) {
                    Object retryValue = payload.get("retryAfter");
                    if (retryValue instanceof Number) {
                        retryAfter = ((Number) retryValue).intValue();
                    }
                }
                return new RateLimitError(message, retryAfter);
            default:
                return new RenamedError(message, "API_ERROR", statusCode, payload);
        }
    }
}
