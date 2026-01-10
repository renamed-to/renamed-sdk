package to.renamed.sdk;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for model serialization and deserialization with Jackson.
 */
class ModelsTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Nested
    @DisplayName("User Model Tests")
    class UserModelTests {

        @Test
        @DisplayName("Deserializes complete user JSON")
        void deserializesCompleteUserJson() throws JsonProcessingException {
            String json = "{" +
                "\"id\": \"user_abc123\"," +
                "\"email\": \"john.doe@example.com\"," +
                "\"name\": \"John Doe\"," +
                "\"credits\": 250," +
                "\"team\": {" +
                    "\"id\": \"team_xyz789\"," +
                    "\"name\": \"Engineering\"" +
                "}" +
            "}";

            User user = objectMapper.readValue(json, User.class);

            assertEquals("user_abc123", user.getId());
            assertEquals("john.doe@example.com", user.getEmail());
            assertEquals("John Doe", user.getName());
            assertEquals(250, user.getCredits());
            assertNotNull(user.getTeam());
            assertEquals("team_xyz789", user.getTeam().getId());
            assertEquals("Engineering", user.getTeam().getName());
        }

        @Test
        @DisplayName("Deserializes user without team")
        void deserializesUserWithoutTeam() throws JsonProcessingException {
            String json = "{" +
                "\"id\": \"user_abc123\"," +
                "\"email\": \"solo@example.com\"," +
                "\"credits\": 50" +
            "}";

            User user = objectMapper.readValue(json, User.class);

            assertEquals("user_abc123", user.getId());
            assertEquals("solo@example.com", user.getEmail());
            assertNull(user.getName());
            assertEquals(50, user.getCredits());
            assertNull(user.getTeam());
        }

        @Test
        @DisplayName("Ignores unknown properties")
        void ignoresUnknownProperties() throws JsonProcessingException {
            String json = "{" +
                "\"id\": \"user_abc123\"," +
                "\"email\": \"test@example.com\"," +
                "\"credits\": 100," +
                "\"unknownField\": \"should be ignored\"," +
                "\"anotherUnknown\": 12345" +
            "}";

            User user = objectMapper.readValue(json, User.class);

            assertEquals("user_abc123", user.getId());
            assertEquals("test@example.com", user.getEmail());
            assertEquals(100, user.getCredits());
        }

        @Test
        @DisplayName("toString returns formatted string")
        void toStringReturnsFormattedString() {
            User user = new User();
            user.setId("user_123");
            user.setEmail("test@example.com");
            user.setName("Test");
            user.setCredits(100);

            String result = user.toString();

            assertTrue(result.contains("user_123"));
            assertTrue(result.contains("test@example.com"));
            assertTrue(result.contains("Test"));
            assertTrue(result.contains("100"));
        }
    }

    @Nested
    @DisplayName("Team Model Tests")
    class TeamModelTests {

        @Test
        @DisplayName("Deserializes team JSON")
        void deserializesTeamJson() throws JsonProcessingException {
            String json = "{\"id\": \"team_123\", \"name\": \"Product Team\"}";

            Team team = objectMapper.readValue(json, Team.class);

            assertEquals("team_123", team.getId());
            assertEquals("Product Team", team.getName());
        }

        @Test
        @DisplayName("Creates team with constructor")
        void createsTeamWithConstructor() {
            Team team = new Team("team_abc", "Development");

            assertEquals("team_abc", team.getId());
            assertEquals("Development", team.getName());
        }

        @Test
        @DisplayName("toString returns formatted string")
        void toStringReturnsFormattedString() {
            Team team = new Team("team_456", "Sales");

            String result = team.toString();

            assertTrue(result.contains("team_456"));
            assertTrue(result.contains("Sales"));
        }
    }

    @Nested
    @DisplayName("RenameResult Model Tests")
    class RenameResultModelTests {

        @Test
        @DisplayName("Deserializes complete rename result JSON")
        void deserializesCompleteRenameResultJson() throws JsonProcessingException {
            String json = "{" +
                "\"originalFilename\": \"IMG_20240115_143022.jpg\"," +
                "\"suggestedFilename\": \"2024-01-15_Receipt_Starbucks.jpg\"," +
                "\"folderPath\": \"Receipts/Food/2024\"," +
                "\"confidence\": 0.87" +
            "}";

            RenameResult result = objectMapper.readValue(json, RenameResult.class);

            assertEquals("IMG_20240115_143022.jpg", result.getOriginalFilename());
            assertEquals("2024-01-15_Receipt_Starbucks.jpg", result.getSuggestedFilename());
            assertEquals("Receipts/Food/2024", result.getFolderPath());
            assertEquals(0.87, result.getConfidence(), 0.001);
        }

        @Test
        @DisplayName("Deserializes rename result without folder path")
        void deserializesRenameResultWithoutFolderPath() throws JsonProcessingException {
            String json = "{" +
                "\"originalFilename\": \"document.pdf\"," +
                "\"suggestedFilename\": \"Invoice_2024.pdf\"," +
                "\"confidence\": 0.95" +
            "}";

            RenameResult result = objectMapper.readValue(json, RenameResult.class);

            assertEquals("document.pdf", result.getOriginalFilename());
            assertEquals("Invoice_2024.pdf", result.getSuggestedFilename());
            assertNull(result.getFolderPath());
            assertEquals(0.95, result.getConfidence(), 0.001);
        }

        @Test
        @DisplayName("Handles zero confidence")
        void handlesZeroConfidence() throws JsonProcessingException {
            String json = "{" +
                "\"originalFilename\": \"random.pdf\"," +
                "\"suggestedFilename\": \"unknown.pdf\"," +
                "\"confidence\": 0.0" +
            "}";

            RenameResult result = objectMapper.readValue(json, RenameResult.class);

            assertEquals(0.0, result.getConfidence(), 0.001);
        }

        @Test
        @DisplayName("Setters work correctly")
        void settersWorkCorrectly() {
            RenameResult result = new RenameResult();
            result.setOriginalFilename("original.pdf");
            result.setSuggestedFilename("renamed.pdf");
            result.setFolderPath("Documents");
            result.setConfidence(0.88);

            assertEquals("original.pdf", result.getOriginalFilename());
            assertEquals("renamed.pdf", result.getSuggestedFilename());
            assertEquals("Documents", result.getFolderPath());
            assertEquals(0.88, result.getConfidence(), 0.001);
        }
    }

    @Nested
    @DisplayName("ExtractResult Model Tests")
    class ExtractResultModelTests {

        @Test
        @DisplayName("Deserializes extract result with various data types")
        void deserializesExtractResultWithVariousDataTypes() throws JsonProcessingException {
            String json = "{" +
                "\"data\": {" +
                    "\"invoiceNumber\": \"INV-2024-001\"," +
                    "\"total\": 1234.56," +
                    "\"itemCount\": 5," +
                    "\"isPaid\": true," +
                    "\"vendor\": {" +
                        "\"name\": \"ACME Corp\"," +
                        "\"city\": \"New York\"" +
                    "}" +
                "}," +
                "\"confidence\": 0.91" +
            "}";

            ExtractResult result = objectMapper.readValue(json, ExtractResult.class);

            assertNotNull(result.getData());
            assertEquals("INV-2024-001", result.getString("invoiceNumber"));
            assertEquals(1234.56, result.getDouble("total"), 0.001);
            assertEquals(5, result.getInt("itemCount"));
            assertEquals(0.91, result.getConfidence(), 0.001);
        }

        @Test
        @DisplayName("getString returns correct value")
        void getStringReturnsCorrectValue() throws JsonProcessingException {
            String json = "{" +
                "\"data\": {" +
                    "\"name\": \"Test Document\"," +
                    "\"category\": \"Invoice\"" +
                "}," +
                "\"confidence\": 0.85" +
            "}";

            ExtractResult result = objectMapper.readValue(json, ExtractResult.class);

            assertEquals("Test Document", result.getString("name"));
            assertEquals("Invoice", result.getString("category"));
            assertNull(result.getString("nonexistent"));
        }

        @Test
        @DisplayName("getInt handles numeric conversion")
        void getIntHandlesNumericConversion() throws JsonProcessingException {
            String json = "{" +
                "\"data\": {" +
                    "\"count\": 42," +
                    "\"amount\": 100.5" +
                "}," +
                "\"confidence\": 0.9" +
            "}";

            ExtractResult result = objectMapper.readValue(json, ExtractResult.class);

            assertEquals(42, result.getInt("count"));
            assertEquals(100, result.getInt("amount"));
            assertNull(result.getInt("missing"));
        }

        @Test
        @DisplayName("getDouble handles numeric conversion")
        void getDoubleHandlesNumericConversion() throws JsonProcessingException {
            String json = "{" +
                "\"data\": {" +
                    "\"price\": 99.99," +
                    "\"quantity\": 10" +
                "}," +
                "\"confidence\": 0.88" +
            "}";

            ExtractResult result = objectMapper.readValue(json, ExtractResult.class);

            assertEquals(99.99, result.getDouble("price"), 0.001);
            assertEquals(10.0, result.getDouble("quantity"), 0.001);
            assertNull(result.getDouble("missing"));
        }

        @Test
        @DisplayName("get with type returns null for type mismatch")
        void getWithTypeReturnsNullForTypeMismatch() throws JsonProcessingException {
            String json = "{" +
                "\"data\": {" +
                    "\"name\": \"Test\"," +
                    "\"count\": 42" +
                "}," +
                "\"confidence\": 0.9" +
            "}";

            ExtractResult result = objectMapper.readValue(json, ExtractResult.class);

            assertNull(result.get("name", Integer.class));
        }

        @Test
        @DisplayName("Handles null data map")
        void handlesNullDataMap() {
            ExtractResult result = new ExtractResult();

            assertNull(result.getString("anything"));
            assertNull(result.getInt("anything"));
            assertNull(result.getDouble("anything"));
        }
    }

    @Nested
    @DisplayName("RenameOptions Model Tests")
    class RenameOptionsModelTests {

        @Test
        @DisplayName("withTemplate returns same instance for chaining")
        void withTemplateReturnsSameInstanceForChaining() {
            RenameOptions options = new RenameOptions();
            RenameOptions returned = options.withTemplate("{date}_{type}");

            assertSame(options, returned);
            assertEquals("{date}_{type}", options.getTemplate());
        }

        @Test
        @DisplayName("setTemplate sets value")
        void setTemplateSetsValue() {
            RenameOptions options = new RenameOptions();
            options.setTemplate("{description}");

            assertEquals("{description}", options.getTemplate());
        }

        @Test
        @DisplayName("Default values are null")
        void defaultValuesAreNull() {
            RenameOptions options = new RenameOptions();

            assertNull(options.getTemplate());
        }
    }

    @Nested
    @DisplayName("ExtractOptions Model Tests")
    class ExtractOptionsModelTests {

        @Test
        @DisplayName("withPrompt returns same instance for chaining")
        void withPromptReturnsSameInstanceForChaining() {
            ExtractOptions options = new ExtractOptions();
            ExtractOptions returned = options.withPrompt("Extract invoice number");

            assertSame(options, returned);
            assertEquals("Extract invoice number", options.getPrompt());
        }

        @Test
        @DisplayName("withSchema returns same instance for chaining")
        void withSchemaReturnsSameInstanceForChaining() {
            Map<String, Object> schema = Map.of(
                "invoiceNumber", Map.of("type", "string"),
                "total", Map.of("type", "number")
            );

            ExtractOptions options = new ExtractOptions();
            ExtractOptions returned = options.withSchema(schema);

            assertSame(options, returned);
            assertNotNull(options.getSchema());
            assertEquals(2, options.getSchema().size());
        }

        @Test
        @DisplayName("Setters work correctly")
        void settersWorkCorrectly() {
            Map<String, Object> schema = Map.of("field", "value");

            ExtractOptions options = new ExtractOptions();
            options.setPrompt("Test prompt");
            options.setSchema(schema);

            assertEquals("Test prompt", options.getPrompt());
            assertEquals(schema, options.getSchema());
        }
    }

    @Nested
    @DisplayName("PdfSplitResult Model Tests")
    class PdfSplitResultModelTests {

        @Test
        @DisplayName("Deserializes complete PDF split result")
        void deserializesCompletePdfSplitResult() throws JsonProcessingException {
            String json = "{" +
                "\"originalFilename\": \"combined.pdf\"," +
                "\"totalPages\": 15," +
                "\"documents\": [" +
                    "{" +
                        "\"index\": 0," +
                        "\"filename\": \"Invoice_001.pdf\"," +
                        "\"pages\": \"1-5\"," +
                        "\"downloadUrl\": \"https://api.example.com/download/abc\"," +
                        "\"size\": 102400" +
                    "}," +
                    "{" +
                        "\"index\": 1," +
                        "\"filename\": \"Invoice_002.pdf\"," +
                        "\"pages\": \"6-10\"," +
                        "\"downloadUrl\": \"https://api.example.com/download/def\"," +
                        "\"size\": 98304" +
                    "}" +
                "]" +
            "}";

            PdfSplitResult result = objectMapper.readValue(json, PdfSplitResult.class);

            assertEquals("combined.pdf", result.getOriginalFilename());
            assertEquals(15, result.getTotalPages());
            assertNotNull(result.getDocuments());
            assertEquals(2, result.getDocuments().size());

            SplitDocument doc1 = result.getDocuments().get(0);
            assertEquals(0, doc1.getIndex());
            assertEquals("Invoice_001.pdf", doc1.getFilename());
            assertEquals("1-5", doc1.getPages());
            assertEquals("https://api.example.com/download/abc", doc1.getDownloadUrl());
            assertEquals(102400, doc1.getSize());
        }

        @Test
        @DisplayName("Handles empty documents list")
        void handlesEmptyDocumentsList() throws JsonProcessingException {
            String json = "{" +
                "\"originalFilename\": \"empty.pdf\"," +
                "\"totalPages\": 0," +
                "\"documents\": []" +
            "}";

            PdfSplitResult result = objectMapper.readValue(json, PdfSplitResult.class);

            assertEquals("empty.pdf", result.getOriginalFilename());
            assertEquals(0, result.getTotalPages());
            assertTrue(result.getDocuments().isEmpty());
        }
    }

    @Nested
    @DisplayName("SplitDocument Model Tests")
    class SplitDocumentModelTests {

        @Test
        @DisplayName("Deserializes split document JSON")
        void deserializesSplitDocumentJson() throws JsonProcessingException {
            String json = "{" +
                "\"index\": 2," +
                "\"filename\": \"Contract_Part3.pdf\"," +
                "\"pages\": \"11-15\"," +
                "\"downloadUrl\": \"https://api.example.com/files/xyz\"," +
                "\"size\": 51200" +
            "}";

            SplitDocument doc = objectMapper.readValue(json, SplitDocument.class);

            assertEquals(2, doc.getIndex());
            assertEquals("Contract_Part3.pdf", doc.getFilename());
            assertEquals("11-15", doc.getPages());
            assertEquals("https://api.example.com/files/xyz", doc.getDownloadUrl());
            assertEquals(51200, doc.getSize());
        }

        @Test
        @DisplayName("Setters work correctly")
        void settersWorkCorrectly() {
            SplitDocument doc = new SplitDocument();
            doc.setIndex(5);
            doc.setFilename("test.pdf");
            doc.setPages("1-10");
            doc.setDownloadUrl("https://example.com/file");
            doc.setSize(12345);

            assertEquals(5, doc.getIndex());
            assertEquals("test.pdf", doc.getFilename());
            assertEquals("1-10", doc.getPages());
            assertEquals("https://example.com/file", doc.getDownloadUrl());
            assertEquals(12345, doc.getSize());
        }
    }

    @Nested
    @DisplayName("JobStatusResponse Model Tests")
    class JobStatusResponseModelTests {

        @Test
        @DisplayName("Deserializes pending job status")
        void deserializesPendingJobStatus() throws JsonProcessingException {
            String json = "{" +
                "\"jobId\": \"job_abc123\"," +
                "\"status\": \"pending\"," +
                "\"progress\": 0" +
            "}";

            JobStatusResponse response = objectMapper.readValue(json, JobStatusResponse.class);

            assertEquals("job_abc123", response.getJobId());
            assertEquals("pending", response.getStatusValue());
            assertEquals(JobStatus.PENDING, response.getStatus());
            assertEquals(0, response.getProgress());
            assertFalse(response.isComplete());
            assertFalse(response.isSuccessful());
        }

        @Test
        @DisplayName("Deserializes processing job status")
        void deserializesProcessingJobStatus() throws JsonProcessingException {
            String json = "{" +
                "\"jobId\": \"job_abc123\"," +
                "\"status\": \"processing\"," +
                "\"progress\": 45" +
            "}";

            JobStatusResponse response = objectMapper.readValue(json, JobStatusResponse.class);

            assertEquals(JobStatus.PROCESSING, response.getStatus());
            assertEquals(45, response.getProgress());
            assertFalse(response.isComplete());
        }

        @Test
        @DisplayName("Deserializes completed job status with result")
        void deserializesCompletedJobStatusWithResult() throws JsonProcessingException {
            String json = "{" +
                "\"jobId\": \"job_abc123\"," +
                "\"status\": \"completed\"," +
                "\"progress\": 100," +
                "\"result\": {" +
                    "\"originalFilename\": \"document.pdf\"," +
                    "\"totalPages\": 10," +
                    "\"documents\": []" +
                "}" +
            "}";

            JobStatusResponse response = objectMapper.readValue(json, JobStatusResponse.class);

            assertEquals(JobStatus.COMPLETED, response.getStatus());
            assertEquals(100, response.getProgress());
            assertTrue(response.isComplete());
            assertTrue(response.isSuccessful());
            assertNotNull(response.getResult());
            assertEquals("document.pdf", response.getResult().getOriginalFilename());
        }

        @Test
        @DisplayName("Deserializes failed job status with error")
        void deserializesFailedJobStatusWithError() throws JsonProcessingException {
            String json = "{" +
                "\"jobId\": \"job_abc123\"," +
                "\"status\": \"failed\"," +
                "\"progress\": 50," +
                "\"error\": \"Processing failed: corrupted PDF\"" +
            "}";

            JobStatusResponse response = objectMapper.readValue(json, JobStatusResponse.class);

            assertEquals(JobStatus.FAILED, response.getStatus());
            assertEquals("Processing failed: corrupted PDF", response.getError());
            assertTrue(response.isComplete());
            assertFalse(response.isSuccessful());
        }

        @Test
        @DisplayName("Setters work correctly")
        void settersWorkCorrectly() {
            JobStatusResponse response = new JobStatusResponse();
            response.setJobId("job_xyz");
            response.setStatus("processing");
            response.setProgress(75);
            response.setError("Test error");

            PdfSplitResult result = new PdfSplitResult();
            response.setResult(result);

            assertEquals("job_xyz", response.getJobId());
            assertEquals("processing", response.getStatusValue());
            assertEquals(75, response.getProgress());
            assertEquals("Test error", response.getError());
            assertSame(result, response.getResult());
        }
    }
}
