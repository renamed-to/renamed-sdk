package to.renamed.sdk;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for error handling and exception mapping.
 */
class ErrorsTest {

    @Nested
    @DisplayName("RenamedError Base Class Tests")
    class RenamedErrorTests {

        @Test
        @DisplayName("Creates error with message and code")
        void createsErrorWithMessageAndCode() {
            RenamedError error = new RenamedError("Something went wrong", "GENERIC_ERROR");

            assertEquals("Something went wrong", error.getMessage());
            assertEquals("GENERIC_ERROR", error.getCode());
            assertEquals(0, error.getStatusCode());
            assertNull(error.getDetails());
        }

        @Test
        @DisplayName("Creates error with all properties")
        void createsErrorWithAllProperties() {
            Map<String, Object> details = Map.of("field", "value", "count", 42);

            RenamedError error = new RenamedError("Detailed error", "DETAILED_ERROR", 500, details);

            assertEquals("Detailed error", error.getMessage());
            assertEquals("DETAILED_ERROR", error.getCode());
            assertEquals(500, error.getStatusCode());
            assertNotNull(error.getDetails());
            assertEquals("value", error.getDetails().get("field"));
            assertEquals(42, error.getDetails().get("count"));
        }

        @Test
        @DisplayName("Creates error with cause")
        void createsErrorWithCause() {
            Exception cause = new RuntimeException("Root cause");
            RenamedError error = new RenamedError("Wrapped error", "WRAPPED_ERROR", cause);

            assertEquals("Wrapped error", error.getMessage());
            assertEquals("WRAPPED_ERROR", error.getCode());
            assertEquals(0, error.getStatusCode());
            assertSame(cause, error.getCause());
        }

        @Test
        @DisplayName("toString with status code")
        void toStringWithStatusCode() {
            RenamedError error = new RenamedError("Server error", "SERVER_ERROR", 500, null);

            String result = error.toString();

            assertTrue(result.contains("SERVER_ERROR"));
            assertTrue(result.contains("500"));
            assertTrue(result.contains("Server error"));
        }

        @Test
        @DisplayName("toString without status code")
        void toStringWithoutStatusCode() {
            RenamedError error = new RenamedError("Network failed", "NETWORK_ERROR");

            String result = error.toString();

            assertTrue(result.contains("NETWORK_ERROR"));
            assertTrue(result.contains("Network failed"));
            assertFalse(result.contains("status"));
        }
    }

    @Nested
    @DisplayName("RenamedError.fromHttpStatus Factory Tests")
    class FromHttpStatusTests {

        @Test
        @DisplayName("Returns AuthenticationError for 401")
        void returnsAuthenticationErrorFor401() {
            RenamedError error = RenamedError.fromHttpStatus(401, "Unauthorized", null);

            assertInstanceOf(AuthenticationError.class, error);
            assertEquals("Unauthorized", error.getMessage());
            assertEquals("AUTHENTICATION_ERROR", error.getCode());
            assertEquals(401, error.getStatusCode());
        }

        @Test
        @DisplayName("Returns InsufficientCreditsError for 402")
        void returnsInsufficientCreditsErrorFor402() {
            RenamedError error = RenamedError.fromHttpStatus(402, "Payment Required", null);

            assertInstanceOf(InsufficientCreditsError.class, error);
            assertEquals("Payment Required", error.getMessage());
            assertEquals("INSUFFICIENT_CREDITS", error.getCode());
            assertEquals(402, error.getStatusCode());
        }

        @Test
        @DisplayName("Returns ValidationError for 400")
        void returnsValidationErrorFor400() {
            Map<String, Object> payload = Map.of("error", "Invalid file format");

            RenamedError error = RenamedError.fromHttpStatus(400, "Bad Request", payload);

            assertInstanceOf(ValidationError.class, error);
            assertEquals("Invalid file format", error.getMessage());
            assertEquals("VALIDATION_ERROR", error.getCode());
            assertEquals(400, error.getStatusCode());
        }

        @Test
        @DisplayName("Returns ValidationError for 422")
        void returnsValidationErrorFor422() {
            Map<String, Object> payload = Map.of("error", "Unprocessable entity");

            RenamedError error = RenamedError.fromHttpStatus(422, "Unprocessable Entity", payload);

            assertInstanceOf(ValidationError.class, error);
            assertEquals("Unprocessable entity", error.getMessage());
        }

