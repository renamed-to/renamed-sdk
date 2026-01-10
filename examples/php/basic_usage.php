<?php

declare(strict_types=1);

/**
 * Basic usage example for the Renamed PHP SDK.
 *
 * This example demonstrates:
 * - Creating a client with an API key
 * - Getting user information and credits
 * - Renaming a file using AI
 * - Handling errors
 *
 * Usage:
 *   RENAMED_API_KEY=rt_... php basic_usage.php invoice.pdf
 */

require_once __DIR__ . '/../../sdks/php/vendor/autoload.php';

use Renamed\Client;
use Renamed\Exceptions\AuthenticationException;
use Renamed\Exceptions\InsufficientCreditsException;
use Renamed\Exceptions\RateLimitException;
use Renamed\Exceptions\ValidationException;
use Renamed\Exceptions\RenamedExceptionBase;

// Get API key from environment
$apiKey = getenv('RENAMED_API_KEY');
if (empty($apiKey)) {
    fwrite(STDERR, "Please set RENAMED_API_KEY environment variable\n");
    exit(1);
}

// Check command line arguments
if ($argc < 2) {
    fwrite(STDERR, "Usage: php basic_usage.php <file>\n");
    exit(1);
}

$filePath = $argv[1];

// Create the client
$client = new Client($apiKey);

try {
    // Get user info
    echo "Fetching user info...\n";
    $user = $client->getUser();
    echo "User: {$user->email}\n";
    echo "Credits: {$user->credits}\n";
    echo "\n";

    // Rename a file
    echo "Renaming: {$filePath}\n";
    $result = $client->rename($filePath);

    echo "\nResult:\n";
    echo "  Original:  {$result->originalFilename}\n";
    echo "  Suggested: {$result->suggestedFilename}\n";
    if ($result->folderPath !== null && $result->folderPath !== '') {
        echo "  Folder:    {$result->folderPath}\n";
    }
    if ($result->confidence !== null) {
        printf("  Confidence: %.1f%%\n", $result->confidence * 100);
    }

} catch (AuthenticationException $e) {
    fwrite(STDERR, "Authentication failed: {$e->getMessage()}\n");
    fwrite(STDERR, "Please check your API key\n");
    exit(1);

} catch (InsufficientCreditsException $e) {
    fwrite(STDERR, "Insufficient credits: {$e->getMessage()}\n");
    fwrite(STDERR, "Please add more credits at https://renamed.to/dashboard\n");
    exit(1);

} catch (RateLimitException $e) {
    fwrite(STDERR, "Rate limit exceeded: {$e->getMessage()}\n");
    fwrite(STDERR, "Please wait before making more requests\n");
    exit(1);

} catch (ValidationException $e) {
    fwrite(STDERR, "Validation error: {$e->getMessage()}\n");
    fwrite(STDERR, "Please check your file format\n");
    exit(1);

} catch (RenamedExceptionBase $e) {
    fwrite(STDERR, "Error: {$e->getMessage()}\n");
    exit(1);
}
