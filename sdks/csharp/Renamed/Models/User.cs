using System.Text.Json.Serialization;

namespace Renamed.Sdk.Models;

/// <summary>
/// Team information associated with a user.
/// </summary>
public sealed class Team
{
    /// <summary>
    /// Unique team identifier.
    /// </summary>
    [JsonPropertyName("id")]
    public required string Id { get; init; }

    /// <summary>
    /// Team display name.
    /// </summary>
    [JsonPropertyName("name")]
    public required string Name { get; init; }
}

/// <summary>
/// User profile and account information.
/// </summary>
public sealed class User
{
    /// <summary>
    /// Unique user identifier.
    /// </summary>
    [JsonPropertyName("id")]
    public required string Id { get; init; }

    /// <summary>
    /// User's email address.
    /// </summary>
    [JsonPropertyName("email")]
    public required string Email { get; init; }

    /// <summary>
    /// User's display name.
    /// </summary>
    [JsonPropertyName("name")]
    public string? Name { get; init; }

    /// <summary>
    /// Available credits for API operations.
    /// </summary>
    [JsonPropertyName("credits")]
    public int? Credits { get; init; }

    /// <summary>
    /// Team information if the user belongs to a team.
    /// </summary>
    [JsonPropertyName("team")]
    public Team? Team { get; init; }
}
