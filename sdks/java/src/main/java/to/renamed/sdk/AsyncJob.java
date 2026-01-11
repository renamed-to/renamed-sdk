package to.renamed.sdk;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * Represents an asynchronous job that can be polled for completion.
 *
 * <p>Async jobs are returned by operations like PDF splitting that may take
 * a long time to complete. Use {@link #await()} or {@link #await(ProgressCallback)}
 * to wait for the job to finish.</p>
 *
 * <p>Example:</p>
 * <pre>{@code
 * AsyncJob job = client.pdfSplit(file, options);
 *
 * // Wait with progress updates
 * PdfSplitResult result = job.await(status -> {
 *     System.out.println("Progress: " + status.getProgress() + "%");
 * });
 *
 * // Or just wait silently
 * PdfSplitResult result = job.await();
 * }</pre>
 */
public class AsyncJob {

    private static final Duration DEFAULT_POLL_INTERVAL = Duration.ofSeconds(2);
    private static final int DEFAULT_MAX_ATTEMPTS = 150; // 5 minutes at 2s intervals

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String statusUrl;
    private final Duration pollInterval;
    private final int maxAttempts;
    private final Logger logger;

    /**
     * Creates a new AsyncJob.
     *
     * @param httpClient the HTTP client to use
     * @param objectMapper the JSON object mapper
     * @param apiKey the API key for authentication
     * @param statusUrl the URL to poll for job status
     */
    AsyncJob(HttpClient httpClient, ObjectMapper objectMapper, String apiKey, String statusUrl) {
        this(httpClient, objectMapper, apiKey, statusUrl, null);
    }

    /**
     * Creates a new AsyncJob with optional logger.
     *
     * @param httpClient the HTTP client to use
     * @param objectMapper the JSON object mapper
     * @param apiKey the API key for authentication
     * @param statusUrl the URL to poll for job status
     * @param logger optional logger for debug output
     */
    AsyncJob(HttpClient httpClient, ObjectMapper objectMapper, String apiKey, String statusUrl, Logger logger) {
        this(httpClient, objectMapper, apiKey, statusUrl, DEFAULT_POLL_INTERVAL, DEFAULT_MAX_ATTEMPTS, logger);
    }

    /**
     * Creates a new AsyncJob with custom polling settings.
     *
     * @param httpClient the HTTP client to use
     * @param objectMapper the JSON object mapper
     * @param apiKey the API key for authentication
     * @param statusUrl the URL to poll for job status
     * @param pollInterval the interval between status checks
     * @param maxAttempts the maximum number of polling attempts
     * @param logger optional logger for debug output
     */
    AsyncJob(HttpClient httpClient, ObjectMapper objectMapper, String apiKey, String statusUrl,
             Duration pollInterval, int maxAttempts, Logger logger) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.statusUrl = statusUrl;
        this.pollInterval = pollInterval;
        this.maxAttempts = maxAttempts;
        this.logger = logger;
    }

    /**
     * Returns the status URL for this job.
     *
     * @return the URL to poll for status updates
     */
    public String getStatusUrl() {
        return statusUrl;
    }

    /**
     * Fetches the current job status.
     *
     * @return the current job status
     * @throws RenamedError if the status request fails
     */
    public JobStatusResponse getStatus() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(statusUrl))
                    .header("Authorization", "Bearer " + apiKey)
                    .GET()
                    .build();

            long startTime = System.currentTimeMillis();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            long elapsed = System.currentTimeMillis() - startTime;

            // Extract path from URL for logging
            String path = URI.create(statusUrl).getPath();
            if (logger != null) {
                logger.log("GET " + path + " -> " + response.statusCode() + " (" + elapsed + "ms)");
            }

            if (response.statusCode() >= 400) {
                Map<String, Object> payload = parseErrorPayload(response.body());
                throw RenamedError.fromHttpStatus(response.statusCode(),
                        "HTTP " + response.statusCode(), payload);
            }

            JobStatusResponse statusResponse = objectMapper.readValue(response.body(), JobStatusResponse.class);

            if (logger != null && statusResponse.getJobId() != null) {
                String statusText = statusResponse.getStatusValue();
                int progress = statusResponse.getProgress();
                logger.log("Job " + statusResponse.getJobId() + ": " + statusText + " (" + progress + "%)");
            }

            return statusResponse;
        } catch (IOException e) {
            throw new NetworkError("Failed to fetch job status: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new NetworkError("Request interrupted", e);
        }
    }

    /**
     * Waits for the job to complete without progress updates.
     *
     * @return the PDF split result when the job completes
     * @throws JobError if the job fails
     * @throws RenamedError if polling fails
     */
    public PdfSplitResult await() {
        return await(null);
    }

    /**
     * Waits for the job to complete, invoking the callback with progress updates.
     *
     * @param onProgress optional callback for progress updates
     * @return the PDF split result when the job completes
     * @throws JobError if the job fails
     * @throws RenamedError if polling fails or times out
     */
    public PdfSplitResult await(ProgressCallback onProgress) {
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            JobStatusResponse status = getStatus();

            if (onProgress != null) {
                onProgress.onProgress(status);
            }

            if (status.getStatus() == JobStatus.COMPLETED && status.getResult() != null) {
                return status.getResult();
            }

            if (status.getStatus() == JobStatus.FAILED) {
                throw new JobError(status.getError(), status.getJobId());
            }

            try {
                Thread.sleep(pollInterval.toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new NetworkError("Job polling interrupted", e);
            }
        }

        throw new JobError("Job polling timeout exceeded");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseErrorPayload(String body) {
        try {
            return objectMapper.readValue(body, Map.class);
        } catch (IOException e) {
            return null;
        }
    }
}
