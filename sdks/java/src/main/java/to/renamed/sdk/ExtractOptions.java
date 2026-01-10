package to.renamed.sdk;

import java.util.Map;

/**
 * Options for the extract operation.
 *
 * <p>You can specify either a prompt (natural language) or a schema (structured)
 * to define what data to extract from the document.</p>
 *
 * <p>Example with prompt:</p>
 * <pre>{@code
 * ExtractOptions options = new ExtractOptions()
 *     .withPrompt("Extract the invoice number, date, and total amount");
 *
 * ExtractResult result = client.extract(file, options);
 * }</pre>
 *
 * <p>Example with schema:</p>
 * <pre>{@code
 * Map<String, Object> schema = Map.of(
 *     "invoiceNumber", Map.of("type", "string"),
 *     "date", Map.of("type", "string", "format", "date"),
 *     "total", Map.of("type", "number")
 * );
 *
 * ExtractOptions options = new ExtractOptions()
 *     .withSchema(schema);
 *
 * ExtractResult result = client.extract(file, options);
 * }</pre>
 */
public class ExtractOptions {

    private String prompt;
    private Map<String, Object> schema;

    /**
     * Creates new ExtractOptions with default settings.
     */
    public ExtractOptions() {
    }

    /**
     * Returns the extraction prompt.
     *
     * @return the prompt, or null if not set
     */
    public String getPrompt() {
        return prompt;
    }

    /**
     * Sets a natural language prompt describing what to extract.
     *
     * @param prompt the extraction prompt
     * @return this options instance for method chaining
     */
    public ExtractOptions withPrompt(String prompt) {
        this.prompt = prompt;
        return this;
    }

    /**
     * Sets the prompt.
     *
     * @param prompt the prompt
     */
    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    /**
     * Returns the extraction schema.
     *
     * @return the schema, or null if not set
     */
    public Map<String, Object> getSchema() {
        return schema;
    }

    /**
     * Sets a JSON schema defining the structure of data to extract.
     *
     * @param schema the extraction schema
     * @return this options instance for method chaining
     */
    public ExtractOptions withSchema(Map<String, Object> schema) {
        this.schema = schema;
        return this;
    }

    /**
     * Sets the schema.
     *
     * @param schema the schema
     */
    public void setSchema(Map<String, Object> schema) {
        this.schema = schema;
    }
}
