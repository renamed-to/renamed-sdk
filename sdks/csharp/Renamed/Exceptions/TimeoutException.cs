namespace Renamed.Sdk.Exceptions;

/// <summary>
/// Exception thrown when a request times out.
/// </summary>
public class RenamedTimeoutException : RenamedExceptionBase
{
    private const string DefaultMessage = "Request timed out";
    private const string ErrorCode = "TIMEOUT_ERROR";

    /// <summary>
    /// Creates a new instance of RenamedTimeoutException with the default message.
    /// </summary>
    public RenamedTimeoutException()
        : base(DefaultMessage, ErrorCode)
    {
    }

    /// <summary>
    /// Creates a new instance of RenamedTimeoutException with a custom message.
    /// </summary>
    /// <param name="message">The error message.</param>
    public RenamedTimeoutException(string message)
        : base(message, ErrorCode)
    {
    }

    /// <summary>
    /// Creates a new instance of RenamedTimeoutException with a custom message and inner exception.
    /// </summary>
    /// <param name="message">The error message.</param>
    /// <param name="innerException">The inner exception.</param>
    public RenamedTimeoutException(string message, Exception innerException)
        : base(message, ErrorCode, innerException)
    {
    }
}
