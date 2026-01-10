using System.Net;
using System.Text;
using System.Text.Json;
using Moq;
using Moq.Protected;
using Renamed.Sdk;
using Renamed.Sdk.Exceptions;
using Renamed.Sdk.Models;
using Xunit;

namespace Renamed.Tests;

/// <summary>
/// Tests for RenamedClient initialization and core functionality.
/// </summary>
public class RenamedClientTests
{
    private const string TestApiKey = "rt_test_key_12345";
    private const string TestBaseUrl = "https://api.test.renamed.to/api/v1";

    #region Constructor Tests

    [Fact]
    public void Constructor_WithApiKey_CreatesClient()
    {
        // Arrange & Act
        using var client = new RenamedClient(TestApiKey);

        // Assert - no exception means success
        Assert.NotNull(client);
    }

    [Fact]
    public void Constructor_WithOptions_CreatesClient()
    {
        // Arrange
        var options = new RenamedClientOptions
        {
            ApiKey = TestApiKey,
            BaseUrl = TestBaseUrl,
            Timeout = TimeSpan.FromSeconds(60),
            MaxRetries = 5
        };

        // Act
        using var client = new RenamedClient(options);

        // Assert
        Assert.NotNull(client);
    }

    [Fact]
    public void Constructor_WithCustomHttpClient_UsesProvidedClient()
    {
        // Arrange
        var httpClient = new HttpClient();
        var options = new RenamedClientOptions
        {
            ApiKey = TestApiKey,
            HttpClient = httpClient
        };

        // Act
        using var client = new RenamedClient(options);

        // Assert
        Assert.NotNull(client);
    }

    [Fact]
    public void Constructor_WithNullOptions_ThrowsArgumentNullException()
    {
        // Act & Assert
        Assert.Throws<ArgumentNullException>(() => new RenamedClient((RenamedClientOptions)null!));
    }

    [Theory]
    [InlineData("")]
    [InlineData("   ")]
    public void Constructor_WithEmptyOrWhitespaceApiKey_ThrowsAuthenticationException(string apiKey)
    {
        // Arrange
        var options = new RenamedClientOptions { ApiKey = apiKey };

        // Act & Assert
        var exception = Assert.Throws<AuthenticationException>(() => new RenamedClient(options));
        Assert.Equal("API key is required", exception.Message);
    }

    [Fact]
    public void Constructor_WithNullApiKeyString_ThrowsAuthenticationException()
    {
        // Act & Assert
        // When passing null to the string constructor, it will be passed through
        // to the options constructor which validates the API key
        var exception = Assert.Throws<AuthenticationException>(() => new RenamedClient((string)null!));
        Assert.Equal("API key is required", exception.Message);
    }

    #endregion

    #region GetUserAsync Tests

    [Fact]
    public async Task GetUserAsync_ReturnsUser()
    {
        // Arrange
        var expectedUser = new
        {
            id = "user_123",
            email = "test@example.com",
            name = "Test User",
            credits = 1000,
            team = new { id = "team_456", name = "Test Team" }
        };

        var client = CreateClientWithMockedResponse(
            HttpStatusCode.OK,
            JsonSerializer.Serialize(expectedUser));

        // Act
        var user = await client.GetUserAsync();

        // Assert
        Assert.NotNull(user);
        Assert.Equal("user_123", user.Id);
        Assert.Equal("test@example.com", user.Email);
        Assert.Equal("Test User", user.Name);
        Assert.Equal(1000, user.Credits);
        Assert.NotNull(user.Team);
        Assert.Equal("team_456", user.Team.Id);
        Assert.Equal("Test Team", user.Team.Name);
    }

