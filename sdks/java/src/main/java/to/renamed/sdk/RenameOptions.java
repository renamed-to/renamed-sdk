package to.renamed.sdk;

/**
 * Options for the rename operation.
 *
 * <p>Example:</p>
 * <pre>{@code
 * RenameOptions options = new RenameOptions()
 *     .withTemplate("{date}_{type}_{description}");
 *
 * RenameResult result = client.rename(file, options);
 * }</pre>
 */
public class RenameOptions {

    private String template;

    /**
     * Creates new RenameOptions with default settings.
     */
    public RenameOptions() {
    }

    /**
     * Returns the custom template for filename generation.
     *
     * @return the template string, or null if not set
     */
    public String getTemplate() {
        return template;
    }

    /**
     * Sets a custom template for filename generation.
     *
     * <p>Templates can include placeholders like {date}, {type}, {description}
     * that will be filled by the AI based on document content.</p>
     *
     * @param template the template string
     * @return this options instance for method chaining
     */
    public RenameOptions withTemplate(String template) {
        this.template = template;
        return this;
    }

    /**
     * Sets the template.
     *
     * @param template the template string
     */
    public void setTemplate(String template) {
        this.template = template;
    }
}
