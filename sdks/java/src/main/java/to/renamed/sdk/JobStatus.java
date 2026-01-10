package to.renamed.sdk;

/**
 * Status of an asynchronous job.
 */
public enum JobStatus {

    /**
     * Job has been created but not yet started.
     */
    PENDING("pending"),

    /**
     * Job is currently being processed.
     */
    PROCESSING("processing"),

    /**
     * Job completed successfully.
     */
    COMPLETED("completed"),

    /**
     * Job failed with an error.
     */
    FAILED("failed");

    private final String value;

    JobStatus(String value) {
        this.value = value;
    }

    /**
     * Returns the API value for this status.
     *
     * @return the string value used in API responses
     */
    public String getValue() {
        return value;
    }

    /**
     * Parses a string value into a JobStatus.
     *
     * @param value the string value from the API
     * @return the corresponding JobStatus, or null if not recognized
     */
    public static JobStatus fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (JobStatus status : values()) {
            if (status.value.equalsIgnoreCase(value)) {
                return status;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return value;
    }
}