        @Test
        @DisplayName("Returns RateLimitError for 429")
        void returnsRateLimitErrorFor429() {
            Map<String, Object> payload = new HashMap<>();
            payload.put("error", "Too many requests");
            payload.put("retryAfter", 60);

            RenamedError error = RenamedError.fromHttpStatus(429, "Too Many Requests", payload);

            assertInstanceOf(RateLimitError.class, error);
            assertEquals("Too many requests", error.getMessage());
            assertEquals("RATE_LIMIT_ERROR", error.getCode());
            assertEquals(429, error.getStatusCode());
            assertEquals(60, ((RateLimitError) error).getRetryAfter());
        }

        @Test
        @DisplayName("Returns RateLimitError with zero retry for missing retryAfter")
        void returnsRateLimitErrorWithZeroRetryForMissingRetryAfter() {
            Map<String, Object> payload = Map.of("error", "Rate limited");

            RenamedError error = RenamedError.fromHttpStatus(429, "Too Many Requests", payload);

            assertInstanceOf(RateLimitError.class, error);
            assertEquals(0, ((RateLimitError) error).getRetryAfter());
        }

        @Test
        @DisplayName("Returns generic RenamedError for 500")
        void returnsGenericRenamedErrorFor500() {
            RenamedError error = RenamedError.fromHttpStatus(500, "Internal Server Error", null);

            assertEquals(RenamedError.class, error.getClass());
            assertEquals("Internal Server Error", error.getMessage());
            assertEquals("API_ERROR", error.getCode());
            assertEquals(500, error.getStatusCode());
        }

        @Test
        @DisplayName("Returns generic RenamedError for 503")
        void returnsGenericRenamedErrorFor503() {
            RenamedError error = RenamedError.fromHttpStatus(503, "Service Unavailable", null);

            assertEquals(RenamedError.class, error.getClass());
            assertEquals("API_ERROR", error.getCode());
            assertEquals(503, error.getStatusCode());
        }

        @Test
        @DisplayName("Extracts error message from payload")
        void extractsErrorMessageFromPayload() {
            Map<String, Object> payload = Map.of("error", "Custom error message");

            RenamedError error = RenamedError.fromHttpStatus(500, "Server Error", payload);

            assertEquals("Custom error message", error.getMessage());
        }

        @Test
        @DisplayName("Falls back to status text when payload has no error")
        void fallsBackToStatusTextWhenPayloadHasNoError() {
            Map<String, Object> payload = Map.of("someOtherField", "value");

            RenamedError error = RenamedError.fromHttpStatus(500, "Server Error", payload);

            assertEquals("Server Error", error.getMessage());
        }

        @Test
        @DisplayName("Handles null payload")
        void handlesNullPayload() {
            RenamedError error = RenamedError.fromHttpStatus(404, "Not Found", null);

            assertEquals("Not Found", error.getMessage());
        }
    }

    @Nested
    @DisplayName("AuthenticationError Tests")
    class AuthenticationErrorTests {

        @Test
        @DisplayName("Creates with default message")
        void createsWithDefaultMessage() {
            AuthenticationError error = new AuthenticationError();

            assertEquals("Invalid or missing API key", error.getMessage());
            assertEquals("AUTHENTICATION_ERROR", error.getCode());
            assertEquals(401, error.getStatusCode());
        }

        @Test
        @DisplayName("Creates with custom message")
        void createsWithCustomMessage() {
            AuthenticationError error = new AuthenticationError("API key expired");

            assertEquals("API key expired", error.getMessage());
            assertEquals("AUTHENTICATION_ERROR", error.getCode());
            assertEquals(401, error.getStatusCode());
        }

        @Test
        @DisplayName("Falls back to default message for null")
        void fallsBackToDefaultMessageForNull() {
            AuthenticationError error = new AuthenticationError(null);

            assertEquals("Invalid or missing API key", error.getMessage());
        }

        @Test
        @DisplayName("Falls back to default message for empty string")
        void fallsBackToDefaultMessageForEmptyString() {
            AuthenticationError error = new AuthenticationError("");

            assertEquals("Invalid or missing API key", error.getMessage());
        }

        @Test
        @DisplayName("Is a RenamedError")
        void isARenamedError() {
            AuthenticationError error = new AuthenticationError();

            assertInstanceOf(RenamedError.class, error);
            assertInstanceOf(RuntimeException.class, error);
        }
    }

    @Nested
    @DisplayName("ValidationError Tests")
    class ValidationErrorTests {

