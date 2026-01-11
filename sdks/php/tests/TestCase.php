<?php

declare(strict_types=1);

namespace Renamed\Tests;

use GuzzleHttp\Client as GuzzleClient;
use GuzzleHttp\Handler\MockHandler;
use GuzzleHttp\HandlerStack;
use GuzzleHttp\Psr7\Response;
use PHPUnit\Framework\TestCase as BaseTestCase;
use Renamed\Client;
use ReflectionClass;

/**
 * Base test case with mock HTTP helpers.
 */
abstract class TestCase extends BaseTestCase
{
    /**
     * Create a Client with mocked HTTP responses.
     *
     * @param array<Response> $responses Queue of responses
     * @param MockHandler|null $mockHandler Reference to get the mock handler
     */
    protected function createMockClient(
        array $responses,
        ?MockHandler &$mockHandler = null,
    ): Client {
        $mockHandler = new MockHandler($responses);
        $handlerStack = HandlerStack::create($mockHandler);
        $httpClient = new GuzzleClient([
            'handler' => $handlerStack,
            'headers' => [
                'Authorization' => 'Bearer rt_test_api_key',
            ],
        ]);

        // Create client with maxRetries=0 for predictable testing
        $client = new Client(
            'rt_test_api_key',
            'https://www.renamed.to/api/v1',
            30.0,
            0, // No retries for testing
        );

        // Use reflection to inject the mock HTTP client
        $reflection = new ReflectionClass($client);
        $property = $reflection->getProperty('httpClient');
        $property->setValue($client, $httpClient);

        return $client;
    }

    /**
     * Create a JSON response.
     *
     * @param int $status HTTP status code
     * @param array<string, mixed> $body Response body
     * @param array<string, string> $headers Additional headers
     */
    protected function jsonResponse(int $status, array $body, array $headers = []): Response
    {
        return new Response(
            $status,
            array_merge(['Content-Type' => 'application/json'], $headers),
            json_encode($body),
        );
    }

    /**
     * Create a binary response.
     *
     * @param int $status HTTP status code
     * @param string $body Response body
     * @param string $contentType Content type
     */
    protected function binaryResponse(int $status, string $body, string $contentType = 'application/octet-stream'): Response
    {
        return new Response(
            $status,
            ['Content-Type' => $contentType],
            $body,
        );
    }
}
