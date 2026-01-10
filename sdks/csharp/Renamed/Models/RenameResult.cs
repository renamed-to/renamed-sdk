using System.Text.Json.Serialization;

namespace Renamed.Sdk.Models;

/// <summary>
/// Result of a rename operation containing the AI-suggested filename.
/// </summary>
public sealed class RenameResult
{
    /// <summary>
    /// Original filename that was uploaded.
    /// </summary>
    [JsonPropertyName("originalFilename")]
    public required string OriginalFilename { get; init; }

    /// <summary>
    /// AI-suggested new filename.
    /// </summary>
    [JsonPropertyName("suggestedFilename")]
    public required string SuggestedFilename { get; init; }

    /// <summary>
    /// Suggested folder path for organization.
    /// </summary>
    [JsonPropertyName("folderPath")]
    public string? FolderPath { get; init; }

    /// <summary>
    /// Confidence score (0-1) of the suggestion.
    /// </summary>
    [JsonPropertyName("confidence")]
    public double? Confidence { get; init; }
}
