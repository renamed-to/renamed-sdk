using Microsoft.Extensions.Logging;

namespace Renamed.Sdk.Logging;

/// <summary>
/// Adapter that wraps Microsoft.Extensions.Logging.ILogger to implement IRenamedLogger.
/// Enables seamless integration with ASP.NET Core and other DI-based logging scenarios.
/// </summary>
public sealed class MicrosoftLoggerAdapter : IRenamedLogger
{
    private readonly ILogger _logger;

    /// <summary>
    /// Creates a new adapter wrapping the specified Microsoft.Extensions.Logging.ILogger.
    /// </summary>
    /// <param name="logger">The Microsoft logger to wrap.</param>
    public MicrosoftLoggerAdapter(ILogger logger)
    {
        _logger = logger ?? throw new ArgumentNullException(nameof(logger));
    }

    /// <summary>
    /// Creates a new adapter from an ILoggerFactory.
    /// </summary>
    /// <param name="loggerFactory">The logger factory.</param>
    /// <returns>A new MicrosoftLoggerAdapter instance.</returns>
    public static MicrosoftLoggerAdapter FromFactory(ILoggerFactory loggerFactory)
    {
        ArgumentNullException.ThrowIfNull(loggerFactory);
        return new MicrosoftLoggerAdapter(loggerFactory.CreateLogger("Renamed"));
    }

    /// <inheritdoc />
    public void Log(string message)
    {
        _logger.LogDebug("{Message}", message);
    }
}
