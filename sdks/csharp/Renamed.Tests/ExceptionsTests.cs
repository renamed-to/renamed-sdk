using Renamed.Sdk.Exceptions;
using Xunit;

namespace Renamed.Tests;

/// <summary>
/// Tests for SDK exception classes.
/// </summary>
public class ExceptionsTests
{
    #region RenamedExceptionBase Tests

    [Fact]
    public void RenamedExceptionBase_CreatesWithAllProperties()
    {
        // Arrange & Act
        var exception = new RenamedExceptionBase(
            "Test error message",
            "TEST_ERROR",
            500,
            new { field = "value" });

        // Assert
        Assert.Equal("Test error message", exception.Message);
        Assert.Equal("TEST_ERROR", exception.Code);
        Assert.Equal(500, exception.StatusCode);
        Assert.NotNull(exception.Details);
    }

    [Fact]
    public void RenamedExceptionBase_CreatesWithInnerException()
    {
        // Arrange
        var innerException = new InvalidOperationException("Inner error");

        // Act
        var exception = new RenamedExceptionBase(
            "Outer error",
            "OUTER_ERROR",
            innerException,
            503);

        // Assert
        Assert.Equal("Outer error", exception.Message);
        Assert.Equal("OUTER_ERROR", exception.Code);
        Assert.Equal(503, exception.StatusCode);
        Assert.Same(innerException, exception.InnerException);
    }

    [Fact]
    public void RenamedExceptionBase_StatusCodeCanBeNull()
    {
        // Arrange & Act
        var exception = new RenamedExceptionBase("Error", "CODE");

        // Assert
        Assert.Null(exception.StatusCode);
    }

    #endregion

    #region AuthenticationException Tests

    [Fact]
    public void AuthenticationException_DefaultConstructor_HasCorrectDefaults()
    {
        // Arrange & Act
        var exception = new AuthenticationException();

        // Assert
        Assert.Equal("Invalid or missing API key", exception.Message);
        Assert.Equal("AUTHENTICATION_ERROR", exception.Code);
        Assert.Equal(401, exception.StatusCode);
    }

    [Fact]
    public void AuthenticationException_WithMessage_SetsMessage()
    {
        // Arrange & Act
        var exception = new AuthenticationException("Custom auth error");

        // Assert
        Assert.Equal("Custom auth error", exception.Message);
        Assert.Equal("AUTHENTICATION_ERROR", exception.Code);
        Assert.Equal(401, exception.StatusCode);
    }

    [Fact]
    public void AuthenticationException_WithInnerException_SetsInnerException()
    {
        // Arrange
        var innerException = new Exception("Original error");

        // Act
        var exception = new AuthenticationException("Auth failed", innerException);

        // Assert
        Assert.Equal("Auth failed", exception.Message);
        Assert.Equal("AUTHENTICATION_ERROR", exception.Code);
        Assert.Equal(401, exception.StatusCode);
        Assert.Same(innerException, exception.InnerException);
    }

    [Fact]
    public void AuthenticationException_InheritsFromRenamedExceptionBase()
    {
        // Arrange & Act
        var exception = new AuthenticationException();

        // Assert
        Assert.IsAssignableFrom<RenamedExceptionBase>(exception);
        Assert.IsAssignableFrom<Exception>(exception);
    }

    #endregion

    #region InsufficientCreditsException Tests

    [Fact]
    public void InsufficientCreditsException_DefaultConstructor_HasCorrectDefaults()
    {
        // Arrange & Act
        var exception = new InsufficientCreditsException();

        // Assert
        Assert.Equal("Insufficient credits", exception.Message);
        Assert.Equal("INSUFFICIENT_CREDITS", exception.Code);
        Assert.Equal(402, exception.StatusCode);
    }