        @Test
        @DisplayName("Creates with message only")
        void createsWithMessageOnly() {
            ValidationError error = new ValidationError("File too large");

            assertEquals("File too large", error.getMessage());
            assertEquals("VALIDATION_ERROR", error.getCode());
            assertEquals(400, error.getStatusCode());
            assertNull(error.getDetails());
        }

        @Test
        @DisplayName("Creates with message and details")
        void createsWithMessageAndDetails() {
            Map<String, Object> details = Map.of(
                "maxSize", 10485760,
                "actualSize", 52428800
            );

            ValidationError error = new ValidationError("File too large", details);

            assertEquals("File too large", error.getMessage());
            assertNotNull(error.getDetails());
            assertEquals(10485760, error.getDetails().get("maxSize"));
            assertEquals(52428800, error.getDetails().get("actualSize"));
        }

        @Test
        @DisplayName("Is a RenamedError")
        void isARenamedError() {
            ValidationError error = new ValidationError("Invalid format");

            assertInstanceOf(RenamedError.class, error);
        }
    }

    @Nested
    @DisplayName("InsufficientCreditsError Tests")
    class InsufficientCreditsErrorTests {

        @Test
        @DisplayName("Creates with default message")
        void createsWithDefaultMessage() {
            InsufficientCreditsError error = new InsufficientCreditsError();

            assertEquals("Insufficient credits", error.getMessage());
            assertEquals("INSUFFICIENT_CREDITS", error.getCode());
            assertEquals(402, error.getStatusCode());
        }

        @Test
        @DisplayName("Creates with custom message")
        void createsWithCustomMessage() {
            InsufficientCreditsError error = new InsufficientCreditsError("You need 10 credits, but have 0");

            assertEquals("You need 10 credits, but have 0", error.getMessage());
            assertEquals("INSUFFICIENT_CREDITS", error.getCode());
        }

        @Test
        @DisplayName("Falls back to default message for null")
        void fallsBackToDefaultMessageForNull() {
            InsufficientCreditsError error = new InsufficientCreditsError(null);

            assertEquals("Insufficient credits", error.getMessage());
        }

        @Test
        @DisplayName("Falls back to default message for empty string")
        void fallsBackToDefaultMessageForEmptyString() {
            InsufficientCreditsError error = new InsufficientCreditsError("");

            assertEquals("Insufficient credits", error.getMessage());
        }
    }

    @Nested
    @DisplayName("RateLimitError Tests")
    class RateLimitErrorTests {

        @Test
        @DisplayName("Creates with default message")
        void createsWithDefaultMessage() {
            RateLimitError error = new RateLimitError();

            assertEquals("Rate limit exceeded", error.getMessage());
            assertEquals("RATE_LIMIT_ERROR", error.getCode());
            assertEquals(429, error.getStatusCode());
            assertEquals(0, error.getRetryAfter());
        }

        @Test
        @DisplayName("Creates with custom message and retry time")
        void createsWithCustomMessageAndRetryTime() {
            RateLimitError error = new RateLimitError("Slow down", 30);

            assertEquals("Slow down", error.getMessage());
            assertEquals(30, error.getRetryAfter());
        }

        @Test
        @DisplayName("Falls back to default message for null")
        void fallsBackToDefaultMessageForNull() {
            RateLimitError error = new RateLimitError(null, 45);

            assertEquals("Rate limit exceeded", error.getMessage());
            assertEquals(45, error.getRetryAfter());
        }

        @Test
        @DisplayName("Handles zero retry after")
        void handlesZeroRetryAfter() {
            RateLimitError error = new RateLimitError("Too fast", 0);

            assertEquals(0, error.getRetryAfter());
        }
    }

    @Nested
    @DisplayName("NetworkError Tests")
    class NetworkErrorTests {

        @Test
        @DisplayName("Creates with default message")
        void createsWithDefaultMessage() {
            NetworkError error = new NetworkError();

            assertEquals("Network request failed", error.getMessage());
            assertEquals("NETWORK_ERROR", error.getCode());
            assertEquals(0, error.getStatusCode());
        }

        @Test
        @DisplayName("Creates with custom message")
        void createsWithCustomMessage() {
            NetworkError error = new NetworkError("Connection refused");

            assertEquals("Connection refused", error.getMessage());
            assertEquals("NETWORK_ERROR", error.getCode());
        }

        @Test
        @DisplayName("Creates with message and cause")
        void createsWithMessageAndCause() {
            Exception cause = new java.net.ConnectException("Host unreachable");
            NetworkError error = new NetworkError("Failed to connect", cause);

            assertEquals("Failed to connect", error.getMessage());
            assertSame(cause, error.getCause());
        }

