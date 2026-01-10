/**
 * Basic usage example for the Renamed Java SDK.
 *
 * This example demonstrates:
 * - Creating a client with an API key
 * - Getting user information and credits
 * - Renaming a file using AI
 * - Handling errors
 *
 * Usage:
 *   RENAMED_API_KEY=rt_... java BasicUsage.java invoice.pdf
 */

import to.renamed.sdk.RenamedClient;
import to.renamed.sdk.User;
import to.renamed.sdk.RenameResult;
import to.renamed.sdk.AuthenticationError;
import to.renamed.sdk.InsufficientCreditsError;
import to.renamed.sdk.RateLimitError;
import to.renamed.sdk.ValidationError;
import to.renamed.sdk.RenamedError;

import java.nio.file.Path;

public class BasicUsage {

    public static void main(String[] args) {
        // Get API key from environment
        String apiKey = System.getenv("RENAMED_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            System.err.println("Please set RENAMED_API_KEY environment variable");
            System.exit(1);
        }

        // Check command line arguments
        if (args.length < 1) {
            System.err.println("Usage: java BasicUsage <file>");
            System.exit(1);
        }

        String filePath = args[0];

        // Create the client
        RenamedClient client = new RenamedClient(apiKey);

        try {
            // Get user info
            System.out.println("Fetching user info...");
            User user = client.getUser();
            System.out.println("User: " + user.getEmail());
            System.out.println("Credits: " + user.getCredits());
            System.out.println();

            // Rename a file
            System.out.println("Renaming: " + filePath);
            RenameResult result = client.rename(Path.of(filePath), null);

            System.out.println("\nResult:");
            System.out.println("  Original:  " + result.getOriginalFilename());
            System.out.println("  Suggested: " + result.getSuggestedFilename());
            if (result.getFolderPath() != null && !result.getFolderPath().isEmpty()) {
                System.out.println("  Folder:    " + result.getFolderPath());
            }
            if (result.getConfidence() > 0) {
                System.out.printf("  Confidence: %.1f%%%n", result.getConfidence() * 100);
            }

        } catch (AuthenticationError e) {
            System.err.println("Authentication failed: " + e.getMessage());
            System.err.println("Please check your API key");
            System.exit(1);

        } catch (InsufficientCreditsError e) {
            System.err.println("Insufficient credits: " + e.getMessage());
            System.err.println("Please add more credits at https://renamed.to/dashboard");
            System.exit(1);

        } catch (RateLimitError e) {
            System.err.println("Rate limit exceeded: " + e.getMessage());
            System.err.println("Please wait before making more requests");
            System.exit(1);

        } catch (ValidationError e) {
            System.err.println("Validation error: " + e.getMessage());
            System.err.println("Please check your file format");
            System.exit(1);

        } catch (RenamedError e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }
}
