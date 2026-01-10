package to.renamed.sdk;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response from the job status endpoint.
 *
 * <p>Contains the current status of an async job, including progress
 * and results when completed.</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class JobStatusResponse {

    @JsonProperty("jobId")
    private String jobId;

    @JsonProperty("status")
    private String status;

    @JsonProperty("progress")
    private int progress;

    @JsonProperty("error")
    private String error;

    @JsonProperty("result")
    private PdfSplitResult result;

    /**
     * Default constructor for Jackson deserialization.
     */
    public JobStatusResponse() {
    }

    /**
     * Returns the unique job identifier.
     *
     * @return the job ID
     */
    public String getJobId() {
        return jobId;
    }

    /**
     * Sets the job ID.
     *
     * @param jobId the job ID
     */
    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    /**
     * Returns the current job status as a string.
     *
     * @return the status string
     */
    public String getStatusValue() {
        return status;
    }

    /**
     * Returns the current job status as an enum.
     *
     * @return the JobStatus enum value
     */
    public JobStatus getStatus() {
        return JobStatus.fromValue(status);
    }

    /**
     * Sets the status.
     *
     * @param status the status string
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Returns the progress percentage (0-100).
     *
     * @return the progress percentage
     */
    public int getProgress() {
        return progress;
    }

    /**
     * Sets the progress.
     *
     * @param progress the progress percentage
     */
    public void setProgress(int progress) {
        this.progress = progress;
    }

    /**
     * Returns the error message if the job failed.
     *
     * @return the error message, or null if no error
     */
    public String getError() {
        return error;
    }

    /**
     * Sets the error message.
     *
     * @param error the error message
     */
    public void setError(String error) {
        this.error = error;
    }

    /**
     * Returns the result when the job is completed.
     *
     * @return the PDF split result, or null if not yet completed
     */
    public PdfSplitResult getResult() {
        return result;
    }

    /**
     * Sets the result.
     *
     * @param result the PDF split result
     */
    public void setResult(PdfSplitResult result) {
        this.result = result;
    }

    /**
     * Checks if the job has completed (successfully or with failure).
     *
     * @return true if the job is no longer running
     */
    public boolean isComplete() {
        JobStatus jobStatus = getStatus();
        return jobStatus == JobStatus.COMPLETED || jobStatus == JobStatus.FAILED;
    }

    /**
     * Checks if the job completed successfully.
     *
     * @return true if the job completed without errors
     */
    public boolean isSuccessful() {
        return getStatus() == JobStatus.COMPLETED && result != null;
    }

    @Override
    public String toString() {
        return "JobStatusResponse{" +
                "jobId='" + jobId + '\'' +
                ", status='" + status + '\'' +
                ", progress=" + progress +
                ", error='" + error + '\'' +
                ", result=" + result +
                '}';
    }
}