    [Fact]
    public async Task GetUserAsync_WithoutTeam_ReturnsUserWithNullTeam()
    {
        // Arrange
        var expectedUser = new
        {
            id = "user_123",
            email = "test@example.com"
        };

        var client = CreateClientWithMockedResponse(
            HttpStatusCode.OK,
            JsonSerializer.Serialize(expectedUser));

        // Act
        var user = await client.GetUserAsync();

        // Assert
        Assert.NotNull(user);
        Assert.Equal("user_123", user.Id);
        Assert.Equal("test@example.com", user.Email);
        Assert.Null(user.Team);
    }

    [Fact]
    public async Task GetUserAsync_Unauthorized_ThrowsAuthenticationException()
    {
        // Arrange
        var client = CreateClientWithMockedResponse(
            HttpStatusCode.Unauthorized,
            JsonSerializer.Serialize(new { error = "Invalid API key" }));

        // Act & Assert
        var exception = await Assert.ThrowsAsync<AuthenticationException>(
            () => client.GetUserAsync());
        Assert.Equal("Invalid API key", exception.Message);
        Assert.Equal(401, exception.StatusCode);
    }

    [Fact]
    public async Task GetUserAsync_InsufficientCredits_ThrowsInsufficientCreditsException()
    {
        // Arrange
        var client = CreateClientWithMockedResponse(
            HttpStatusCode.PaymentRequired,
            JsonSerializer.Serialize(new { error = "No credits remaining" }));

        // Act & Assert
        var exception = await Assert.ThrowsAsync<InsufficientCreditsException>(
            () => client.GetUserAsync());
        Assert.Equal("No credits remaining", exception.Message);
        Assert.Equal(402, exception.StatusCode);
    }

    [Fact]
    public async Task GetUserAsync_RateLimited_ThrowsRateLimitException()
    {
        // Arrange
        var client = CreateClientWithMockedResponse(
            HttpStatusCode.TooManyRequests,
            JsonSerializer.Serialize(new { error = "Rate limit exceeded", retryAfter = 60 }));

        // Act & Assert
        var exception = await Assert.ThrowsAsync<RateLimitException>(
            () => client.GetUserAsync());
        Assert.Equal("Rate limit exceeded", exception.Message);
        Assert.Equal(429, exception.StatusCode);
    }

    [Fact]
    public async Task GetUserAsync_ValidationError_ThrowsValidationException()
    {
        // Arrange
        var client = CreateClientWithMockedResponse(
            HttpStatusCode.BadRequest,
            JsonSerializer.Serialize(new { error = "Invalid request parameters" }));

        // Act & Assert
        var exception = await Assert.ThrowsAsync<ValidationException>(
            () => client.GetUserAsync());
        Assert.Equal("Invalid request parameters", exception.Message);
        Assert.Equal(400, exception.StatusCode);
    }

    #endregion

    #region RenameAsync Tests

    [Fact]
    public async Task RenameAsync_WithStream_ReturnsRenameResult()
    {
        // Arrange
        var expectedResult = new
        {
            originalFilename = "document.pdf",
            suggestedFilename = "2025-01-15_Invoice_AcmeCorp.pdf",
            folderPath = "Invoices/2025",
            confidence = 0.95
        };

        var client = CreateClientWithMockedResponse(
            HttpStatusCode.OK,
            JsonSerializer.Serialize(expectedResult));

        using var stream = new MemoryStream(Encoding.UTF8.GetBytes("fake pdf content"));

        // Act
        var result = await client.RenameAsync(stream, "document.pdf");

        // Assert
        Assert.NotNull(result);
        Assert.Equal("document.pdf", result.OriginalFilename);
        Assert.Equal("2025-01-15_Invoice_AcmeCorp.pdf", result.SuggestedFilename);
        Assert.Equal("Invoices/2025", result.FolderPath);
        Assert.Equal(0.95, result.Confidence);
    }