    [Fact]
    public void InsufficientCreditsException_WithMessage_SetsMessage()
    {
        // Arrange & Act
        var exception = new InsufficientCreditsException("You need 100 more credits");

        // Assert
        Assert.Equal("You need 100 more credits", exception.Message);
        Assert.Equal("INSUFFICIENT_CREDITS", exception.Code);
        Assert.Equal(402, exception.StatusCode);
    }

    [Fact]
    public void InsufficientCreditsException_WithInnerException_SetsInnerException()
    {
        // Arrange
        var innerException = new Exception("Payment service error");

        // Act
        var exception = new InsufficientCreditsException("No credits", innerException);

        // Assert
        Assert.Equal("No credits", exception.Message);
        Assert.Same(innerException, exception.InnerException);
    }

    #endregion

    #region RateLimitException Tests

    [Fact]
    public void RateLimitException_DefaultConstructor_HasCorrectDefaults()
    {
        // Arrange & Act
        var exception = new RateLimitException();

        // Assert
        Assert.Equal("Rate limit exceeded", exception.Message);
        Assert.Equal("RATE_LIMIT_ERROR", exception.Code);
        Assert.Equal(429, exception.StatusCode);
        Assert.Null(exception.RetryAfterSeconds);
    }

    [Fact]
    public void RateLimitException_WithRetryAfter_SetsRetryAfterSeconds()
    {
        // Arrange & Act
        var exception = new RateLimitException("Too many requests", 60);

        // Assert
        Assert.Equal("Too many requests", exception.Message);
        Assert.Equal(429, exception.StatusCode);
        Assert.Equal(60, exception.RetryAfterSeconds);
    }

    [Fact]
    public void RateLimitException_WithInnerException_SetsAllProperties()
    {
        // Arrange
        var innerException = new Exception("Network throttled");

        // Act
        var exception = new RateLimitException("Rate limited", innerException, 30);

        // Assert
        Assert.Equal("Rate limited", exception.Message);
        Assert.Same(innerException, exception.InnerException);
        Assert.Equal(30, exception.RetryAfterSeconds);
    }

    [Theory]
    [InlineData(null)]
    [InlineData(0)]
    [InlineData(1)]
    [InlineData(60)]
    [InlineData(3600)]
    public void RateLimitException_VariousRetryAfterValues(int? retryAfter)
    {
        // Arrange & Act
        var exception = new RateLimitException("Rate limited", retryAfter);

        // Assert
        Assert.Equal(retryAfter, exception.RetryAfterSeconds);
    }

    #endregion

    #region ValidationException Tests

    [Fact]
    public void ValidationException_WithMessage_SetsMessage()
    {
        // Arrange & Act
        var exception = new ValidationException("Invalid file format");

        // Assert
        Assert.Equal("Invalid file format", exception.Message);
        Assert.Equal("VALIDATION_ERROR", exception.Code);
        Assert.Equal(400, exception.StatusCode);
        Assert.Null(exception.Details);
    }

    [Fact]
    public void ValidationException_WithDetails_SetsDetails()
    {
        // Arrange
        var details = new { field = "email", message = "Invalid email format" };

        // Act
        var exception = new ValidationException("Validation failed", details);

        // Assert
        Assert.Equal("Validation failed", exception.Message);
        Assert.NotNull(exception.Details);
    }

    [Fact]
    public void ValidationException_WithInnerException_SetsAllProperties()
    {
        // Arrange
        var innerException = new FormatException("Bad format");
        var details = new[] { "error1", "error2" };

        // Act
        var exception = new ValidationException("Multiple errors", innerException, details);

        // Assert
        Assert.Equal("Multiple errors", exception.Message);
        Assert.Same(innerException, exception.InnerException);
        Assert.NotNull(exception.Details);
    }

    #endregion

    #region NetworkException Tests

    [Fact]
    public void NetworkException_DefaultConstructor_HasCorrectDefaults()
    {
        // Arrange & Act
        var exception = new NetworkException();

        // Assert
        Assert.Equal("Network request failed", exception.Message);
        Assert.Equal("NETWORK_ERROR", exception.Code);
        Assert.Null(exception.StatusCode);
    }

