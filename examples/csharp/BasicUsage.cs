/**
 * Basic usage example for the Renamed C# SDK.
 *
 * This example demonstrates:
 * - Creating a client with an API key
 * - Getting user information and credits
 * - Renaming a file using AI
 * - Handling errors
 *
 * Usage:
 *   RENAMED_API_KEY=rt_... dotnet run invoice.pdf
 */

using Renamed.Sdk;
using Renamed.Sdk.Models;
using Renamed.Sdk.Exceptions;

class Program
{
    static async Task Main(string[] args)
    {
        // Get API key from environment
        var apiKey = Environment.GetEnvironmentVariable("RENAMED_API_KEY");
        if (string.IsNullOrEmpty(apiKey))
        {
            Console.Error.WriteLine("Please set RENAMED_API_KEY environment variable");
            Environment.Exit(1);
        }

        // Check command line arguments
        if (args.Length < 1)
        {
            Console.Error.WriteLine("Usage: dotnet run <file>");
            Environment.Exit(1);
        }

        var filePath = args[0];

        // Create the client
        using var client = new RenamedClient(apiKey);

        try
        {
            // Get user info
            Console.WriteLine("Fetching user info...");
            var user = await client.GetUserAsync();
            Console.WriteLine($"User: {user.Email}");
            Console.WriteLine($"Credits: {user.Credits}");
            Console.WriteLine();

            // Rename a file
            Console.WriteLine($"Renaming: {filePath}");
            var result = await client.RenameAsync(filePath);

            Console.WriteLine("\nResult:");
            Console.WriteLine($"  Original:  {result.OriginalFilename}");
            Console.WriteLine($"  Suggested: {result.SuggestedFilename}");
            if (!string.IsNullOrEmpty(result.FolderPath))
            {
                Console.WriteLine($"  Folder:    {result.FolderPath}");
            }
            if (result.Confidence.HasValue)
            {
                Console.WriteLine($"  Confidence: {result.Confidence.Value * 100:F1}%");
            }
        }
        catch (AuthenticationException ex)
        {
            Console.Error.WriteLine($"Authentication failed: {ex.Message}");
            Console.Error.WriteLine("Please check your API key");
            Environment.Exit(1);
        }
        catch (InsufficientCreditsException ex)
        {
            Console.Error.WriteLine($"Insufficient credits: {ex.Message}");
            Console.Error.WriteLine("Please add more credits at https://renamed.to/dashboard");
            Environment.Exit(1);
        }
        catch (RateLimitException ex)
        {
            Console.Error.WriteLine($"Rate limit exceeded: {ex.Message}");
            Console.Error.WriteLine("Please wait before making more requests");
            Environment.Exit(1);
        }
        catch (ValidationException ex)
        {
            Console.Error.WriteLine($"Validation error: {ex.Message}");
            Console.Error.WriteLine("Please check your file format");
            Environment.Exit(1);
        }
        catch (RenamedExceptionBase ex)
        {
            Console.Error.WriteLine($"Error: {ex.Message}");
            Environment.Exit(1);
        }
    }
}
