namespace Renamed.Sdk.Models;

/// <summary>
/// Options for the rename operation.
/// </summary>
public sealed class RenameOptions
{
    /// <summary>
    /// Custom template for filename generation.
    /// </summary>
    public string? Template { get; init; }
}
