namespace Renamed.Sdk.Logging;

/// <summary>
/// Simple logging interface for the Renamed SDK.
/// Provides a minimal abstraction for users who don't want to depend on Microsoft.Extensions.Logging.
/// </summary>
public interface IRenamedLogger
{
    /// <summary>
    /// Logs a debug message.
    /// </summary>
    /// <param name="message">The message to log.</param>
    void Log(string message);
}