    [Fact]
    public void NetworkException_WithMessage_SetsMessage()
    {
        // Arrange & Act
        var exception = new NetworkException("Connection refused");

        // Assert
        Assert.Equal("Connection refused", exception.Message);
        Assert.Equal("NETWORK_ERROR", exception.Code);
    }

    [Fact]
    public void NetworkException_WithInnerException_SetsInnerException()
    {
        // Arrange
        var innerException = new System.Net.Http.HttpRequestException("DNS lookup failed");

        // Act
        var exception = new NetworkException("Network error", innerException);

        // Assert
        Assert.Equal("Network error", exception.Message);
        Assert.Same(innerException, exception.InnerException);
    }

    #endregion

    #region RenamedTimeoutException Tests

    [Fact]
    public void RenamedTimeoutException_DefaultConstructor_HasCorrectDefaults()
    {
        // Arrange & Act
        var exception = new RenamedTimeoutException();

        // Assert
        Assert.Equal("Request timed out", exception.Message);
        Assert.Equal("TIMEOUT_ERROR", exception.Code);
        Assert.Null(exception.StatusCode);
    }

    [Fact]
    public void RenamedTimeoutException_WithMessage_SetsMessage()
    {
        // Arrange & Act
        var exception = new RenamedTimeoutException("Operation timed out after 30 seconds");

        // Assert
        Assert.Equal("Operation timed out after 30 seconds", exception.Message);
        Assert.Equal("TIMEOUT_ERROR", exception.Code);
    }

    [Fact]
    public void RenamedTimeoutException_WithInnerException_SetsInnerException()
    {
        // Arrange
        var innerException = new TaskCanceledException("The operation was canceled");

        // Act
        var exception = new RenamedTimeoutException("Request timeout", innerException);

        // Assert
        Assert.Equal("Request timeout", exception.Message);
        Assert.Same(innerException, exception.InnerException);
    }

    #endregion

    #region JobException Tests

    [Fact]
    public void JobException_WithMessage_SetsMessage()
    {
        // Arrange & Act
        var exception = new JobException("Job processing failed");

        // Assert
        Assert.Equal("Job processing failed", exception.Message);
        Assert.Equal("JOB_ERROR", exception.Code);
        Assert.Null(exception.JobId);
        Assert.Null(exception.StatusCode);
    }

    [Fact]
    public void JobException_WithJobId_SetsJobId()
    {
        // Arrange & Act
        var exception = new JobException("Job failed", "job_abc123");

        // Assert
        Assert.Equal("Job failed", exception.Message);
        Assert.Equal("job_abc123", exception.JobId);
    }

    [Fact]
    public void JobException_WithInnerException_SetsAllProperties()
    {
        // Arrange
        var innerException = new InvalidOperationException("PDF parse error");

        // Act
        var exception = new JobException("Split operation failed", innerException, "job_xyz789");

        // Assert
        Assert.Equal("Split operation failed", exception.Message);
        Assert.Same(innerException, exception.InnerException);
        Assert.Equal("job_xyz789", exception.JobId);
    }

    [Theory]
    [InlineData(null)]
    [InlineData("")]
    [InlineData("job_12345")]
    [InlineData("job-with-dashes-and-numbers-123")]
    public void JobException_VariousJobIdFormats(string? jobId)
    {
        // Arrange & Act
        var exception = new JobException("Error", jobId);

        // Assert
        Assert.Equal(jobId, exception.JobId);
    }

    #endregion

    #region Exception Hierarchy Tests

