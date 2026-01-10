using System.Text.Json;
using System.Text.Json.Serialization;
using Renamed.Sdk.Models;
using Xunit;

namespace Renamed.Tests;

/// <summary>
/// Tests for model serialization and deserialization.
/// </summary>
public class ModelsTests
{
    private static readonly JsonSerializerOptions JsonOptions = new()
    {
        PropertyNamingPolicy = JsonNamingPolicy.CamelCase,
        DefaultIgnoreCondition = JsonIgnoreCondition.WhenWritingNull,
        Converters = { new JsonStringEnumConverter(JsonNamingPolicy.CamelCase) }
    };

    #region User Model Tests

    [Fact]
    public void User_Deserialize_WithAllFields()
    {
        // Arrange
        var json = """
            {
                "id": "user_abc123",
                "email": "john.doe@example.com",
                "name": "John Doe",
                "credits": 5000,
                "team": {
                    "id": "team_xyz789",
                    "name": "Engineering Team"
                }
            }
            """;

        // Act
        var user = JsonSerializer.Deserialize<User>(json, JsonOptions);

        // Assert
        Assert.NotNull(user);
        Assert.Equal("user_abc123", user.Id);
        Assert.Equal("john.doe@example.com", user.Email);
        Assert.Equal("John Doe", user.Name);
        Assert.Equal(5000, user.Credits);
        Assert.NotNull(user.Team);
        Assert.Equal("team_xyz789", user.Team.Id);
        Assert.Equal("Engineering Team", user.Team.Name);
    }

    [Fact]
    public void User_Deserialize_WithRequiredFieldsOnly()
    {
        // Arrange
        var json = """
            {
                "id": "user_123",
                "email": "minimal@test.com"
            }
            """;

        // Act
        var user = JsonSerializer.Deserialize<User>(json, JsonOptions);

        // Assert
        Assert.NotNull(user);
        Assert.Equal("user_123", user.Id);
        Assert.Equal("minimal@test.com", user.Email);
        Assert.Null(user.Name);
        Assert.Null(user.Credits);
        Assert.Null(user.Team);
    }

    [Fact]
    public void User_Deserialize_WithZeroCredits()
    {
        // Arrange
        var json = """
            {
                "id": "user_123",
                "email": "test@test.com",
                "credits": 0
            }
            """;

        // Act
        var user = JsonSerializer.Deserialize<User>(json, JsonOptions);

        // Assert
        Assert.NotNull(user);
        Assert.Equal(0, user.Credits);
    }

    #endregion

    #region Team Model Tests

    [Fact]
    public void Team_Deserialize_Success()
    {
        // Arrange
        var json = """
            {
                "id": "team_001",
                "name": "Product Team"
            }
            """;

        // Act
        var team = JsonSerializer.Deserialize<Team>(json, JsonOptions);

        // Assert
        Assert.NotNull(team);
        Assert.Equal("team_001", team.Id);
        Assert.Equal("Product Team", team.Name);
    }

    #endregion

    #region RenameResult Model Tests

    [Fact]
    public void RenameResult_Deserialize_WithAllFields()
    {
        // Arrange
        var json = """
            {
                "originalFilename": "scan001.pdf",
                "suggestedFilename": "2025-01-15_Invoice_AcmeCorp_INV-12345.pdf",
                "folderPath": "Invoices/2025/January",
                "confidence": 0.97
            }
            """;

        // Act
        var result = JsonSerializer.Deserialize<RenameResult>(json, JsonOptions);

        // Assert
        Assert.NotNull(result);
        Assert.Equal("scan001.pdf", result.OriginalFilename);
        Assert.Equal("2025-01-15_Invoice_AcmeCorp_INV-12345.pdf", result.SuggestedFilename);
        Assert.Equal("Invoices/2025/January", result.FolderPath);
        Assert.Equal(0.97, result.Confidence);
    }

    [Fact]
    public void RenameResult_Deserialize_WithRequiredFieldsOnly()
    {
        // Arrange
        var json = """
            {
                "originalFilename": "doc.pdf",
                "suggestedFilename": "renamed_doc.pdf"
            }
            """;

        // Act
        var result = JsonSerializer.Deserialize<RenameResult>(json, JsonOptions);

        // Assert
        Assert.NotNull(result);
        Assert.Equal("doc.pdf", result.OriginalFilename);
        Assert.Equal("renamed_doc.pdf", result.SuggestedFilename);
        Assert.Null(result.FolderPath);
        Assert.Null(result.Confidence);
    }