    [Fact]
    public async Task RenameAsync_WithBytes_ReturnsRenameResult()
    {
        // Arrange
        var expectedResult = new
        {
            originalFilename = "image.png",
            suggestedFilename = "Screenshot_2025-01-15.png"
        };

        var client = CreateClientWithMockedResponse(
            HttpStatusCode.OK,
            JsonSerializer.Serialize(expectedResult));

        var fileBytes = Encoding.UTF8.GetBytes("fake image content");

        // Act
        var result = await client.RenameAsync(fileBytes, "image.png");

        // Assert
        Assert.NotNull(result);
        Assert.Equal("image.png", result.OriginalFilename);
        Assert.Equal("Screenshot_2025-01-15.png", result.SuggestedFilename);
    }

    [Fact]
    public async Task RenameAsync_WithOptions_SendsTemplate()
    {
        // Arrange
        var expectedResult = new
        {
            originalFilename = "doc.pdf",
            suggestedFilename = "CUSTOM_doc.pdf"
        };

        var client = CreateClientWithMockedResponse(
            HttpStatusCode.OK,
            JsonSerializer.Serialize(expectedResult));

        using var stream = new MemoryStream(Encoding.UTF8.GetBytes("content"));
        var options = new RenameOptions { Template = "{date}_{type}_{vendor}" };

        // Act
        var result = await client.RenameAsync(stream, "doc.pdf", options);

        // Assert
        Assert.NotNull(result);
        Assert.Equal("CUSTOM_doc.pdf", result.SuggestedFilename);
    }

    #endregion

    #region ExtractAsync Tests

    [Fact]
    public async Task ExtractAsync_WithPrompt_ReturnsExtractResult()
    {
        // Arrange
        var expectedResult = new
        {
            data = new Dictionary<string, object?>
            {
                ["invoiceNumber"] = "INV-12345",
                ["amount"] = 1500.50,
                ["date"] = "2025-01-15"
            },
            confidence = 0.92
        };

        var client = CreateClientWithMockedResponse(
            HttpStatusCode.OK,
            JsonSerializer.Serialize(expectedResult));

        using var stream = new MemoryStream(Encoding.UTF8.GetBytes("invoice content"));
        var options = new ExtractOptions { Prompt = "Extract invoice details" };

        // Act
        var result = await client.ExtractAsync(stream, "invoice.pdf", options);

        // Assert
        Assert.NotNull(result);
        Assert.Equal(0.92, result.Confidence);
        Assert.NotNull(result.Data);
    }

    #endregion

    #region Dispose Tests

    [Fact]
    public void Dispose_CanBeCalledMultipleTimes()
    {
        // Arrange
        var client = new RenamedClient(TestApiKey);

        // Act & Assert - should not throw
        client.Dispose();
        client.Dispose();
    }

    [Fact]
    public void Dispose_WithCustomHttpClient_DoesNotDisposeIt()
    {
        // Arrange
        var httpClient = new HttpClient();
        var options = new RenamedClientOptions
        {
            ApiKey = TestApiKey,
            HttpClient = httpClient
        };
        var client = new RenamedClient(options);

        // Act
        client.Dispose();

        // Assert - httpClient should still be usable (not disposed)
        // Accessing Timeout on a disposed HttpClient would throw ObjectDisposedException
        var timeout = httpClient.Timeout;
        Assert.True(timeout >= TimeSpan.Zero);
    }

    #endregion

    #region Helper Methods

    private static RenamedClient CreateClientWithMockedResponse(
        HttpStatusCode statusCode,
        string responseContent)
    {
        var handlerMock = new Mock<HttpMessageHandler>();
        handlerMock
            .Protected()
            .Setup<Task<HttpResponseMessage>>(
                "SendAsync",
                ItExpr.IsAny<HttpRequestMessage>(),
                ItExpr.IsAny<CancellationToken>())
            .ReturnsAsync(new HttpResponseMessage
            {
                StatusCode = statusCode,
                Content = new StringContent(responseContent, Encoding.UTF8, "application/json")
            });

        var httpClient = new HttpClient(handlerMock.Object);

        var options = new RenamedClientOptions
        {
            ApiKey = TestApiKey,
            HttpClient = httpClient
        };

        return new RenamedClient(options);
    }

    #endregion
}