    [Fact]
    public void AllExceptions_InheritFromRenamedExceptionBase()
    {
        // Arrange & Act
        RenamedExceptionBase[] exceptions =
        [
            new AuthenticationException(),
            new InsufficientCreditsException(),
            new RateLimitException(),
            new ValidationException("test"),
            new NetworkException(),
            new RenamedTimeoutException(),
            new JobException("test")
        ];

        // Assert
        foreach (var exception in exceptions)
        {
            Assert.IsAssignableFrom<RenamedExceptionBase>(exception);
            Assert.IsAssignableFrom<Exception>(exception);
        }
    }

    [Fact]
    public void AllExceptions_CanBeCaughtAsRenamedExceptionBase()
    {
        // Arrange
        var exceptions = new Action[]
        {
            () => throw new AuthenticationException(),
            () => throw new InsufficientCreditsException(),
            () => throw new RateLimitException(),
            () => throw new ValidationException("test"),
            () => throw new NetworkException(),
            () => throw new RenamedTimeoutException(),
            () => throw new JobException("test")
        };

        // Act & Assert
        foreach (var throwAction in exceptions)
        {
            // Catch as base type and verify the exception has expected properties
            try
            {
                throwAction();
                Assert.Fail("Expected exception to be thrown");
            }
            catch (RenamedExceptionBase ex)
            {
                Assert.NotNull(ex.Code);
            }
        }
    }

    #endregion

    #region Exception Codes Tests

    [Theory]
    [InlineData(typeof(AuthenticationException), "AUTHENTICATION_ERROR")]
    [InlineData(typeof(InsufficientCreditsException), "INSUFFICIENT_CREDITS")]
    [InlineData(typeof(RateLimitException), "RATE_LIMIT_ERROR")]
    [InlineData(typeof(NetworkException), "NETWORK_ERROR")]
    [InlineData(typeof(RenamedTimeoutException), "TIMEOUT_ERROR")]
    public void Exceptions_HaveCorrectErrorCodes(Type exceptionType, string expectedCode)
    {
        // Arrange
        var exception = (RenamedExceptionBase)Activator.CreateInstance(exceptionType)!;

        // Assert
        Assert.Equal(expectedCode, exception.Code);
    }

    [Fact]
    public void JobException_HasCorrectErrorCode()
    {
        // Arrange - JobException requires a message parameter
        var exception = new JobException("test");

        // Assert
        Assert.Equal("JOB_ERROR", exception.Code);
    }

    [Fact]
    public void ValidationException_HasCorrectErrorCode()
    {
        // Arrange & Act
        var exception = new ValidationException("test");

        // Assert
        Assert.Equal("VALIDATION_ERROR", exception.Code);
    }

    #endregion

    #region HTTP Status Code Tests

    [Theory]
    [InlineData(typeof(AuthenticationException), 401)]
    [InlineData(typeof(InsufficientCreditsException), 402)]
    [InlineData(typeof(RateLimitException), 429)]
    public void HttpExceptions_HaveCorrectStatusCodes(Type exceptionType, int expectedStatusCode)
    {
        // Arrange
        var exception = (RenamedExceptionBase)Activator.CreateInstance(exceptionType)!;

        // Assert
        Assert.Equal(expectedStatusCode, exception.StatusCode);
    }

    [Fact]
    public void ValidationException_HasCorrectStatusCode()
    {
        // Arrange & Act
        var exception = new ValidationException("test");

        // Assert
        Assert.Equal(400, exception.StatusCode);
    }

    [Fact]
    public void NetworkException_HasNullStatusCode()
    {
        // Arrange & Act
        var exception = new NetworkException();

        // Assert
        Assert.Null(exception.StatusCode);
    }

    [Fact]
    public void TimeoutException_HasNullStatusCode()
    {
        // Arrange & Act
        var exception = new RenamedTimeoutException();

        // Assert
        Assert.Null(exception.StatusCode);
    }

    [Fact]
    public void JobException_HasNullStatusCode()
    {
        // Arrange & Act
        var exception = new JobException("test");

        // Assert
        Assert.Null(exception.StatusCode);
    }

    #endregion
}
