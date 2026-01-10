namespace Renamed.Sdk.Models;

/// <summary>
/// Options for the extract operation.
/// </summary>
public sealed class ExtractOptions
{
    /// <summary>
    /// Schema defining what to extract.
    /// </summary>
    public IDictionary<string, object?>? Schema { get; init; }

    /// <summary>
    /// Prompt describing what to extract.
    /// </summary>
    public string? Prompt { get; init; }
}
