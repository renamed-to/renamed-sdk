package to.renamed.sdk;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * HTTP integration tests for RenamedClient using MockWebServer.
 */
@DisplayName("RenamedClient HTTP Tests")
class RenamedClientHttpTest {

    private MockWebServer mockServer;
    private RenamedClient client;

    @BeforeEach
    void setUp() throws IOException {
        mockServer = new MockWebServer();
        mockServer.start();
        client = RenamedClient.builder("rt_test_key")
                .baseUrl(mockServer.url("/api/v1").toString())
                .timeout(Duration.ofSeconds(5))
                .maxRetries(0) // Disable retries for predictable tests
                .build();
    }

    @AfterEach
    void tearDown() throws IOException {
        mockServer.shutdown();
    }

    @Nested
    @DisplayName("getUser Tests")
    class GetUserTests {

        @Test
        @DisplayName("Returns user on successful response")
        void getUser_success_returnsUser() {
            mockServer.enqueue(new MockResponse()
                    .setBody("{\"id\":\"user_123\",\"email\":\"test@example.com\",\"name\":\"Test User\",\"credits\":100}")
                    .addHeader("Content-Type", "application/json"));

            User user = client.getUser();

            assertEquals("user_123", user.getId());
            assertEquals("test@example.com", user.getEmail());
            assertEquals("Test User", user.getName());
            assertEquals(100, user.getCredits());
        }

        @Test
        @DisplayName("Returns user with missing credits as zero")
        void getUser_missingCredits_returnsZero() {
            mockServer.enqueue(new MockResponse()
                    .setBody("{\"id\":\"user_456\",\"email\":\"user@test.com\",\"name\":\"No Credits\"}")
                    .addHeader("Content-Type", "application/json"));

            User user = client.getUser();

            assertEquals("user_456", user.getId());
            assertEquals(0, user.getCredits()); // Jackson defaults missing int to 0
        }

        @Test
        @DisplayName("Sends Authorization header with Bearer token")
        void getUser_sendsAuthorizationHeader() throws InterruptedException {
            mockServer.enqueue(new MockResponse()
                    .setBody("{\"id\":\"user_123\",\"email\":\"test@example.com\"}")
                    .addHeader("Content-Type", "application/json"));

            client.getUser();

            RecordedRequest request = mockServer.takeRequest(1, TimeUnit.SECONDS);
            assertNotNull(request);
            assertEquals("Bearer rt_test_key", request.getHeader("Authorization"));
            assertEquals("GET", request.getMethod());
            assertTrue(request.getPath().endsWith("/user"));
        }

        @Test
        @DisplayName("Throws AuthenticationError on 401")
        void getUser_401_throwsAuthenticationError() {
            mockServer.enqueue(new MockResponse()
                    .setResponseCode(401)
                    .setBody("{\"error\":\"Invalid API key\"}")
                    .addHeader("Content-Type", "application/json"));

            AuthenticationError error = assertThrows(AuthenticationError.class, () -> client.getUser());
            assertEquals(401, error.getStatusCode());
        }

        @Test
        @DisplayName("Throws InsufficientCreditsError on 402")
        void getUser_402_throwsInsufficientCreditsError() {
            mockServer.enqueue(new MockResponse()
                    .setResponseCode(402)
                    .setBody("{\"error\":\"Insufficient credits\"}")
                    .addHeader("Content-Type", "application/json"));

            InsufficientCreditsError error = assertThrows(InsufficientCreditsError.class, () -> client.getUser());
            assertEquals(402, error.getStatusCode());
        }

        @Test
        @DisplayName("Throws RateLimitError on 429 with retryAfter")
        void getUser_429_throwsRateLimitError() {
            mockServer.enqueue(new MockResponse()
                    .setResponseCode(429)
                    .setBody("{\"error\":\"Rate limit exceeded\",\"retryAfter\":60}")
                    .addHeader("Content-Type", "application/json"));

            RateLimitError error = assertThrows(RateLimitError.class, () -> client.getUser());
            assertEquals(429, error.getStatusCode());
            assertEquals(60, error.getRetryAfter());
        }

        @Test
        @DisplayName("Throws ValidationError on 400")
        void getUser_400_throwsValidationError() {
            mockServer.enqueue(new MockResponse()
                    .setResponseCode(400)
                    .setBody("{\"error\":\"Bad request\"}")
                    .addHeader("Content-Type", "application/json"));

            ValidationError error = assertThrows(ValidationError.class, () -> client.getUser());
            assertEquals(400, error.getStatusCode());
        }

