package to.renamed.sdk;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Result of a file rename operation.
 *
 * <p>Contains the AI-suggested filename and optional folder path for
 * organizing the file.</p>
 *
 * <p>Example:</p>
 * <pre>{@code
 * RenameResult result = client.rename(file, null);
 * System.out.println("Original: " + result.getOriginalFilename());
 * System.out.println("Suggested: " + result.getSuggestedFilename());
 * System.out.println("Confidence: " + result.getConfidence());
 * }</pre>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RenameResult {

    @JsonProperty("originalFilename")
    private String originalFilename;

    @JsonProperty("suggestedFilename")
    private String suggestedFilename;

    @JsonProperty("folderPath")
    private String folderPath;

    @JsonProperty("confidence")
    private double confidence;

    /**
     * Default constructor for Jackson deserialization.
     */
    public RenameResult() {
    }

    /**
     * Returns the original filename that was uploaded.
     *
     * @return the original filename
     */
    public String getOriginalFilename() {
        return originalFilename;
    }

    /**
     * Sets the original filename.
     *
     * @param originalFilename the original filename
     */
    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    /**
     * Returns the AI-suggested new filename.
     *
     * @return the suggested filename
     */
    public String getSuggestedFilename() {
        return suggestedFilename;
    }

    /**
     * Sets the suggested filename.
     *
     * @param suggestedFilename the suggested filename
     */
    public void setSuggestedFilename(String suggestedFilename) {
        this.suggestedFilename = suggestedFilename;
    }

    /**
     * Returns the suggested folder path for organization.
     *
     * @return the folder path, or null if not suggested
     */
    public String getFolderPath() {
        return folderPath;
    }

    /**
     * Sets the folder path.
     *
     * @param folderPath the folder path
     */
    public void setFolderPath(String folderPath) {
        this.folderPath = folderPath;
    }

    /**
     * Returns the confidence score for the suggestion.
     *
     * @return the confidence score between 0 and 1
     */
    public double getConfidence() {
        return confidence;
    }

    /**
     * Sets the confidence score.
     *
     * @param confidence the confidence score
     */
    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    @Override
    public String toString() {
        return "RenameResult{" +
                "originalFilename='" + originalFilename + '\'' +
                ", suggestedFilename='" + suggestedFilename + '\'' +
                ", folderPath='" + folderPath + '\'' +
                ", confidence=" + confidence +
                '}';
    }
}