    [Theory]
    [InlineData(0.0)]
    [InlineData(0.5)]
    [InlineData(1.0)]
    public void RenameResult_Deserialize_VariousConfidenceValues(double confidence)
    {
        // Arrange
        var json = $$"""
            {
                "originalFilename": "doc.pdf",
                "suggestedFilename": "renamed.pdf",
                "confidence": {{confidence}}
            }
            """;

        // Act
        var result = JsonSerializer.Deserialize<RenameResult>(json, JsonOptions);

        // Assert
        Assert.NotNull(result);
        Assert.Equal(confidence, result.Confidence);
    }

    #endregion

    #region PdfSplitResult Model Tests

    [Fact]
    public void PdfSplitResult_Deserialize_WithMultipleDocuments()
    {
        // Arrange
        var json = """
            {
                "originalFilename": "combined.pdf",
                "totalPages": 25,
                "documents": [
                    {
                        "index": 0,
                        "filename": "Invoice_1.pdf",
                        "pages": "1-5",
                        "downloadUrl": "https://api.renamed.to/download/abc123",
                        "size": 102400
                    },
                    {
                        "index": 1,
                        "filename": "Invoice_2.pdf",
                        "pages": "6-15",
                        "downloadUrl": "https://api.renamed.to/download/def456",
                        "size": 204800
                    },
                    {
                        "index": 2,
                        "filename": "Invoice_3.pdf",
                        "pages": "16-25",
                        "downloadUrl": "https://api.renamed.to/download/ghi789",
                        "size": 153600
                    }
                ]
            }
            """;

        // Act
        var result = JsonSerializer.Deserialize<PdfSplitResult>(json, JsonOptions);

        // Assert
        Assert.NotNull(result);
        Assert.Equal("combined.pdf", result.OriginalFilename);
        Assert.Equal(25, result.TotalPages);
        Assert.Equal(3, result.Documents.Count);

        Assert.Equal(0, result.Documents[0].Index);
        Assert.Equal("Invoice_1.pdf", result.Documents[0].Filename);
        Assert.Equal("1-5", result.Documents[0].Pages);
        Assert.Equal("https://api.renamed.to/download/abc123", result.Documents[0].DownloadUrl);
        Assert.Equal(102400, result.Documents[0].Size);

        Assert.Equal(1, result.Documents[1].Index);
        Assert.Equal("Invoice_2.pdf", result.Documents[1].Filename);
    }

    [Fact]
    public void PdfSplitResult_Deserialize_WithEmptyDocuments()
    {
        // Arrange
        var json = """
            {
                "originalFilename": "empty.pdf",
                "totalPages": 0,
                "documents": []
            }
            """;

        // Act
        var result = JsonSerializer.Deserialize<PdfSplitResult>(json, JsonOptions);

        // Assert
        Assert.NotNull(result);
        Assert.Equal("empty.pdf", result.OriginalFilename);
        Assert.Equal(0, result.TotalPages);
        Assert.Empty(result.Documents);
    }

    #endregion

    #region SplitDocument Model Tests

    [Fact]
    public void SplitDocument_Deserialize_Success()
    {
        // Arrange
        var json = """
            {
                "index": 5,
                "filename": "Document_5.pdf",
                "pages": "21-25",
                "downloadUrl": "https://api.renamed.to/download/xyz",
                "size": 51200
            }
            """;

        // Act
        var doc = JsonSerializer.Deserialize<SplitDocument>(json, JsonOptions);

        // Assert
        Assert.NotNull(doc);
        Assert.Equal(5, doc.Index);
        Assert.Equal("Document_5.pdf", doc.Filename);
        Assert.Equal("21-25", doc.Pages);
        Assert.Equal("https://api.renamed.to/download/xyz", doc.DownloadUrl);
        Assert.Equal(51200, doc.Size);
    }

    #endregion

    #region ExtractResult Model Tests