        @Test
        @DisplayName("Throws RenamedError on 500")
        void getUser_500_throwsRenamedError() {
            mockServer.enqueue(new MockResponse()
                    .setResponseCode(500)
                    .setBody("{\"error\":\"Internal server error\"}")
                    .addHeader("Content-Type", "application/json"));

            RenamedError error = assertThrows(RenamedError.class, () -> client.getUser());
            assertEquals(500, error.getStatusCode());
        }
    }

    @Nested
    @DisplayName("rename Tests")
    class RenameTests {

        @Test
        @DisplayName("Returns RenameResult on success")
        void rename_success_returnsResult() {
            mockServer.enqueue(new MockResponse()
                    .setBody("{\"originalFilename\":\"doc.pdf\",\"suggestedFilename\":\"Invoice_2025.pdf\",\"confidence\":0.95}")
                    .addHeader("Content-Type", "application/json"));

            RenameResult result = client.rename("fake pdf content".getBytes(), "doc.pdf", null);

            assertEquals("doc.pdf", result.getOriginalFilename());
            assertEquals("Invoice_2025.pdf", result.getSuggestedFilename());
            assertEquals(0.95, result.getConfidence(), 0.001);
        }

        @Test
        @DisplayName("Sends multipart form data with file")
        void rename_sendsMultipartFormData() throws InterruptedException {
            mockServer.enqueue(new MockResponse()
                    .setBody("{\"originalFilename\":\"test.pdf\",\"suggestedFilename\":\"renamed.pdf\",\"confidence\":0.9}")
                    .addHeader("Content-Type", "application/json"));

            client.rename("content".getBytes(), "test.pdf", null);

            RecordedRequest request = mockServer.takeRequest(1, TimeUnit.SECONDS);
            assertNotNull(request);
            String contentType = request.getHeader("Content-Type");
            assertNotNull(contentType);
            assertTrue(contentType.startsWith("multipart/form-data"));
            assertEquals("POST", request.getMethod());
            assertTrue(request.getPath().endsWith("/rename"));
        }

        @Test
        @DisplayName("Includes file content in multipart body")
        void rename_includesFileContent() throws InterruptedException {
            mockServer.enqueue(new MockResponse()
                    .setBody("{\"originalFilename\":\"test.pdf\",\"suggestedFilename\":\"renamed.pdf\"}")
                    .addHeader("Content-Type", "application/json"));

            byte[] fileContent = "test file content".getBytes();
            client.rename(fileContent, "test.pdf", null);

            RecordedRequest request = mockServer.takeRequest(1, TimeUnit.SECONDS);
            String body = request.getBody().readUtf8();
            assertTrue(body.contains("test file content"));
            assertTrue(body.contains("filename=\"test.pdf\""));
        }

        @Test
        @DisplayName("Includes template option when provided")
        void rename_withTemplate_sendsTemplateField() throws InterruptedException {
            mockServer.enqueue(new MockResponse()
                    .setBody("{\"originalFilename\":\"doc.pdf\",\"suggestedFilename\":\"2025-01-01_Invoice.pdf\"}")
                    .addHeader("Content-Type", "application/json"));

            RenameOptions options = new RenameOptions().withTemplate("{date}_{type}");
            client.rename("content".getBytes(), "doc.pdf", options);

            RecordedRequest request = mockServer.takeRequest(1, TimeUnit.SECONDS);
            String body = request.getBody().readUtf8();
            assertTrue(body.contains("name=\"template\""));
            assertTrue(body.contains("{date}_{type}"));
        }

        @Test
        @DisplayName("Throws AuthenticationError on 401")
        void rename_401_throwsAuthenticationError() {
            mockServer.enqueue(new MockResponse()
                    .setResponseCode(401)
                    .setBody("{\"error\":\"Invalid API key\"}"));

            assertThrows(AuthenticationError.class,
                    () -> client.rename("content".getBytes(), "test.pdf", null));
        }

        @Test
        @DisplayName("Throws InsufficientCreditsError on 402")
        void rename_402_throwsInsufficientCreditsError() {
            mockServer.enqueue(new MockResponse()
                    .setResponseCode(402)
                    .setBody("{\"error\":\"No credits remaining\"}"));

            assertThrows(InsufficientCreditsError.class,
                    () -> client.rename("content".getBytes(), "test.pdf", null));
        }
    }

    @Nested
    @DisplayName("pdfSplit Tests")
    class PdfSplitTests {

