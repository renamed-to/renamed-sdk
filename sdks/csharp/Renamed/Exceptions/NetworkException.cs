namespace Renamed.Sdk.Exceptions;

/// <summary>
/// Exception thrown when a network error occurs during API communication.
/// </summary>
public class NetworkException : RenamedExceptionBase
{
    private const string DefaultMessage = "Network request failed";
    private const string ErrorCode = "NETWORK_ERROR";

    /// <summary>
    /// Creates a new instance of NetworkException with the default message.
    /// </summary>
    public NetworkException()
        : base(DefaultMessage, ErrorCode)
    {
    }

    /// <summary>
    /// Creates a new instance of NetworkException with a custom message.
    /// </summary>
    /// <param name="message">The error message.</param>
    public NetworkException(string message)
        : base(message, ErrorCode)
    {
    }

    /// <summary>
    /// Creates a new instance of NetworkException with a custom message and inner exception.
    /// </summary>
    /// <param name="message">The error message.</param>
    /// <param name="innerException">The inner exception.</param>
    public NetworkException(string message, Exception innerException)
        : base(message, ErrorCode, innerException)
    {
    }
}
