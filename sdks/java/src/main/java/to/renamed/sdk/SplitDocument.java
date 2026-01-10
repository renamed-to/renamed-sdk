package to.renamed.sdk;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a single document from a PDF split operation.
 *
 * <p>Each split document includes a download URL that can be used to
 * retrieve the PDF content.</p>
 *
 * <p>Example:</p>
 * <pre>{@code
 * PdfSplitResult result = job.await();
 * for (SplitDocument doc : result.getDocuments()) {
 *     System.out.println("Document " + doc.getIndex() + ": " + doc.getFilename());
 *     System.out.println("Pages: " + doc.getPages());
 *     byte[] content = client.downloadFile(doc.getDownloadUrl());
 *     Files.write(Paths.get(doc.getFilename()), content);
 * }
 * }</pre>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SplitDocument {

    @JsonProperty("index")
    private int index;

    @JsonProperty("filename")
    private String filename;

    @JsonProperty("pages")
    private String pages;

    @JsonProperty("downloadUrl")
    private String downloadUrl;

    @JsonProperty("size")
    private long size;

    /**
     * Default constructor for Jackson deserialization.
     */
    public SplitDocument() {
    }

    /**
     * Returns the document index (0-based).
     *
     * @return the index of this document in the split results
     */
    public int getIndex() {
        return index;
    }

    /**
     * Sets the document index.
     *
     * @param index the document index
     */
    public void setIndex(int index) {
        this.index = index;
    }

    /**
     * Returns the suggested filename for this document.
     *
     * @return the AI-suggested filename
     */
    public String getFilename() {
        return filename;
    }

    /**
     * Sets the filename.
     *
     * @param filename the filename
     */
    public void setFilename(String filename) {
        this.filename = filename;
    }

    /**
     * Returns the page range included in this document.
     *
     * @return the page range (e.g., "1-5", "6-10")
     */
    public String getPages() {
        return pages;
    }

    /**
     * Sets the page range.
     *
     * @param pages the page range string
     */
    public void setPages(String pages) {
        this.pages = pages;
    }

    /**
     * Returns the URL to download this document.
     *
     * @return the download URL
     */
    public String getDownloadUrl() {
        return downloadUrl;
    }

    /**
     * Sets the download URL.
     *
     * @param downloadUrl the download URL
     */
    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    /**
     * Returns the size of this document in bytes.
     *
     * @return the file size in bytes
     */
    public long getSize() {
        return size;
    }

    /**
     * Sets the document size.
     *
     * @param size the size in bytes
     */
    public void setSize(long size) {
        this.size = size;
    }

    @Override
    public String toString() {
        return "SplitDocument{" +
                "index=" + index +
                ", filename='" + filename + '\'' +
                ", pages='" + pages + '\'' +
                ", downloadUrl='" + downloadUrl + '\'' +
                ", size=" + size +
                '}';
    }
}