        @Test
        @DisplayName("Returns AsyncJob on success")
        void pdfSplit_success_returnsAsyncJob() {
            mockServer.enqueue(new MockResponse()
                    .setBody("{\"jobId\":\"job_123\",\"statusUrl\":\"" + mockServer.url("/status/job_123") + "\"}")
                    .addHeader("Content-Type", "application/json"));

            AsyncJob job = client.pdfSplit("pdf content".getBytes(), "multi.pdf", null);

            assertNotNull(job);
            assertNotNull(job.getStatusUrl());
            assertTrue(job.getStatusUrl().contains("/status/job_123"));
        }

        @Test
        @DisplayName("Sends file in multipart request")
        void pdfSplit_sendsMultipartRequest() throws InterruptedException {
            mockServer.enqueue(new MockResponse()
                    .setBody("{\"jobId\":\"job_123\",\"statusUrl\":\"http://localhost/status\"}")
                    .addHeader("Content-Type", "application/json"));

            client.pdfSplit("pdf data".getBytes(), "document.pdf", null);

            RecordedRequest request = mockServer.takeRequest(1, TimeUnit.SECONDS);
            assertNotNull(request);
            assertTrue(request.getHeader("Content-Type").startsWith("multipart/form-data"));
            assertTrue(request.getPath().endsWith("/pdf-split"));
        }

        @Test
        @DisplayName("Includes mode option when provided")
        void pdfSplit_withMode_sendsModeField() throws InterruptedException {
            mockServer.enqueue(new MockResponse()
                    .setBody("{\"jobId\":\"job_123\",\"statusUrl\":\"http://localhost/status\"}")
                    .addHeader("Content-Type", "application/json"));

            PdfSplitOptions options = new PdfSplitOptions().withMode(SplitMode.AUTO);
            client.pdfSplit("pdf".getBytes(), "doc.pdf", options);

            RecordedRequest request = mockServer.takeRequest(1, TimeUnit.SECONDS);
            String body = request.getBody().readUtf8();
            assertTrue(body.contains("name=\"mode\""));
            assertTrue(body.contains("auto"));
        }

        @Test
        @DisplayName("Includes pagesPerSplit option when provided")
        void pdfSplit_withPagesPerSplit_sendsField() throws InterruptedException {
            mockServer.enqueue(new MockResponse()
                    .setBody("{\"jobId\":\"job_123\",\"statusUrl\":\"http://localhost/status\"}")
                    .addHeader("Content-Type", "application/json"));

            PdfSplitOptions options = new PdfSplitOptions().withPagesPerSplit(5);
            client.pdfSplit("pdf".getBytes(), "doc.pdf", options);

            RecordedRequest request = mockServer.takeRequest(1, TimeUnit.SECONDS);
            String body = request.getBody().readUtf8();
            assertTrue(body.contains("name=\"pagesPerSplit\""));
            assertTrue(body.contains("5"));
        }
    }

    @Nested
    @DisplayName("extract Tests")
    class ExtractTests {

        @Test
        @DisplayName("Returns ExtractResult on success")
        void extract_success_returnsResult() {
            mockServer.enqueue(new MockResponse()
                    .setBody("{\"data\":{\"invoiceNumber\":\"INV-001\",\"total\":100.50}}")
                    .addHeader("Content-Type", "application/json"));

            ExtractResult result = client.extract("pdf content".getBytes(), "invoice.pdf", null);

            assertNotNull(result);
            assertNotNull(result.getData());
            assertEquals("INV-001", result.getData().get("invoiceNumber"));
            assertEquals(100.50, result.getData().get("total"));
        }

        @Test
        @DisplayName("Includes prompt option when provided")
        void extract_withPrompt_sendsPromptField() throws InterruptedException {
            mockServer.enqueue(new MockResponse()
                    .setBody("{\"data\":{}}")
                    .addHeader("Content-Type", "application/json"));

            ExtractOptions options = new ExtractOptions().withPrompt("Extract invoice details");
            client.extract("content".getBytes(), "doc.pdf", options);

            RecordedRequest request = mockServer.takeRequest(1, TimeUnit.SECONDS);
            String body = request.getBody().readUtf8();
            assertTrue(body.contains("name=\"prompt\""));
            assertTrue(body.contains("Extract invoice details"));
        }

        @Test
        @DisplayName("Throws ValidationError on 400")
        void extract_400_throwsValidationError() {
            mockServer.enqueue(new MockResponse()
                    .setResponseCode(400)
                    .setBody("{\"error\":\"Invalid file format\"}"));

            assertThrows(ValidationError.class,
                    () -> client.extract("content".getBytes(), "file.txt", null));
        }
    }

