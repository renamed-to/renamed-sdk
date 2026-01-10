package to.renamed.sdk;

/**
 * Exception thrown when the API rate limit has been exceeded.
 *
 * <p>This error includes a {@code retryAfter} value indicating the number
 * of seconds to wait before retrying the request.</p>
 *
 * <p>Example:</p>
 * <pre>{@code
 * try {
 *     client.rename(file, options);
 * } catch (RateLimitError e) {
 *     int waitTime = e.getRetryAfter();
 *     System.out.println("Rate limited. Retry after " + waitTime + " seconds");
 *     Thread.sleep(waitTime * 1000L);
 *     // Retry the request
 * }
 * }</pre>
 */
public class RateLimitError extends RenamedError {

    private static final String DEFAULT_MESSAGE = "Rate limit exceeded";
    private static final String ERROR_CODE = "RATE_LIMIT_ERROR";
    private static final int STATUS_CODE = 429;

    private final int retryAfter;

    /**
     * Creates a new RateLimitError with the default message.
     */
    public RateLimitError() {
        this(DEFAULT_MESSAGE, 0);
    }

    /**
     * Creates a new RateLimitError with a custom message and retry time.
     *
     * @param message the error message
     * @param retryAfter seconds until the rate limit resets
     */
    public RateLimitError(String message, int retryAfter) {
        super(message != null && !message.isEmpty() ? message : DEFAULT_MESSAGE,
              ERROR_CODE, STATUS_CODE, null);
        this.retryAfter = retryAfter;
    }

    /**
     * Returns the number of seconds to wait before retrying.
     *
     * @return seconds until the rate limit resets, or 0 if unknown
     */
    public int getRetryAfter() {
        return retryAfter;
    }
}
