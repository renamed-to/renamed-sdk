namespace Renamed.Sdk.Exceptions;

/// <summary>
/// Exception thrown when request validation fails.
/// </summary>
public class ValidationException : RenamedExceptionBase
{
    private const string ErrorCode = "VALIDATION_ERROR";
    private const int HttpStatusCode = 400;

    /// <summary>
    /// Creates a new instance of ValidationException with a custom message.
    /// </summary>
    /// <param name="message">The error message.</param>
    /// <param name="details">Additional validation error details.</param>
    public ValidationException(string message, object? details = null)
        : base(message, ErrorCode, HttpStatusCode, details)
    {
    }

    /// <summary>
    /// Creates a new instance of ValidationException with a custom message and inner exception.
    /// </summary>
    /// <param name="message">The error message.</param>
    /// <param name="innerException">The inner exception.</param>
    /// <param name="details">Additional validation error details.</param>
    public ValidationException(string message, Exception innerException, object? details = null)
        : base(message, ErrorCode, innerException, HttpStatusCode, details)
    {
    }
}
