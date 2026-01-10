using System.Text.Json.Serialization;

namespace Renamed.Sdk.Models;

/// <summary>
/// Split mode for PDF splitting operation.
/// </summary>
[JsonConverter(typeof(JsonStringEnumConverter))]
public enum PdfSplitMode
{
    /// <summary>
    /// AI automatically detects document boundaries.
    /// </summary>
    [JsonPropertyName("auto")]
    Auto,

    /// <summary>
    /// Split every N pages.
    /// </summary>
    [JsonPropertyName("pages")]
    Pages,

    /// <summary>
    /// Split at blank pages.
    /// </summary>
    [JsonPropertyName("blank")]
    Blank
}

/// <summary>
/// Options for PDF split operation.
/// </summary>
public sealed class PdfSplitOptions
{
    /// <summary>
    /// Split mode: 'Auto' (AI-detected), 'Pages' (every N pages), 'Blank' (at blank pages).
    /// </summary>
    public PdfSplitMode? Mode { get; init; }

    /// <summary>
    /// Number of pages per split (for 'Pages' mode).
    /// </summary>
    public int? PagesPerSplit { get; init; }
}
