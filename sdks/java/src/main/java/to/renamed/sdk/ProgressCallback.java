package to.renamed.sdk;

/**
 * Callback interface for receiving progress updates during async job polling.
 *
 * <p>Example:</p>
 * <pre>{@code
 * PdfSplitResult result = job.await(status -> {
 *     System.out.println("Progress: " + status.getProgress() + "%");
 * });
 * }</pre>
 */
@FunctionalInterface
public interface ProgressCallback {

    /**
     * Called with each status update during polling.
     *
     * @param status the current job status
     */
    void onProgress(JobStatusResponse status);
}
