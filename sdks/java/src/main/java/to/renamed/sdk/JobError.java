package to.renamed.sdk;

/**
 * Exception thrown when an async job fails.
 *
 * <p>This error occurs when an asynchronous job (such as PDF splitting)
 * fails during processing. The error includes the job ID for debugging.</p>
 *
 * <p>Example:</p>
 * <pre>{@code
 * try {
 *     PdfSplitResult result = job.await();
 * } catch (JobError e) {
 *     System.err.println("Job " + e.getJobId() + " failed: " + e.getMessage());
 * }
 * }</pre>
 */
public class JobError extends RenamedError {

    private static final String ERROR_CODE = "JOB_ERROR";

    private final String jobId;

    /**
     * Creates a new JobError with a message.
     *
     * @param message the error message
     */
    public JobError(String message) {
        this(message, null);
    }

    /**
     * Creates a new JobError with a message and job ID.
     *
     * @param message the error message
     * @param jobId the ID of the failed job
     */
    public JobError(String message, String jobId) {
        super(message, ERROR_CODE);
        this.jobId = jobId;
    }

    /**
     * Returns the ID of the failed job.
     *
     * @return the job ID, or null if not available
     */
    public String getJobId() {
        return jobId;
    }
}