    [Fact]
    public void ExtractResult_Deserialize_WithComplexData()
    {
        // Arrange
        var json = """
            {
                "data": {
                    "invoiceNumber": "INV-2025-001",
                    "vendorName": "Acme Corporation",
                    "amount": 1500.75,
                    "date": "2025-01-15",
                    "lineItems": [
                        {"description": "Widget A", "quantity": 10, "price": 50.00},
                        {"description": "Widget B", "quantity": 5, "price": 100.15}
                    ]
                },
                "confidence": 0.89
            }
            """;

        // Act
        var result = JsonSerializer.Deserialize<ExtractResult>(json, JsonOptions);

        // Assert
        Assert.NotNull(result);
        Assert.Equal(0.89, result.Confidence);
        Assert.NotNull(result.Data);
        Assert.True(result.Data.ContainsKey("invoiceNumber"));
        Assert.True(result.Data.ContainsKey("vendorName"));
        Assert.True(result.Data.ContainsKey("amount"));
        Assert.True(result.Data.ContainsKey("lineItems"));
    }

    [Fact]
    public void ExtractResult_Deserialize_WithEmptyData()
    {
        // Arrange
        var json = """
            {
                "data": {},
                "confidence": 0.0
            }
            """;

        // Act
        var result = JsonSerializer.Deserialize<ExtractResult>(json, JsonOptions);

        // Assert
        Assert.NotNull(result);
        Assert.Empty(result.Data);
        Assert.Equal(0.0, result.Confidence);
    }

    #endregion

    #region JobStatus Enum Tests

    [Theory]
    [InlineData("pending", JobStatus.Pending)]
    [InlineData("processing", JobStatus.Processing)]
    [InlineData("completed", JobStatus.Completed)]
    [InlineData("failed", JobStatus.Failed)]
    public void JobStatus_Deserialize_AllValues(string jsonValue, JobStatus expected)
    {
        // Arrange
        var json = $"\"{jsonValue}\"";

        // Act
        var status = JsonSerializer.Deserialize<JobStatus>(json, JsonOptions);

        // Assert
        Assert.Equal(expected, status);
    }

    [Theory]
    [InlineData(JobStatus.Pending, "pending")]
    [InlineData(JobStatus.Processing, "processing")]
    [InlineData(JobStatus.Completed, "completed")]
    [InlineData(JobStatus.Failed, "failed")]
    public void JobStatus_Serialize_AllValues(JobStatus status, string expected)
    {
        // Act
        var json = JsonSerializer.Serialize(status, JsonOptions);

        // Assert
        Assert.Equal($"\"{expected}\"", json);
    }

    #endregion

    #region JobStatusResponse Model Tests

    [Fact]
    public void JobStatusResponse_Deserialize_Pending()
    {
        // Arrange
        var json = """
            {
                "jobId": "job_abc123",
                "status": "pending",
                "progress": 0
            }
            """;

        // Act
        var response = JsonSerializer.Deserialize<JobStatusResponse>(json, JsonOptions);

        // Assert
        Assert.NotNull(response);
        Assert.Equal("job_abc123", response.JobId);
        Assert.Equal(JobStatus.Pending, response.Status);
        Assert.Equal(0, response.Progress);
        Assert.Null(response.Error);
        Assert.Null(response.Result);
    }

    [Fact]
    public void JobStatusResponse_Deserialize_Processing()
    {
        // Arrange
        var json = """
            {
                "jobId": "job_def456",
                "status": "processing",
                "progress": 50
            }
            """;

        // Act
        var response = JsonSerializer.Deserialize<JobStatusResponse>(json, JsonOptions);

        // Assert
        Assert.NotNull(response);
        Assert.Equal("job_def456", response.JobId);
        Assert.Equal(JobStatus.Processing, response.Status);
        Assert.Equal(50, response.Progress);
    }

    [Fact]
    public void JobStatusResponse_Deserialize_CompletedWithResult()
    {
        // Arrange
        var json = """
            {
                "jobId": "job_ghi789",
                "status": "completed",
                "progress": 100,
                "result": {
                    "originalFilename": "document.pdf",
                    "totalPages": 10,
                    "documents": [
                        {
                            "index": 0,
                            "filename": "part1.pdf",
                            "pages": "1-5",
                            "downloadUrl": "https://api.renamed.to/download/part1",
                            "size": 50000
                        }
                    ]
                }
            }
            """;

        // Act
        var response = JsonSerializer.Deserialize<JobStatusResponse>(json, JsonOptions);

        // Assert
        Assert.NotNull(response);
        Assert.Equal("job_ghi789", response.JobId);
        Assert.Equal(JobStatus.Completed, response.Status);
        Assert.Equal(100, response.Progress);
        Assert.NotNull(response.Result);
        Assert.Equal("document.pdf", response.Result.OriginalFilename);
        Assert.Single(response.Result.Documents);
    }

