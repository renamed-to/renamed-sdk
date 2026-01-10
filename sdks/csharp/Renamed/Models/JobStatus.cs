using System.Text.Json.Serialization;

namespace Renamed.Sdk.Models;

/// <summary>
/// Status of an async job.
/// </summary>
[JsonConverter(typeof(JsonStringEnumConverter))]
public enum JobStatus
{
    /// <summary>
    /// Job is waiting to be processed.
    /// </summary>
    [JsonPropertyName("pending")]
    Pending,

    /// <summary>
    /// Job is currently being processed.
    /// </summary>
    [JsonPropertyName("processing")]
    Processing,

    /// <summary>
    /// Job completed successfully.
    /// </summary>
    [JsonPropertyName("completed")]
    Completed,

    /// <summary>
    /// Job failed with an error.
    /// </summary>
    [JsonPropertyName("failed")]
    Failed
}

/// <summary>
/// Response from job status endpoint.
/// </summary>
public sealed class JobStatusResponse
{
    /// <summary>
    /// Unique job identifier.
    /// </summary>
    [JsonPropertyName("jobId")]
    public required string JobId { get; init; }

    /// <summary>
    /// Current job status.
    /// </summary>
    [JsonPropertyName("status")]
    public JobStatus Status { get; init; }

    /// <summary>
    /// Progress percentage (0-100).
    /// </summary>
    [JsonPropertyName("progress")]
    public int? Progress { get; init; }

    /// <summary>
    /// Error message if job failed.
    /// </summary>
    [JsonPropertyName("error")]
    public string? Error { get; init; }

    /// <summary>
    /// Result data when job is completed.
    /// </summary>
    [JsonPropertyName("result")]
    public PdfSplitResult? Result { get; init; }
}
