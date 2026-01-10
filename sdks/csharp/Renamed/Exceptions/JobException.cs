namespace Renamed.Sdk.Exceptions;

/// <summary>
/// Exception thrown when an async job fails.
/// </summary>
public class JobException : RenamedExceptionBase
{
    private const string ErrorCode = "JOB_ERROR";

    /// <summary>
    /// The ID of the failed job, if available.
    /// </summary>
    public string? JobId { get; }

    /// <summary>
    /// Creates a new instance of JobException with a custom message.
    /// </summary>
    /// <param name="message">The error message.</param>
    /// <param name="jobId">The ID of the failed job.</param>
    public JobException(string message, string? jobId = null)
        : base(message, ErrorCode)
    {
        JobId = jobId;
    }

    /// <summary>
    /// Creates a new instance of JobException with a custom message and inner exception.
    /// </summary>
    /// <param name="message">The error message.</param>
    /// <param name="innerException">The inner exception.</param>
    /// <param name="jobId">The ID of the failed job.</param>
    public JobException(string message, Exception innerException, string? jobId = null)
        : base(message, ErrorCode, innerException)
    {
        JobId = jobId;
    }
}
