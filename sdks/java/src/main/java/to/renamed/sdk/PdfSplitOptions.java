package to.renamed.sdk;

/**
 * Options for the PDF split operation.
 *
 * <p>Example:</p>
 * <pre>{@code
 * // Split using AI to detect document boundaries
 * PdfSplitOptions options = new PdfSplitOptions()
 *     .withMode(SplitMode.AUTO);
 *
 * // Split every 5 pages
 * PdfSplitOptions options = new PdfSplitOptions()
 *     .withMode(SplitMode.PAGES)
 *     .withPagesPerSplit(5);
 * }</pre>
 */
public class PdfSplitOptions {

    private SplitMode mode;
    private int pagesPerSplit;

    /**
     * Creates new PdfSplitOptions with default settings.
     */
    public PdfSplitOptions() {
    }

    /**
     * Returns the split mode.
     *
     * @return the split mode, or null if not set
     */
    public SplitMode getMode() {
        return mode;
    }

    /**
     * Sets the split mode.
     *
     * @param mode the split mode (auto, pages, or blank)
     * @return this options instance for method chaining
     */
    public PdfSplitOptions withMode(SplitMode mode) {
        this.mode = mode;
        return this;
    }

    /**
     * Sets the split mode.
     *
     * @param mode the split mode
     */
    public void setMode(SplitMode mode) {
        this.mode = mode;
    }

    /**
     * Returns the number of pages per split.
     *
     * @return pages per split, or 0 if not set
     */
    public int getPagesPerSplit() {
        return pagesPerSplit;
    }

    /**
     * Sets the number of pages per split (for PAGES mode).
     *
     * @param pagesPerSplit number of pages in each split document
     * @return this options instance for method chaining
     */
    public PdfSplitOptions withPagesPerSplit(int pagesPerSplit) {
        this.pagesPerSplit = pagesPerSplit;
        return this;
    }

    /**
     * Sets the pages per split.
     *
     * @param pagesPerSplit number of pages per split
     */
    public void setPagesPerSplit(int pagesPerSplit) {
        this.pagesPerSplit = pagesPerSplit;
    }
}
