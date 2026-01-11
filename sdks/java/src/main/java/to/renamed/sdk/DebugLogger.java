package to.renamed.sdk;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Default logger implementation that writes to System.out.
 *
 * <p>Used when debug mode is enabled but no custom logger is provided.</p>
 */
final class DebugLogger implements Logger {

    private static final String PREFIX = "[Renamed]";
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    @Override
    public void log(String message) {
        String timestamp = LocalDateTime.now().format(TIME_FORMAT);
        System.out.println(timestamp + " " + PREFIX + " " + message);
    }

    /**
     * Masks an API key for safe logging.
     * Shows first 3 characters and last 4 characters only.
     *
     * @param apiKey the API key to mask
     * @return the masked API key (e.g., "rt_...xxxx")
     */
    static String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() < 8) {
            return "***";
        }
        String prefix = apiKey.substring(0, 3);
        String suffix = apiKey.substring(apiKey.length() - 4);
        return prefix + "..." + suffix;
    }

    /**
     * Formats a file size in human-readable format.
     *
     * @param bytes the size in bytes
     * @return formatted size string (e.g., "1.2 MB")
     */
    static String formatSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        }
        if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        }
        return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }
}
