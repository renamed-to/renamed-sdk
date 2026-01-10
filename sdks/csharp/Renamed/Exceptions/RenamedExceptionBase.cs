namespace Renamed.Sdk.Exceptions;

/// <summary>
/// Base exception class for all renamed.to SDK exceptions.
/// </summary>
public class RenamedExceptionBase : Exception
{
    /// <summary>
    /// Error code identifying the type of error.
    /// </summary>
    public string Code { get; }

    /// <summary>
    /// HTTP status code associated with the error, if applicable.
    /// </summary>
    public int? StatusCode { get; }

    /// <summary>
    /// Additional error details from the API response.
    /// </summary>
    public object? Details { get; }

    /// <summary>
    /// Creates a new instance of RenamedExceptionBase.
    /// </summary>
    /// <param name="message">The error message.</param>
    /// <param name="code">The error code.</param>
    /// <param name="statusCode">The HTTP status code, if applicable.</param>
    /// <param name="details">Additional error details.</param>
    public RenamedExceptionBase(
        string message,
        string code,
        int? statusCode = null,
        object? details = null)
        : base(message)
    {
        Code = code;
        StatusCode = statusCode;
        Details = details;
    }

    /// <summary>
    /// Creates a new instance of RenamedExceptionBase with an inner exception.
    /// </summary>
    /// <param name="message">The error message.</param>
    /// <param name="code">The error code.</param>
    /// <param name="innerException">The inner exception.</param>
    /// <param name="statusCode">The HTTP status code, if applicable.</param>
    /// <param name="details">Additional error details.</param>
    public RenamedExceptionBase(
        string message,
        string code,
        Exception innerException,
        int? statusCode = null,
        object? details = null)
        : base(message, innerException)
    {
        Code = code;
        StatusCode = statusCode;
        Details = details;
    }
}
