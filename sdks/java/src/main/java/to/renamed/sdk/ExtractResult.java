package to.renamed.sdk;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * Result of an extract operation.
 *
 * <p>Contains the extracted data matching the requested prompt or schema.</p>
 *
 * <p>Example:</p>
 * <pre>{@code
 * ExtractResult result = client.extract(file, options);
 * Map<String, Object> data = result.getData();
 *
 * String invoiceNumber = (String) data.get("invoiceNumber");
 * Double total = (Double) data.get("total");
 *
 * System.out.println("Invoice: " + invoiceNumber);
 * System.out.println("Total: $" + total);
 * System.out.println("Confidence: " + result.getConfidence());
 * }</pre>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExtractResult {

    @JsonProperty("data")
    private Map<String, Object> data;

    @JsonProperty("confidence")
    private double confidence;

    /**
     * Default constructor for Jackson deserialization.
     */
    public ExtractResult() {
    }

    /**
     * Returns the extracted data.
     *
     * @return a map containing the extracted key-value pairs
     */
    public Map<String, Object> getData() {
        return data;
    }

    /**
     * Sets the extracted data.
     *
     * @param data the extracted data map
     */
    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    /**
     * Returns the confidence score for the extraction.
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

    /**
     * Gets a value from the extracted data with type casting.
     *
     * @param <T> the expected type
     * @param key the data key
     * @param type the expected class type
     * @return the value cast to the specified type, or null if not found
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        if (data == null) {
            return null;
        }
        Object value = data.get(key);
        if (value == null) {
            return null;
        }
        if (type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }

    /**
     * Gets a string value from the extracted data.
     *
     * @param key the data key
     * @return the string value, or null if not found
     */
    public String getString(String key) {
        return get(key, String.class);
    }

    /**
     * Gets an integer value from the extracted data.
     *
     * @param key the data key
     * @return the integer value, or null if not found
     */
    public Integer getInt(String key) {
        Object value = data != null ? data.get(key) : null;
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return null;
    }

    /**
     * Gets a double value from the extracted data.
     *
     * @param key the data key
     * @return the double value, or null if not found
     */
    public Double getDouble(String key) {
        Object value = data != null ? data.get(key) : null;
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return null;
    }

    @Override
    public String toString() {
        return "ExtractResult{" +
                "data=" + data +
                ", confidence=" + confidence +
                '}';
    }
}
