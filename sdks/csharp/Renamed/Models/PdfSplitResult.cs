using System.Text.Json.Serialization;

namespace Renamed.Sdk.Models;

/// <summary>
/// A single document resulting from PDF split operation.
/// </summary>
public sealed class SplitDocument
{
    /// <summary>
    /// Document index (0-based).
    /// </summary>
    [JsonPropertyName("index")]
    public int Index { get; init; }

    /// <summary>
    /// Suggested filename for this document.
    /// </summary>
    [JsonPropertyName("filename")]
    public required string Filename { get; init; }

    /// <summary>
    /// Page range included in this document (e.g., "1-5").
    /// </summary>
    [JsonPropertyName("pages")]
    public required string Pages { get; init; }

    /// <summary>
    /// URL to download this document.
    /// </summary>
    [JsonPropertyName("downloadUrl")]
    public required string DownloadUrl { get; init; }

    /// <summary>
    /// Size of the document in bytes.
    /// </summary>
    [JsonPropertyName("size")]
    public long Size { get; init; }
}

/// <summary>
/// Result of PDF split operation.
/// </summary>
public sealed class PdfSplitResult
{
    /// <summary>
    /// Original filename of the uploaded PDF.
    /// </summary>
    [JsonPropertyName("originalFilename")]
    public required string OriginalFilename { get; init; }

    /// <summary>
    /// Collection of split documents.
    /// </summary>
    [JsonPropertyName("documents")]
    public required IReadOnlyList<SplitDocument> Documents { get; init; }

    /// <summary>
    /// Total number of pages in the original document.
    /// </summary>
    [JsonPropertyName("totalPages")]
    public int TotalPages { get; init; }
}
