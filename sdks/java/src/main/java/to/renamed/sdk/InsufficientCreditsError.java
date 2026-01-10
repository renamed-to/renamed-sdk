package to.renamed.sdk;

/**
 * Exception thrown when the account has insufficient credits.
 *
 * <p>This error occurs when the account does not have enough credits
 * to complete the requested operation. Credits can be purchased through
 * the renamed.to dashboard.</p>
 *
 * <p>Example:</p>
 * <pre>{@code
 * try {
 *     client.rename(file, options);
 * } catch (InsufficientCreditsError e) {
 *     System.err.println("Please purchase more credits: " + e.getMessage());
 * }
 * }</pre>
 */
public class InsufficientCreditsError extends RenamedError {

    private static final String DEFAULT_MESSAGE = "Insufficient credits";
    private static final String ERROR_CODE = "INSUFFICIENT_CREDITS";
    private static final int STATUS_CODE = 402;

    /**
     * Creates a new InsufficientCreditsError with the default message.
     */
    public InsufficientCreditsError() {
        this(DEFAULT_MESSAGE);
    }

    /**
     * Creates a new InsufficientCreditsError with a custom message.
     *
     * @param message the error message
     */
    public InsufficientCreditsError(String message) {
        super(message != null && !message.isEmpty() ? message : DEFAULT_MESSAGE,
              ERROR_CODE, STATUS_CODE, null);
    }
}
