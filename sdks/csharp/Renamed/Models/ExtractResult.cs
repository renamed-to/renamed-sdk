using System.Text.Json.Serialization;

namespace Renamed.Sdk.Models;

/// <summary>
/// Result of extract operation containing structured data from the document.
/// </summary>
public sealed class ExtractResult
{
    /// <summary>
    /// Extracted data matching the provided schema.
    /// </summary>
    [JsonPropertyName("data")]
    public required IDictionary<string, object?> Data { get; init; }

    /// <summary>
    /// Confidence score (0-1) of the extraction.
    /// </summary>
    [JsonPropertyName("confidence")]
    public double Confidence { get; init; }
}
