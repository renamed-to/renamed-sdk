package to.renamed.sdk;

/**
 * Exception thrown when API authentication fails.
 *
 * <p>This error occurs when:</p>
 * <ul>
 *   <li>The API key is missing or empty</li>
 *   <li>The API key is invalid or expired</li>
 *   <li>The API key does not have permission for the requested operation</li>
 * </ul>
 *
 * <p>Example:</p>
 * <pre>{@code
 * try {
 *     client.getUser();
 * } catch (AuthenticationError e) {
 *     System.err.println("Invalid API key: " + e.getMessage());
 * }
 * }</pre>
 */
public class AuthenticationError extends RenamedError {

    private static final String DEFAULT_MESSAGE = "Invalid or missing API key";
    private static final String ERROR_CODE = "AUTHENTICATION_ERROR";
    private static final int STATUS_CODE = 401;

    /**
     * Creates a new AuthenticationError with the default message.
     */
    public AuthenticationError() {
        this(DEFAULT_MESSAGE);
    }

    /**
     * Creates a new AuthenticationError with a custom message.
     *
     * @param message the error message
     */
    public AuthenticationError(String message) {
        super(message != null && !message.isEmpty() ? message : DEFAULT_MESSAGE,
              ERROR_CODE, STATUS_CODE, null);
    }
}
