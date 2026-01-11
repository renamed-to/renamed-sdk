namespace Renamed.Sdk.Logging;

/// <summary>
/// Default console-based logger implementation.
/// Used when Debug=true and no custom logger is provided.
/// </summary>
internal sealed class ConsoleLogger : IRenamedLogger
{
    /// <summary>
    /// Singleton instance of the console logger.
    /// </summary>
    public static readonly ConsoleLogger Instance = new();

    private ConsoleLogger()
    {
    }

    /// <inheritdoc />
    public void Log(string message)
    {
        Console.WriteLine(message);
    }
}
