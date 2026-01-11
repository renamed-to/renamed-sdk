using Renamed.Sdk.Logging;

namespace Renamed.Sdk;

/// <summary>
/// Configuration options for the RenamedClient.
/// </summary>
public sealed class RenamedClientOptions
{
    /// <summary>
    /// API key for authentication (starts with rt_).
    /// </summary>
    public required string ApiKey { get; init; }

    /// <summary>
    /// Base URL for the API.
    /// Default: https://www.renamed.to/api/v1
    /// </summary>
    public string BaseUrl { get; init; } = "https://www.renamed.to/api/v1";

    /// <summary>
    /// Request timeout.
    /// Default: 30 seconds.
    /// </summary>
    public TimeSpan Timeout { get; init; } = TimeSpan.FromSeconds(30);

    /// <summary>
    /// Maximum number of retries for failed requests.
    /// Default: 2.
    /// </summary>
    public int MaxRetries { get; init; } = 2;

    /// <summary>
    /// Custom HttpClient to use for requests.
    /// If not provided, a new HttpClient will be created.
    /// </summary>
    public HttpClient? HttpClient { get; init; }

    /// <summary>
    /// Enable debug logging.
    /// When true, logs HTTP requests, responses, retries, uploads, and job polling.
    /// Default: false.
    /// </summary>
    public bool Debug { get; init; }

    /// <summary>
    /// Custom logger for debug output.
    /// When Debug=true and Logger is null, uses Console.WriteLine.
    /// For Microsoft.Extensions.Logging integration, use MicrosoftLoggerAdapter.
    /// </summary>
    /// <example>
    /// <code>
    /// // Using custom logger
    /// var options = new RenamedClientOptions
    /// {
    ///     ApiKey = "rt_xxx",
    ///     Debug = true,
    ///     Logger = new MicrosoftLoggerAdapter(loggerFactory.CreateLogger("Renamed"))
    /// };
    /// </code>
    /// </example>
    public IRenamedLogger? Logger { get; init; }
}
