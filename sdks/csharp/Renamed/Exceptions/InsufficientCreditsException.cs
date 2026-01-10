namespace Renamed.Sdk.Exceptions;

/// <summary>
/// Exception thrown when the user does not have enough credits to perform an operation.
/// </summary>
public class InsufficientCreditsException : RenamedExceptionBase
{
    private const string DefaultMessage = "Insufficient credits";
    private const string ErrorCode = "INSUFFICIENT_CREDITS";
    private const int HttpStatusCode = 402;

    /// <summary>
    /// Creates a new instance of InsufficientCreditsException with the default message.
    /// </summary>
    public InsufficientCreditsException()
        : base(DefaultMessage, ErrorCode, HttpStatusCode)
    {
    }

    /// <summary>
    /// Creates a new instance of InsufficientCreditsException with a custom message.
    /// </summary>
    /// <param name="message">The error message.</param>
    public InsufficientCreditsException(string message)
        : base(message, ErrorCode, HttpStatusCode)
    {
    }

    /// <summary>
    /// Creates a new instance of InsufficientCreditsException with a custom message and inner exception.
    /// </summary>
    /// <param name="message">The error message.</param>
    /// <param name="innerException">The inner exception.</param>
    public InsufficientCreditsException(string message, Exception innerException)
        : base(message, ErrorCode, innerException, HttpStatusCode)
    {
    }
}