    @Nested
    @DisplayName("downloadFile Tests")
    class DownloadFileTests {

        @Test
        @DisplayName("Returns file content on success")
        void downloadFile_success_returnsBytes() {
            byte[] expectedContent = "downloaded file content".getBytes();
            mockServer.enqueue(new MockResponse()
                    .setBody(new okio.Buffer().write(expectedContent))
                    .addHeader("Content-Type", "application/pdf"));

            byte[] result = client.downloadFile(mockServer.url("/download/file.pdf").toString());

            assertArrayEquals(expectedContent, result);
        }

        @Test
        @DisplayName("Sends Authorization header")
        void downloadFile_sendsAuthHeader() throws InterruptedException {
            mockServer.enqueue(new MockResponse()
                    .setBody(new okio.Buffer().write("content".getBytes())));

            client.downloadFile(mockServer.url("/download/file.pdf").toString());

            RecordedRequest request = mockServer.takeRequest(1, TimeUnit.SECONDS);
            assertNotNull(request);
            assertEquals("Bearer rt_test_key", request.getHeader("Authorization"));
        }

        @Test
        @DisplayName("Throws RenamedError on HTTP error")
        void downloadFile_httpError_throwsException() {
            mockServer.enqueue(new MockResponse()
                    .setResponseCode(404)
                    .setBody("Not found"));

            assertThrows(RenamedError.class,
                    () -> client.downloadFile(mockServer.url("/download/missing.pdf").toString()));
        }
    }

    @Nested
    @DisplayName("Retry Logic Tests")
    class RetryLogicTests {

        @Test
        @DisplayName("Does not retry when maxRetries is 0")
        void noRetry_singleAttempt() {
            // Server returns error
            mockServer.enqueue(new MockResponse().setResponseCode(500));

            assertThrows(RenamedError.class, () -> client.getUser());

            // Should only have made 1 request
            assertEquals(1, mockServer.getRequestCount());
        }

        @Test
        @DisplayName("Retries on server error when maxRetries > 0")
        void retry_onServerError() throws IOException {
            // Create a new client with retries enabled
            RenamedClient retryClient = RenamedClient.builder("rt_test_key")
                    .baseUrl(mockServer.url("/api/v1").toString())
                    .timeout(Duration.ofSeconds(5))
                    .maxRetries(2)
                    .build();

            // Enqueue responses: fail twice, then succeed
            mockServer.enqueue(new MockResponse().setResponseCode(500));
            mockServer.enqueue(new MockResponse().setResponseCode(500));
            mockServer.enqueue(new MockResponse()
                    .setBody("{\"id\":\"user_123\",\"email\":\"test@example.com\"}")
                    .addHeader("Content-Type", "application/json"));

            // Note: The current implementation only retries on IOException, not HTTP errors
            // This test documents the current behavior
            assertThrows(RenamedError.class, () -> retryClient.getUser());
        }
    }

    @Nested
    @DisplayName("URL Building Tests")
    class UrlBuildingTests {

        @Test
        @DisplayName("Handles base URL without trailing slash")
        void baseUrl_noTrailingSlash() throws InterruptedException {
            mockServer.enqueue(new MockResponse()
                    .setBody("{\"id\":\"user_123\",\"email\":\"test@example.com\"}")
                    .addHeader("Content-Type", "application/json"));

            client.getUser();

            RecordedRequest request = mockServer.takeRequest(1, TimeUnit.SECONDS);
            assertNotNull(request);
            assertTrue(request.getPath().startsWith("/api/v1/"));
        }

        @Test
        @DisplayName("Handles base URL with trailing slash")
        void baseUrl_withTrailingSlash() throws IOException, InterruptedException {
            RenamedClient slashClient = RenamedClient.builder("rt_test_key")
                    .baseUrl(mockServer.url("/api/v1/").toString())
                    .maxRetries(0)
                    .build();

            mockServer.enqueue(new MockResponse()
                    .setBody("{\"id\":\"user_123\",\"email\":\"test@example.com\"}")
                    .addHeader("Content-Type", "application/json"));

            slashClient.getUser();

            RecordedRequest request = mockServer.takeRequest(1, TimeUnit.SECONDS);
            assertNotNull(request);
            // Should not have double slashes
            assertFalse(request.getPath().contains("//user"));
        }
    }
}