        @Test
        @DisplayName("Falls back to default message for null")
        void fallsBackToDefaultMessageForNull() {
            NetworkError error = new NetworkError(null);

            assertEquals("Network request failed", error.getMessage());
        }

        @Test
        @DisplayName("Falls back to default message for empty string")
        void fallsBackToDefaultMessageForEmptyString() {
            NetworkError error = new NetworkError("");

            assertEquals("Network request failed", error.getMessage());
        }

        @Test
        @DisplayName("Falls back to default message with cause for null message")
        void fallsBackToDefaultMessageWithCauseForNullMessage() {
            Exception cause = new RuntimeException("Original");
            NetworkError error = new NetworkError(null, cause);

            assertEquals("Network request failed", error.getMessage());
            assertSame(cause, error.getCause());
        }
    }

    @Nested
    @DisplayName("JobStatus Enum Tests")
    class JobStatusTests {

        @Test
        @DisplayName("Values have correct string representations")
        void valuesHaveCorrectStringRepresentations() {
            assertEquals("pending", JobStatus.PENDING.getValue());
            assertEquals("processing", JobStatus.PROCESSING.getValue());
            assertEquals("completed", JobStatus.COMPLETED.getValue());
            assertEquals("failed", JobStatus.FAILED.getValue());
        }

        @Test
        @DisplayName("fromValue parses correctly")
        void fromValueParsesCorrectly() {
            assertEquals(JobStatus.PENDING, JobStatus.fromValue("pending"));
            assertEquals(JobStatus.PROCESSING, JobStatus.fromValue("processing"));
            assertEquals(JobStatus.COMPLETED, JobStatus.fromValue("completed"));
            assertEquals(JobStatus.FAILED, JobStatus.fromValue("failed"));
        }

        @Test
        @DisplayName("fromValue is case insensitive")
        void fromValueIsCaseInsensitive() {
            assertEquals(JobStatus.PENDING, JobStatus.fromValue("PENDING"));
            assertEquals(JobStatus.COMPLETED, JobStatus.fromValue("Completed"));
            assertEquals(JobStatus.FAILED, JobStatus.fromValue("FAILED"));
        }

        @Test
        @DisplayName("fromValue returns null for unknown value")
        void fromValueReturnsNullForUnknownValue() {
            assertNull(JobStatus.fromValue("unknown"));
            assertNull(JobStatus.fromValue("running"));
            assertNull(JobStatus.fromValue(""));
        }

        @Test
        @DisplayName("fromValue returns null for null input")
        void fromValueReturnsNullForNullInput() {
            assertNull(JobStatus.fromValue(null));
        }

        @Test
        @DisplayName("toString returns value")
        void toStringReturnsValue() {
            assertEquals("pending", JobStatus.PENDING.toString());
            assertEquals("completed", JobStatus.COMPLETED.toString());
        }
    }

    @Nested
    @DisplayName("Error Inheritance Tests")
    class ErrorInheritanceTests {

        @Test
        @DisplayName("All errors extend RenamedError")
        void allErrorsExtendRenamedError() {
            assertInstanceOf(RenamedError.class, new AuthenticationError());
            assertInstanceOf(RenamedError.class, new ValidationError("test"));
            assertInstanceOf(RenamedError.class, new InsufficientCreditsError());
            assertInstanceOf(RenamedError.class, new RateLimitError());
            assertInstanceOf(RenamedError.class, new NetworkError());
        }

        @Test
        @DisplayName("All errors are RuntimeExceptions")
        void allErrorsAreRuntimeExceptions() {
            assertInstanceOf(RuntimeException.class, new AuthenticationError());
            assertInstanceOf(RuntimeException.class, new ValidationError("test"));
            assertInstanceOf(RuntimeException.class, new InsufficientCreditsError());
            assertInstanceOf(RuntimeException.class, new RateLimitError());
            assertInstanceOf(RuntimeException.class, new NetworkError());
        }

        @Test
        @DisplayName("Errors can be caught as RenamedError")
        void errorsCanBeCaughtAsRenamedError() {
            try {
                throw new AuthenticationError("test");
            } catch (RenamedError e) {
                assertEquals("AUTHENTICATION_ERROR", e.getCode());
            }

            try {
                throw new ValidationError("test");
            } catch (RenamedError e) {
                assertEquals("VALIDATION_ERROR", e.getCode());
            }
        }
    }
}
