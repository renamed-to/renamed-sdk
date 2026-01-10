package to.renamed.sdk;

import java.util.Map;

/**
 * Exception thrown when request validation fails.
 *
 * <p>This error occurs when:</p>
 * <ul>
 *   <li>Required parameters are missing</li>
 *   <li>Parameter values are invalid or out of range</li>
 *   <li>The uploaded file format is not supported</li>
 *   <li>The file exceeds size limits</li>
 * </ul>
 *
 * <p>Example:</p>
 * <pre>{@code
 * try {
 *     client.rename(file, options);
 * } catch (ValidationError e) {
 *     System.err.println("Invalid request: " + e.getMessage());
 *     Map<String, Object> details = e.getDetails();
 *     if (details != null) {
 *         System.err.println("Details: " + details);
 *     }
 * }
 * }</pre>
 */
public class ValidationError extends RenamedError {

    private static final String ERROR_CODE = "VALIDATION_ERROR";
    private static final int STATUS_CODE = 400;

    /**
     * Creates a new ValidationError with a message.
     *
     * @param message the error message
     */
    public ValidationError(String message) {
        this(message, null);
    }

    /**
     * Creates a new ValidationError with a message and additional details.
     *
     * @param message the error message
     * @param details additional validation error details
     */
    public ValidationError(String message, Map<String, Object> details) {
        super(message, ERROR_CODE, STATUS_CODE, details);
    }
}
