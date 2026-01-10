/**
 * Basic usage example for the Renamed Swift SDK.
 *
 * This example demonstrates:
 * - Creating a client with an API key
 * - Getting user information and credits
 * - Renaming a file using AI
 * - Handling errors
 *
 * Usage:
 *   RENAMED_API_KEY=rt_... swift run BasicUsage invoice.pdf
 */

import Foundation
import Renamed

@main
struct BasicUsage {
    static func main() async {
        // Get API key from environment
        guard let apiKey = ProcessInfo.processInfo.environment["RENAMED_API_KEY"],
              !apiKey.isEmpty else {
            fputs("Please set RENAMED_API_KEY environment variable\n", stderr)
            exit(1)
        }

        // Check command line arguments
        let args = CommandLine.arguments
        guard args.count > 1 else {
            fputs("Usage: swift run BasicUsage <file>\n", stderr)
            exit(1)
        }

        let filePath = args[1]

        do {
            // Create the client
            let client = try RenamedClient(apiKey: apiKey)

            // Get user info
            print("Fetching user info...")
            let user = try await client.getUser()
            print("User: \(user.email)")
            print("Credits: \(user.credits ?? 0)")
            print()

            // Rename a file
            print("Renaming: \(filePath)")
            let fileUrl = URL(fileURLWithPath: filePath)
            let file = try FileInput(url: fileUrl)
            let result = try await client.rename(file: file)

            print("\nResult:")
            print("  Original:  \(result.originalFilename)")
            print("  Suggested: \(result.suggestedFilename)")
            if let folder = result.folderPath, !folder.isEmpty {
                print("  Folder:    \(folder)")
            }
            if let confidence = result.confidence {
                print(String(format: "  Confidence: %.1f%%", confidence * 100))
            }

        } catch let error as RenamedError {
            handleError(error)
        } catch {
            fputs("Error: \(error.localizedDescription)\n", stderr)
            exit(1)
        }
    }

    static func handleError(_ error: RenamedError) {
        switch error {
        case .authentication(let message):
            fputs("Authentication failed: \(message)\n", stderr)
            fputs("Please check your API key\n", stderr)

        case .insufficientCredits(let message):
            fputs("Insufficient credits: \(message)\n", stderr)
            fputs("Please add more credits at https://renamed.to/dashboard\n", stderr)

        case .rateLimit(let message, let retryAfter):
            fputs("Rate limit exceeded: \(message)\n", stderr)
            if let seconds = retryAfter {
                fputs("Retry after \(seconds) seconds\n", stderr)
            }

        case .validation(let message, _):
            fputs("Validation error: \(message)\n", stderr)
            fputs("Please check your file format\n", stderr)

        default:
            fputs("Error: \(error.localizedDescription)\n", stderr)
        }
        exit(1)
    }
}
