package to.renamed.sdk;

/**
 * Specifies the mode for PDF splitting.
 */
public enum SplitMode {

    /**
     * Uses AI to detect document boundaries automatically.
     */
    AUTO("auto"),

    /**
     * Splits every N pages (specify with pagesPerSplit option).
     */
    PAGES("pages"),

    /**
     * Splits at blank pages.
     */
    BLANK("blank");

    private final String value;

    SplitMode(String value) {
        this.value = value;
    }

    /**
     * Returns the API value for this mode.
     *
     * @return the string value used in API requests
     */
    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}
