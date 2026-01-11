package to.renamed.sdk;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Client for the renamed.to API.
 *
 * <p>The RenamedClient provides access to all renamed.to API operations including
 * file renaming, PDF splitting, and data extraction.</p>
 *
 * <p>Basic usage:</p>
 * <pre>{@code
 * RenamedClient client = new RenamedClient("rt_your_api_key");
 *
 * // Rename a file
 * RenameResult result = client.rename(Path.of("invoice.pdf"), null);
 * System.out.println("Suggested: " + result.getSuggestedFilename());
 *
 * // Get user info
 * User user = client.getUser();
 * System.out.println("Credits: " + user.getCredits());
 * }</pre>
 *
 * <p>With custom configuration:</p>
 * <pre>{@code
 * RenamedClient client = RenamedClient.builder("rt_your_api_key")
 *     .baseUrl("https://custom-api.example.com")
 *     .timeout(Duration.ofSeconds(60))
 *     .maxRetries(3)
 *     .build();
 * }</pre>
 */
public class RenamedClient {

    private static final String DEFAULT_BASE_URL = "https://www.renamed.to/api/v1";
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);
    private static final int DEFAULT_MAX_RETRIES = 2;
    private static final String CRLF = "\r\n";

    private final String apiKey;
    private final String baseUrl;
    private final Duration timeout;
    private final int maxRetries;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final boolean debug;
    private final Logger logger;

    /**
     * Creates a new RenamedClient with the specified API key.
     *
     * @param apiKey your renamed.to API key (starts with "rt_")
     */
    public RenamedClient(String apiKey) {
        this(apiKey, DEFAULT_BASE_URL, DEFAULT_TIMEOUT, DEFAULT_MAX_RETRIES, null, false, null);
    }

    /**
     * Creates a new RenamedClient with full configuration.
     */
    private RenamedClient(String apiKey, String baseUrl, Duration timeout, int maxRetries,
                          HttpClient httpClient, boolean debug, Logger logger) {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalArgumentException("API key is required");
        }
        this.apiKey = apiKey;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.timeout = timeout;
        this.maxRetries = maxRetries;
        this.httpClient = httpClient != null ? httpClient : HttpClient.newBuilder()
                .connectTimeout(timeout)
                .build();
        this.objectMapper = new ObjectMapper();
        this.debug = debug;
        this.logger = debug ? (logger != null ? logger : new DebugLogger()) : null;

        if (this.debug && this.logger != null) {
            this.logger.log("Initialized with API key " + DebugLogger.maskApiKey(apiKey));
        }
    }

    /**
     * Creates a new builder for configuring a RenamedClient.
     *
     * @param apiKey your renamed.to API key
     * @return a new builder instance
     */
    public static Builder builder(String apiKey) {
        return new Builder(apiKey);
    }

    /**
     * Returns the current user profile and credits.
     *
     * <p>Example:</p>
     * <pre>{@code
     * User user = client.getUser();
     * System.out.println("Email: " + user.getEmail());
     * System.out.println("Credits: " + user.getCredits());
     * }</pre>
     *
     * @return the user profile
     * @throws AuthenticationError if the API key is invalid
     * @throws RenamedError if the request fails
     */
    public User getUser() {
        String response = request("GET", "/user", null, null);
        return parseResponse(response, User.class);
    }

    /**
     * Renames a file using AI.
     *
     * <p>Example:</p>
     * <pre>{@code
     * RenameResult result = client.rename(Path.of("document.pdf"), null);
     * System.out.println("Original: " + result.getOriginalFilename());
     * System.out.println("Suggested: " + result.getSuggestedFilename());
     * }</pre>
     *
     * @param file the file to rename
     * @param options optional rename settings
     * @return the rename result with suggested filename
     * @throws ValidationError if the file format is not supported
     * @throws InsufficientCreditsError if the account has no credits
     * @throws RenamedError if the request fails
     */
    public RenameResult rename(Path file, RenameOptions options) {
        Map<String, String> fields = new HashMap<>();
        if (options != null && options.getTemplate() != null) {
            fields.put("template", options.getTemplate());
        }

        String response = uploadFile("/rename", file, fields);
        return parseResponse(response, RenameResult.class);
    }

    /**
     * Renames a file from a byte array.
     *
     * @param content the file content
     * @param filename the filename
     * @param options optional rename settings
     * @return the rename result with suggested filename
     */
    public RenameResult rename(byte[] content, String filename, RenameOptions options) {
        Map<String, String> fields = new HashMap<>();
        if (options != null && options.getTemplate() != null) {
            fields.put("template", options.getTemplate());
        }

        String response = uploadFile("/rename", content, filename, fields);
        return parseResponse(response, RenameResult.class);
    }

    /**
     * Renames a file from an InputStream.
     *
     * @param inputStream the input stream containing file content
     * @param filename the filename
     * @param options optional rename settings
     * @return the rename result with suggested filename
     * @throws RenamedError if the request fails
     */
    public RenameResult rename(InputStream inputStream, String filename, RenameOptions options) {
        try {
            byte[] content = inputStream.readAllBytes();
            return rename(content, filename, options);
        } catch (IOException e) {
            throw new NetworkError("Failed to read input stream: " + e.getMessage(), e);
        }
    }

    /**
     * Splits a PDF into multiple documents.
     *
     * <p>This operation is asynchronous. The returned {@link AsyncJob} can be
     * used to poll for completion and retrieve results.</p>
     *
     * <p>Example:</p>
     * <pre>{@code
     * PdfSplitOptions options = new PdfSplitOptions()
     *     .withMode(SplitMode.AUTO);
     *
     * AsyncJob job = client.pdfSplit(Path.of("multi-page.pdf"), options);
     *
     * // Wait for completion with progress updates
     * PdfSplitResult result = job.await(status -> {
     *     System.out.println("Progress: " + status.getProgress() + "%");
     * });
     *
     * for (SplitDocument doc : result.getDocuments()) {
     *     System.out.println(doc.getFilename());
     * }
     * }</pre>
     *
     * @param file the PDF file to split
     * @param options optional split settings
     * @return an async job that can be polled for results
     * @throws ValidationError if the file is not a valid PDF
     * @throws InsufficientCreditsError if the account has no credits
     * @throws RenamedError if the request fails
     */
    public AsyncJob pdfSplit(Path file, PdfSplitOptions options) {
        Map<String, String> fields = new HashMap<>();
        if (options != null) {
            if (options.getMode() != null) {
                fields.put("mode", options.getMode().getValue());
            }
            if (options.getPagesPerSplit() > 0) {
                fields.put("pagesPerSplit", String.valueOf(options.getPagesPerSplit()));
            }
        }

        String response = uploadFile("/pdf-split", file, fields);
        return createAsyncJob(response);
    }

    /**
     * Splits a PDF from a byte array.
     *
     * @param content the PDF file content
     * @param filename the filename
     * @param options optional split settings
     * @return an async job that can be polled for results
     */
    public AsyncJob pdfSplit(byte[] content, String filename, PdfSplitOptions options) {
        Map<String, String> fields = new HashMap<>();
        if (options != null) {
            if (options.getMode() != null) {
                fields.put("mode", options.getMode().getValue());
            }
            if (options.getPagesPerSplit() > 0) {
                fields.put("pagesPerSplit", String.valueOf(options.getPagesPerSplit()));
            }
        }

        String response = uploadFile("/pdf-split", content, filename, fields);
        return createAsyncJob(response);
    }

    /**
     * Splits a PDF from an InputStream.
     *
     * @param inputStream the input stream containing PDF content
     * @param filename the filename
     * @param options optional split settings
     * @return an async job that can be polled for results
     */
    public AsyncJob pdfSplit(InputStream inputStream, String filename, PdfSplitOptions options) {
        try {
            byte[] content = inputStream.readAllBytes();
            return pdfSplit(content, filename, options);
        } catch (IOException e) {
            throw new NetworkError("Failed to read input stream: " + e.getMessage(), e);
        }
    }

    /**
     * Extracts structured data from a document.
     *
     * <p>Example:</p>
     * <pre>{@code
     * ExtractOptions options = new ExtractOptions()
     *     .withPrompt("Extract the invoice number, date, and total amount");
     *
     * ExtractResult result = client.extract(Path.of("invoice.pdf"), options);
     *
     * Map<String, Object> data = result.getData();
     * System.out.println("Invoice: " + data.get("invoiceNumber"));
     * System.out.println("Total: $" + data.get("total"));
     * }</pre>
     *
     * @param file the document to extract data from
     * @param options extraction options (prompt or schema)
     * @return the extraction result with structured data
     * @throws ValidationError if the file format is not supported
     * @throws InsufficientCreditsError if the account has no credits
     * @throws RenamedError if the request fails
     */
    public ExtractResult extract(Path file, ExtractOptions options) {
        Map<String, String> fields = new HashMap<>();
        if (options != null) {
            if (options.getPrompt() != null) {
                fields.put("prompt", options.getPrompt());
            }
            if (options.getSchema() != null) {
                try {
                    fields.put("schema", objectMapper.writeValueAsString(options.getSchema()));
                } catch (IOException e) {
                    throw new ValidationError("Failed to serialize schema: " + e.getMessage());
                }
            }
        }

        String response = uploadFile("/extract", file, fields);
        return parseResponse(response, ExtractResult.class);
    }

    /**
     * Extracts structured data from a byte array.
     *
     * @param content the file content
     * @param filename the filename
     * @param options extraction options
     * @return the extraction result with structured data
     */
    public ExtractResult extract(byte[] content, String filename, ExtractOptions options) {
        Map<String, String> fields = new HashMap<>();
        if (options != null) {
            if (options.getPrompt() != null) {
                fields.put("prompt", options.getPrompt());
            }
            if (options.getSchema() != null) {
                try {
                    fields.put("schema", objectMapper.writeValueAsString(options.getSchema()));
                } catch (IOException e) {
                    throw new ValidationError("Failed to serialize schema: " + e.getMessage());
                }
            }
        }

        String response = uploadFile("/extract", content, filename, fields);
        return parseResponse(response, ExtractResult.class);
    }

    /**
     * Extracts structured data from an InputStream.
     *
     * @param inputStream the input stream containing file content
     * @param filename the filename
     * @param options extraction options
     * @return the extraction result with structured data
     */
    public ExtractResult extract(InputStream inputStream, String filename, ExtractOptions options) {
        try {
            byte[] content = inputStream.readAllBytes();
            return extract(content, filename, options);
        } catch (IOException e) {
            throw new NetworkError("Failed to read input stream: " + e.getMessage(), e);
        }
    }

    /**
     * Downloads a file from a URL (e.g., split document).
     *
     * <p>Example:</p>
     * <pre>{@code
     * PdfSplitResult result = job.await();
     * for (SplitDocument doc : result.getDocuments()) {
     *     byte[] content = client.downloadFile(doc.getDownloadUrl());
     *     Files.write(Path.of(doc.getFilename()), content);
     * }
     * }</pre>
     *
     * @param url the download URL
     * @return the file content as a byte array
     * @throws RenamedError if the download fails
     */
    public byte[] downloadFile(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + apiKey)
                    .GET()
                    .timeout(timeout)
                    .build();

            long startTime = System.currentTimeMillis();
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            long elapsed = System.currentTimeMillis() - startTime;

            // Extract path from URL for logging (avoid logging full URL which may contain tokens)
            String path = URI.create(url).getPath();
            if (logger != null) {
                logger.log("GET " + path + " -> " + response.statusCode() + " (" + elapsed + "ms)");
            }

            if (response.statusCode() >= 400) {
                throw RenamedError.fromHttpStatus(response.statusCode(),
                        "HTTP " + response.statusCode(), null);
            }

            if (logger != null) {
                logger.log("Download: " + DebugLogger.formatSize(response.body().length));
            }

            return response.body();
        } catch (IOException e) {
            throw new NetworkError("Download failed: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new NetworkError("Download interrupted", e);
        }
    }

    private String buildUrl(String path) {
        if (path.startsWith("http://") || path.startsWith("https://")) {
            return path;
        }
        if (path.startsWith("/")) {
            return baseUrl + path;
        }
        return baseUrl + "/" + path;
    }

    private String request(String method, String path, byte[] body, String contentType) {
        String url = buildUrl(path);
        Exception lastException = null;

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Authorization", "Bearer " + apiKey)
                        .timeout(timeout);

                if (body != null) {
                    requestBuilder.method(method, HttpRequest.BodyPublishers.ofByteArray(body));
                    if (contentType != null) {
                        requestBuilder.header("Content-Type", contentType);
                    }
                } else {
                    requestBuilder.method(method, HttpRequest.BodyPublishers.noBody());
                }

                long startTime = System.currentTimeMillis();
                HttpResponse<String> response = httpClient.send(requestBuilder.build(),
                        HttpResponse.BodyHandlers.ofString());
                long elapsed = System.currentTimeMillis() - startTime;

                if (logger != null) {
                    logger.log(method + " " + path + " -> " + response.statusCode() + " (" + elapsed + "ms)");
                }

                if (response.statusCode() >= 400) {
                    Map<String, Object> payload = parseErrorPayload(response.body());
                    throw RenamedError.fromHttpStatus(response.statusCode(),
                            "HTTP " + response.statusCode(), payload);
                }

                return response.body();
            } catch (IOException e) {
                lastException = e;
                if (attempt < maxRetries) {
                    long backoffMs = (1L << attempt) * 100;
                    if (logger != null) {
                        logger.log("Retry attempt " + (attempt + 1) + "/" + maxRetries + ", waiting " + backoffMs + "ms");
                    }
                    try {
                        Thread.sleep(backoffMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new NetworkError("Request interrupted", ie);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new NetworkError("Request interrupted", e);
            }
        }

        throw new NetworkError("Request failed after " + (maxRetries + 1) + " attempts: " +
                (lastException != null ? lastException.getMessage() : "unknown error"), lastException);
    }

    private String uploadFile(String path, Path file, Map<String, String> fields) {
        try {
            byte[] content = Files.readAllBytes(file);
            String filename = file.getFileName().toString();
            return uploadFile(path, content, filename, fields);
        } catch (IOException e) {
            throw new NetworkError("Failed to read file: " + e.getMessage(), e);
        }
    }

    private String uploadFile(String path, byte[] content, String filename, Map<String, String> fields) {
        if (logger != null) {
            logger.log("Upload: " + filename + " (" + DebugLogger.formatSize(content.length) + ")");
        }
        String boundary = "----RenamedBoundary" + UUID.randomUUID().toString().replace("-", "");
        byte[] body = buildMultipartBody(boundary, content, filename, fields);
        return request("POST", path, body, "multipart/form-data; boundary=" + boundary);
    }

    private byte[] buildMultipartBody(String boundary, byte[] fileContent, String filename, Map<String, String> fields) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8), true);

            // Add file field
            writer.append("--").append(boundary).append(CRLF);
            writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"")
                    .append(filename).append("\"").append(CRLF);
            writer.append("Content-Type: ").append(getMimeType(filename)).append(CRLF);
            writer.append(CRLF).flush();
            baos.write(fileContent);
            baos.flush();
            writer.append(CRLF);

            // Add additional fields
            for (Map.Entry<String, String> entry : fields.entrySet()) {
                writer.append("--").append(boundary).append(CRLF);
                writer.append("Content-Disposition: form-data; name=\"")
                        .append(entry.getKey()).append("\"").append(CRLF);
                writer.append(CRLF);
                writer.append(entry.getValue()).append(CRLF);
            }

            // End boundary
            writer.append("--").append(boundary).append("--").append(CRLF);
            writer.flush();

            return baos.toByteArray();
        } catch (IOException e) {
            throw new NetworkError("Failed to build multipart body: " + e.getMessage(), e);
        }
    }

    private String getMimeType(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".pdf")) {
            return "application/pdf";
        }
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (lower.endsWith(".png")) {
            return "image/png";
        }
        if (lower.endsWith(".tiff") || lower.endsWith(".tif")) {
            return "image/tiff";
        }
        return "application/octet-stream";
    }

    private <T> T parseResponse(String body, Class<T> type) {
        try {
            return objectMapper.readValue(body, type);
        } catch (IOException e) {
            throw new RenamedError("Failed to parse response: " + e.getMessage(), "PARSE_ERROR");
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseErrorPayload(String body) {
        try {
            return objectMapper.readValue(body, Map.class);
        } catch (IOException e) {
            return null;
        }
    }

    private AsyncJob createAsyncJob(String response) {
        try {
            JsonNode node = objectMapper.readTree(response);
            String statusUrl = node.get("statusUrl").asText();
            return new AsyncJob(httpClient, objectMapper, apiKey, statusUrl, logger);
        } catch (IOException e) {
            throw new RenamedError("Failed to parse async job response: " + e.getMessage(), "PARSE_ERROR");
        }
    }

    /**
     * Builder for configuring a RenamedClient.
     */
    public static class Builder {
        private final String apiKey;
        private String baseUrl = DEFAULT_BASE_URL;
        private Duration timeout = DEFAULT_TIMEOUT;
        private int maxRetries = DEFAULT_MAX_RETRIES;
        private HttpClient httpClient;
        private boolean debug = false;
        private Logger logger;

        private Builder(String apiKey) {
            this.apiKey = apiKey;
        }

        /**
         * Sets a custom base URL.
         *
         * @param baseUrl the API base URL
         * @return this builder
         */
        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        /**
         * Sets the request timeout.
         *
         * @param timeout the timeout duration
         * @return this builder
         */
        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        /**
         * Sets the maximum number of retries for failed requests.
         *
         * @param maxRetries the maximum retry count
         * @return this builder
         */
        public Builder maxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }

        /**
         * Sets a custom HTTP client.
         *
         * @param httpClient the HTTP client to use
         * @return this builder
         */
        public Builder httpClient(HttpClient httpClient) {
            this.httpClient = httpClient;
            return this;
        }

        /**
         * Enables or disables debug logging.
         *
         * <p>When enabled, the SDK logs HTTP requests, responses, retries,
         * file uploads, and async job polling. API keys are masked in logs.</p>
         *
         * <p>Example:</p>
         * <pre>{@code
         * RenamedClient client = RenamedClient.builder("rt_your_api_key")
         *     .debug(true)
         *     .build();
         * }</pre>
         *
         * @param debug true to enable debug logging
         * @return this builder
         */
        public Builder debug(boolean debug) {
            this.debug = debug;
            return this;
        }

        /**
         * Sets a custom logger for debug output.
         *
         * <p>If not set and debug is enabled, logs are written to System.out.
         * The logger can integrate with any logging framework.</p>
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
         * @param logger the logger to use for debug output
         * @return this builder
         */
        public Builder logger(Logger logger) {
            this.logger = logger;
            return this;
        }

        /**
         * Builds the RenamedClient with the configured settings.
         *
         * @return the configured client
         */
        public RenamedClient build() {
            return new RenamedClient(apiKey, baseUrl, timeout, maxRetries, httpClient, debug, logger);
        }
    }
}
