namespace Renamed.Sdk.Exceptions;

/// <summary>
/// Exception thrown when authentication fails due to an invalid or missing API key.
/// </summary>
public class AuthenticationException : RenamedExceptionBase
{
    private const string DefaultMessage = "Invalid or missing API key";
    private const string ErrorCode = "AUTHENTICATION_ERROR";
    private const int HttpStatusCode = 401;

    /// <summary>
    /// Creates a new instance of AuthenticationException with the default message.
    /// </summary>
    public AuthenticationException()
        : base(DefaultMessage, ErrorCode, HttpStatusCode)
    {
    }

    /// <summary>
    /// Creates a new instance of AuthenticationException with a custom message.
    /// </summary>
    /// <param name="message">The error message.</param>
    public AuthenticationException(string message)
        : base(message, ErrorCode, HttpStatusCode)
    {
    }

    /// <summary>
    /// Creates a new instance of AuthenticationException with a custom message and inner exception.
    /// </summary>
    /// <param name="message">The error message.</param>
    /// <param name="innerException">The inner exception.</param>
    public AuthenticationException(string message, Exception innerException)
        : base(message, ErrorCode, innerException, HttpStatusCode)
    {
    }
}
