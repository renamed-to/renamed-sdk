package to.renamed.sdk;

/**
 * Logger interface for the Renamed SDK.
 *
 * <p>This interface allows users to integrate the SDK with their preferred
 * logging framework (SLF4J, Log4j, java.util.logging, etc.).</p>
 *
 * <p>Example with SLF4J:</p>
 * <pre>{@code
 * import org.slf4j.LoggerFactory;
 *
 * RenamedClient client = RenamedClient.builder("rt_your_api_key")
 *     .debug(true)
 *     .logger(LoggerFactory.getLogger(RenamedClient.class)::info)
 *     .build();
 * }</pre>
 *
 * <p>Example with custom logger:</p>
 * <pre>{@code
 * RenamedClient client = RenamedClient.builder("rt_your_api_key")
 *     .debug(true)
 *     .logger(message -> System.out.println("[MyApp] " + message))
 *     .build();
 * }</pre>
 */
@FunctionalInterface
public interface Logger {

    /**
     * Logs a debug message.
     *
     * @param message the message to log
     */
    void log(String message);
}
