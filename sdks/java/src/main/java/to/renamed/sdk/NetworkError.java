package to.renamed.sdk;

/**
 * Exception thrown when a network connection failure occurs.
 *
 * <p>This error indicates that the request could not be completed due to
 * network issues such as:</p>
 * <ul>
 *   <li>Connection refused</li>
 *   <li>DNS resolution failure</li>
 *   <li>Connection timeout</li>
 *   <li>Network unreachable</li>
 * </ul>
 *
 * <p>Example:</p>
 * <pre>{@code
 * try {
 *     client.rename(file, options);
 * } catch (NetworkError e) {
 *     System.err.println("Network error: " + e.getMessage());
 *     // Implement retry logic
 * }
 * }</pre>
 */
public class NetworkError extends RenamedError {

    private static final String DEFAULT_MESSAGE = "Network request failed";
    private static final String ERROR_CODE = "NETWORK_ERROR";

    /**
     * Creates a new NetworkError with the default message.
     */
    public NetworkError() {
        this(DEFAULT_MESSAGE);
    }

    /**
     * Creates a new NetworkError with a custom message.
     *
     * @param message the error message
     */
    public NetworkError(String message) {
        super(message != null && !message.isEmpty() ? message : DEFAULT_MESSAGE, ERROR_CODE);
    }

    /**
     * Creates a new NetworkError with a message and cause.
     *
     * @param message the error message
     * @param cause the underlying exception
     */
    public NetworkError(String message, Throwable cause) {
        super(message != null && !message.isEmpty() ? message : DEFAULT_MESSAGE, ERROR_CODE, cause);
    }
}
