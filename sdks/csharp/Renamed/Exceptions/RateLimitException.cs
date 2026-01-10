namespace Renamed.Sdk.Exceptions;

/// <summary>
/// Exception thrown when the rate limit has been exceeded.
/// </summary>
public class RateLimitException : RenamedExceptionBase
{
    private const string DefaultMessage = "Rate limit exceeded";
    private const string ErrorCode = "RATE_LIMIT_ERROR";
    private const int HttpStatusCode = 429;

    /// <summary>
    /// Number of seconds to wait before retrying, if provided by the API.
    /// </summary>
    public int? RetryAfterSeconds { get; }

    /// <summary>
    /// Creates a new instance of RateLimitException with the default message.
    /// </summary>
    public RateLimitException()
        : base(DefaultMessage, ErrorCode, HttpStatusCode)
    {
    }

    /// <summary>
    /// Creates a new instance of RateLimitException with a custom message.
    /// </summary>
    /// <param name="message">The error message.</param>
    /// <param name="retryAfterSeconds">Number of seconds to wait before retrying.</param>
    public RateLimitException(string message, int? retryAfterSeconds = null)
        : base(message, ErrorCode, HttpStatusCode)
    {
        RetryAfterSeconds = retryAfterSeconds;
    }

    /// <summary>
    /// Creates a new instance of RateLimitException with a custom message and inner exception.
    /// </summary>
    /// <param name="message">The error message.</param>
    /// <param name="innerException">The inner exception.</param>
    /// <param name="retryAfterSeconds">Number of seconds to wait before retrying.</param>
    public RateLimitException(string message, Exception innerException, int? retryAfterSeconds = null)
        : base(message, ErrorCode, innerException, HttpStatusCode)
    {
        RetryAfterSeconds = retryAfterSeconds;
    }
}