    [Fact]
    public void JobStatusResponse_Deserialize_Failed()
    {
        // Arrange
        var json = """
            {
                "jobId": "job_fail001",
                "status": "failed",
                "progress": 25,
                "error": "PDF parsing failed: corrupted file"
            }
            """;

        // Act
        var response = JsonSerializer.Deserialize<JobStatusResponse>(json, JsonOptions);

        // Assert
        Assert.NotNull(response);
        Assert.Equal("job_fail001", response.JobId);
        Assert.Equal(JobStatus.Failed, response.Status);
        Assert.Equal(25, response.Progress);
        Assert.Equal("PDF parsing failed: corrupted file", response.Error);
        Assert.Null(response.Result);
    }

    #endregion

    #region PdfSplitMode Enum Tests

    [Theory]
    [InlineData("auto", PdfSplitMode.Auto)]
    [InlineData("pages", PdfSplitMode.Pages)]
    [InlineData("blank", PdfSplitMode.Blank)]
    public void PdfSplitMode_Deserialize_AllValues(string jsonValue, PdfSplitMode expected)
    {
        // Arrange
        var json = $"\"{jsonValue}\"";

        // Act
        var mode = JsonSerializer.Deserialize<PdfSplitMode>(json, JsonOptions);

        // Assert
        Assert.Equal(expected, mode);
    }

    [Theory]
    [InlineData(PdfSplitMode.Auto, "auto")]
    [InlineData(PdfSplitMode.Pages, "pages")]
    [InlineData(PdfSplitMode.Blank, "blank")]
    public void PdfSplitMode_Serialize_AllValues(PdfSplitMode mode, string expected)
    {
        // Act
        var json = JsonSerializer.Serialize(mode, JsonOptions);

        // Assert
        Assert.Equal($"\"{expected}\"", json);
    }

    #endregion

    #region Options Model Tests

    [Fact]
    public void RenameOptions_CanBeCreated_WithTemplate()
    {
        // Arrange & Act
        var options = new RenameOptions
        {
            Template = "{date}_{type}_{vendor}"
        };

        // Assert
        Assert.Equal("{date}_{type}_{vendor}", options.Template);
    }

    [Fact]
    public void RenameOptions_CanBeCreated_WithNullTemplate()
    {
        // Arrange & Act
        var options = new RenameOptions();

        // Assert
        Assert.Null(options.Template);
    }

    [Fact]
    public void PdfSplitOptions_CanBeCreated_WithAllFields()
    {
        // Arrange & Act
        var options = new PdfSplitOptions
        {
            Mode = PdfSplitMode.Pages,
            PagesPerSplit = 5
        };

        // Assert
        Assert.Equal(PdfSplitMode.Pages, options.Mode);
        Assert.Equal(5, options.PagesPerSplit);
    }

    [Fact]
    public void PdfSplitOptions_CanBeCreated_WithAutoMode()
    {
        // Arrange & Act
        var options = new PdfSplitOptions
        {
            Mode = PdfSplitMode.Auto
        };

        // Assert
        Assert.Equal(PdfSplitMode.Auto, options.Mode);
        Assert.Null(options.PagesPerSplit);
    }

    [Fact]
    public void ExtractOptions_CanBeCreated_WithSchema()
    {
        // Arrange
        var schema = new Dictionary<string, object?>
        {
            ["invoiceNumber"] = new { type = "string" },
            ["amount"] = new { type = "number" }
        };

        // Act
        var options = new ExtractOptions
        {
            Schema = schema,
            Prompt = "Extract invoice data"
        };

        // Assert
        Assert.NotNull(options.Schema);
        Assert.Equal(2, options.Schema.Count);
        Assert.Equal("Extract invoice data", options.Prompt);
    }

    [Fact]
    public void ExtractOptions_CanBeCreated_WithPromptOnly()
    {
        // Arrange & Act
        var options = new ExtractOptions
        {
            Prompt = "Extract all dates from the document"
        };

        // Assert
        Assert.Null(options.Schema);
        Assert.Equal("Extract all dates from the document", options.Prompt);
    }

    #endregion
}
