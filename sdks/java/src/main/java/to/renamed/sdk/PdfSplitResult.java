package to.renamed.sdk;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Result of a PDF split operation.
 *
 * <p>Contains the list of split documents that can be downloaded individually.</p>
 *
 * <p>Example:</p>
 * <pre>{@code
 * AsyncJob job = client.pdfSplit(file, options);
 * PdfSplitResult result = job.await();
 *
 * System.out.println("Original: " + result.getOriginalFilename());
 * System.out.println("Total pages: " + result.getTotalPages());
 * System.out.println("Split into " + result.getDocuments().size() + " documents");
 * }</pre>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PdfSplitResult {

    @JsonProperty("originalFilename")
    private String originalFilename;

    @JsonProperty("documents")
    private List<SplitDocument> documents;

    @JsonProperty("totalPages")
    private int totalPages;

    /**
     * Default constructor for Jackson deserialization.
     */
    public PdfSplitResult() {
    }

    /**
     * Returns the original filename.
     *
     * @return the original uploaded filename
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
     * Returns the list of split documents.
     *
     * @return the split documents
     */
    public List<SplitDocument> getDocuments() {
        return documents;
    }

    /**
     * Sets the documents list.
     *
     * @param documents the split documents
     */
    public void setDocuments(List<SplitDocument> documents) {
        this.documents = documents;
    }

    /**
     * Returns the total number of pages in the original document.
     *
     * @return the total page count
     */
    public int getTotalPages() {
        return totalPages;
    }

    /**
     * Sets the total pages.
     *
     * @param totalPages the total page count
     */
    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    @Override
    public String toString() {
        return "PdfSplitResult{" +
                "originalFilename='" + originalFilename + '\'' +
                ", documents=" + documents +
                ", totalPages=" + totalPages +
                '}';
    }
}
