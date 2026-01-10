package to.renamed.sdk;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for RenamedClient initialization and builder pattern.
 *
 * Note: Tests that require mocking HttpClient are limited due to Java 11+
 * restrictions on mocking sealed/final classes. Integration tests should
 * be used for full HTTP request/response testing.
 */
class RenamedClientTest {

    private static final String TEST_API_KEY = "rt_test_api_key_12345";
    private static final String CUSTOM_BASE_URL = "https://custom-api.example.com";

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Creates client with valid API key")
        void createsClientWithValidApiKey() {
            RenamedClient client = new RenamedClient(TEST_API_KEY);
            assertNotNull(client);
        }

        @Test
        @DisplayName("Throws exception when API key is null")
        void throwsExceptionWhenApiKeyIsNull() {
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new RenamedClient(null)
            );
            assertEquals("API key is required", exception.getMessage());
        }

        @Test
        @DisplayName("Throws exception when API key is empty")
        void throwsExceptionWhenApiKeyIsEmpty() {
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new RenamedClient("")
            );
            assertEquals("API key is required", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Builder Tests")
    class BuilderTests {

        @Test
        @DisplayName("Creates builder with API key")
        void createsBuilderWithApiKey() {
            RenamedClient.Builder builder = RenamedClient.builder(TEST_API_KEY);
            assertNotNull(builder);
        }

        @Test
        @DisplayName("Builder creates client with default settings")
        void builderCreatesClientWithDefaults() {
            RenamedClient client = RenamedClient.builder(TEST_API_KEY).build();
            assertNotNull(client);
        }

        @Test
        @DisplayName("Builder accepts custom base URL")
        void builderAcceptsCustomBaseUrl() {
            RenamedClient client = RenamedClient.builder(TEST_API_KEY)
                .baseUrl(CUSTOM_BASE_URL)
                .build();
            assertNotNull(client);
        }

        @Test
        @DisplayName("Builder accepts custom timeout")
        void builderAcceptsCustomTimeout() {
            RenamedClient client = RenamedClient.builder(TEST_API_KEY)
                .timeout(Duration.ofSeconds(60))
                .build();
            assertNotNull(client);
        }

        @Test
        @DisplayName("Builder accepts custom max retries")
        void builderAcceptsMaxRetries() {
            RenamedClient client = RenamedClient.builder(TEST_API_KEY)
                .maxRetries(5)
                .build();
            assertNotNull(client);
        }

        @Test
        @DisplayName("Builder accepts custom HTTP client")
        void builderAcceptsCustomHttpClient() {
            HttpClient customHttpClient = HttpClient.newHttpClient();
            RenamedClient client = RenamedClient.builder(TEST_API_KEY)
                .httpClient(customHttpClient)
                .build();
            assertNotNull(client);
        }

        @Test
        @DisplayName("Builder supports method chaining")
        void builderSupportsMethodChaining() {
            RenamedClient client = RenamedClient.builder(TEST_API_KEY)
                .baseUrl(CUSTOM_BASE_URL)
                .timeout(Duration.ofSeconds(45))
                .maxRetries(3)
                .httpClient(HttpClient.newHttpClient())
                .build();
            assertNotNull(client);
        }

        @Test
        @DisplayName("Builder throws exception for null API key")
        void builderThrowsExceptionForNullApiKey() {
            assertThrows(
                IllegalArgumentException.class,
                () -> RenamedClient.builder(null).build()
            );
        }

        @Test
        @DisplayName("Builder throws exception for empty API key")
        void builderThrowsExceptionForEmptyApiKey() {
            assertThrows(
                IllegalArgumentException.class,
                () -> RenamedClient.builder("").build()
            );
        }

        @Test
        @DisplayName("Builder with zero max retries")
        void builderWithZeroMaxRetries() {
            RenamedClient client = RenamedClient.builder(TEST_API_KEY)
                .maxRetries(0)
                .build();
            assertNotNull(client);
        }

        @Test
        @DisplayName("Builder with short timeout")
        void builderWithShortTimeout() {
            RenamedClient client = RenamedClient.builder(TEST_API_KEY)
                .timeout(Duration.ofMillis(100))
                .build();
            assertNotNull(client);
        }

        @Test
        @DisplayName("Builder with long timeout")
        void builderWithLongTimeout() {
            RenamedClient client = RenamedClient.builder(TEST_API_KEY)
                .timeout(Duration.ofMinutes(5))
                .build();
            assertNotNull(client);
        }

        @Test
        @DisplayName("Builder with custom base URL ending with slash")
        void builderWithBaseUrlEndingWithSlash() {
            // The builder should strip the trailing slash internally
            RenamedClient client = RenamedClient.builder(TEST_API_KEY)
                .baseUrl("https://api.example.com/v1/")
                .build();
            assertNotNull(client);
        }

        @Test
        @DisplayName("Builder with custom base URL without slash")
        void builderWithBaseUrlWithoutSlash() {
            RenamedClient client = RenamedClient.builder(TEST_API_KEY)
                .baseUrl("https://api.example.com/v1")
                .build();
            assertNotNull(client);
        }
    }

    @Nested
    @DisplayName("RenameOptions Tests")
    class RenameOptionsTests {

        @Test
        @DisplayName("RenameOptions with template")
        void renameOptionsWithTemplate() {
            RenameOptions options = new RenameOptions().withTemplate("{date}_{type}");
            assertEquals("{date}_{type}", options.getTemplate());
        }

        @Test
        @DisplayName("RenameOptions default values")
        void renameOptionsDefaultValues() {
            RenameOptions options = new RenameOptions();
            assertNull(options.getTemplate());
        }

        @Test
        @DisplayName("RenameOptions method chaining")
        void renameOptionsMethodChaining() {
            RenameOptions options = new RenameOptions()
                .withTemplate("{date}_{description}");
            assertSame(options, options.withTemplate("new_template"));
        }
    }

    @Nested
    @DisplayName("ExtractOptions Tests")
    class ExtractOptionsTests {

        @Test
        @DisplayName("ExtractOptions with prompt")
        void extractOptionsWithPrompt() {
            ExtractOptions options = new ExtractOptions()
                .withPrompt("Extract invoice details");
            assertEquals("Extract invoice details", options.getPrompt());
        }

        @Test
        @DisplayName("ExtractOptions default values")
        void extractOptionsDefaultValues() {
            ExtractOptions options = new ExtractOptions();
            assertNull(options.getPrompt());
            assertNull(options.getSchema());
        }

        @Test
        @DisplayName("ExtractOptions method chaining")
        void extractOptionsMethodChaining() {
            ExtractOptions options = new ExtractOptions();
            ExtractOptions returned = options.withPrompt("test");
            assertSame(options, returned);
        }
    }

    @Nested
    @DisplayName("Client Configuration Validation Tests")
    class ClientConfigurationTests {

        @Test
        @DisplayName("Client created with minimum configuration")
        void clientCreatedWithMinimumConfiguration() {
            // Only API key is required
            RenamedClient client = new RenamedClient(TEST_API_KEY);
            assertNotNull(client);
        }

        @Test
        @DisplayName("Client created with full builder configuration")
        void clientCreatedWithFullBuilderConfiguration() {
            HttpClient customClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

            RenamedClient client = RenamedClient.builder(TEST_API_KEY)
                .baseUrl("https://custom.api.com/v2")
                .timeout(Duration.ofSeconds(45))
                .maxRetries(5)
                .httpClient(customClient)
                .build();

            assertNotNull(client);
        }

        @Test
        @DisplayName("API key with prefix is accepted")
        void apiKeyWithPrefixIsAccepted() {
            RenamedClient client = new RenamedClient("rt_12345abcdef");
            assertNotNull(client);
        }

        @Test
        @DisplayName("API key without prefix is accepted")
        void apiKeyWithoutPrefixIsAccepted() {
            // SDK accepts any non-empty API key - validation is server-side
            RenamedClient client = new RenamedClient("any_api_key_format");
            assertNotNull(client);
        }
    }
}
