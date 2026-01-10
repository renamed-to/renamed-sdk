<?php

declare(strict_types=1);

namespace Renamed;

use GuzzleHttp\Client as GuzzleClient;
use GuzzleHttp\Exception\ConnectException;
use GuzzleHttp\Exception\RequestException;
use GuzzleHttp\Psr7\Utils;
use Psr\Http\Message\ResponseInterface;
use Renamed\Exceptions\AuthenticationException;
use Renamed\Exceptions\NetworkException;
use Renamed\Exceptions\RenamedExceptionBase;
use Renamed\Exceptions\TimeoutException;
use Renamed\Models\ExtractResult;
use Renamed\Models\RenameResult;
use Renamed\Models\User;

/**
 * renamed.to API client.
 *
 * @example
 * ```php
 * use Renamed\Client;
 *
 * $client = new Client('rt_your_api_key');
 * $result = $client->rename('/path/to/invoice.pdf');
 * echo $result->suggestedFilename; // "2025-01-15_AcmeCorp_INV-12345.pdf"
 * ```
 */
final class Client
{
    private const DEFAULT_BASE_URL = 'https://www.renamed.to/api/v1';
    private const DEFAULT_TIMEOUT = 30.0;
    private const DEFAULT_MAX_RETRIES = 2;

    private const MIME_TYPES = [
        'pdf' => 'application/pdf',
        'jpg' => 'image/jpeg',
        'jpeg' => 'image/jpeg',
        'png' => 'image/png',
        'tiff' => 'image/tiff',
        'tif' => 'image/tiff',
    ];

    private string $apiKey;
    private string $baseUrl;
    private float $timeout;
    private int $maxRetries;
    private GuzzleClient $httpClient;

    /**
     * Create a new renamed.to API client.
     *
     * @param string $apiKey API key for authentication (starts with rt_)
     * @param string $baseUrl Base URL for the API
     * @param float $timeout Request timeout in seconds
     * @param int $maxRetries Maximum number of retries for failed requests
     */
    public function __construct(
        string $apiKey,
        string $baseUrl = self::DEFAULT_BASE_URL,
        float $timeout = self::DEFAULT_TIMEOUT,
        int $maxRetries = self::DEFAULT_MAX_RETRIES,
    ) {
        if (empty($apiKey)) {
            throw new AuthenticationException('API key is required');
        }

        $this->apiKey = $apiKey;
        $this->baseUrl = rtrim($baseUrl, '/');
        $this->timeout = $timeout;
        $this->maxRetries = $maxRetries;

        $this->httpClient = new GuzzleClient([
            'timeout' => $timeout,
            'headers' => [
                'Authorization' => "Bearer {$apiKey}",
            ],
        ]);
    }

    /**
     * Get current user profile and credits.
     *
     * @return User User profile with credits balance
     * @throws RenamedExceptionBase
     *
     * @example
     * ```php
     * $user = $client->getUser();
     * echo "Credits remaining: {$user->credits}";
     * ```
     */
    public function getUser(): User
    {
        $response = $this->request('GET', '/user');
        return User::fromArray($response);
    }

    /**
     * Rename a file using AI.
     *
     * @param string|resource $file File path or resource to rename
     * @param array<string, mixed>|null $options Rename options (template: string)
     * @return RenameResult Result with suggested filename and folder path
     * @throws RenamedExceptionBase
     *
     * @example
     * ```php
     * $result = $client->rename('/path/to/invoice.pdf');
     * echo $result->suggestedFilename; // "2025-01-15_AcmeCorp_INV-12345.pdf"
     *
     * // With custom template
     * $result = $client->rename('/path/to/file.pdf', ['template' => '{date}_{vendor}']);
     * ```
     */
    public function rename(string|mixed $file, ?array $options = null): RenameResult
    {
        $additionalFields = [];
        if (isset($options['template'])) {
            $additionalFields['template'] = $options['template'];
        }

        $response = $this->uploadFile('/rename', $file, $additionalFields);
        return RenameResult::fromArray($response);
    }

    /**
     * Split a PDF into multiple documents.
     *
     * Returns an AsyncJob that can be polled for completion.
     *
     * @param string|resource $file PDF file path or resource to split
     * @param array<string, mixed>|null $options Split options (mode: 'auto'|'pages'|'blank', pagesPerSplit: int)
     * @return AsyncJob Async job that can be waited on for the result
     * @throws RenamedExceptionBase
     *
     * @example
     * ```php
     * $job = $client->pdfSplit('/path/to/multi-page.pdf', ['mode' => 'auto']);
     * $result = $job->wait(fn($s) => echo "Progress: {$s->progress}%\n");
     * foreach ($result->documents as $doc) {
     *     echo "{$doc->filename}: {$doc->downloadUrl}\n";
     * }
     * ```
     */
    public function pdfSplit(string|mixed $file, ?array $options = null): AsyncJob
    {
        $additionalFields = [];

        if (isset($options['mode'])) {
            $additionalFields['mode'] = $options['mode'];
        }
        if (isset($options['pagesPerSplit'])) {
            $additionalFields['pagesPerSplit'] = (string) $options['pagesPerSplit'];
        }

        $response = $this->uploadFile('/pdf-split', $file, $additionalFields);

        return new AsyncJob($this, $response['statusUrl']);
    }

    /**
     * Extract structured data from a document.
     *
     * @param string|resource $file Document file path or resource
     * @param array<string, mixed>|null $options Extract options (prompt: string, schema: array)
     * @return ExtractResult Result with extracted data
     * @throws RenamedExceptionBase
     *
     * @example
     * ```php
     * $result = $client->extract('/path/to/invoice.pdf', [
     *     'prompt' => 'Extract invoice details',
     * ]);
     * print_r($result->data);
     *
     * // With JSON schema
     * $result = $client->extract('/path/to/invoice.pdf', [
     *     'schema' => [
     *         'type' => 'object',
     *         'properties' => [
     *             'vendor' => ['type' => 'string'],
     *             'amount' => ['type' => 'number'],
     *         ],
     *     ],
     * ]);
     * ```
     */
    public function extract(string|mixed $file, ?array $options = null): ExtractResult
    {
        $additionalFields = [];

        if (isset($options['prompt'])) {
            $additionalFields['prompt'] = $options['prompt'];
        }
        if (isset($options['schema'])) {
            $additionalFields['schema'] = json_encode($options['schema']);
        }

        $response = $this->uploadFile('/extract', $file, $additionalFields);
        return ExtractResult::fromArray($response);
    }

    /**
     * Download a file from a URL (e.g., split document).
     *
     * @param string $url URL to download from
     * @return string File content as string
     * @throws RenamedExceptionBase
     *
     * @example
     * ```php
     * $result = $job->wait();
     * foreach ($result->documents as $doc) {
     *     $content = $client->downloadFile($doc->downloadUrl);
     *     file_put_contents($doc->filename, $content);
     * }
     * ```
     */
    public function downloadFile(string $url): string
    {
        try {
            $response = $this->httpClient->get($url);
            return (string) $response->getBody();
        } catch (ConnectException $e) {
            throw new NetworkException($e->getMessage());
        } catch (RequestException $e) {
            if ($e->hasResponse()) {
                $response = $e->getResponse();
                throw RenamedExceptionBase::fromHttpStatus(
                    $response->getStatusCode(),
                    $response->getReasonPhrase()
                );
            }
            throw new NetworkException($e->getMessage());
        }
    }

    /**
     * Make a request to the API.
     *
     * @internal
     * @param string $method HTTP method
     * @param string $path API path or full URL
     * @param array<string, mixed> $options Request options
     * @return array<string, mixed> Response data
     * @throws RenamedExceptionBase
     */
    public function request(string $method, string $path, array $options = []): array
    {
        $url = $this->buildUrl($path);
        $lastException = null;
        $attempts = 0;

        while ($attempts <= $this->maxRetries) {
            try {
                $response = $this->httpClient->request($method, $url, $options);
                return $this->handleResponse($response);
            } catch (ConnectException $e) {
                $lastException = new NetworkException($e->getMessage());
            } catch (RequestException $e) {
                if ($e->hasResponse()) {
                    $response = $e->getResponse();
                    $statusCode = $response->getStatusCode();

                    // Don't retry client errors (4xx)
                    if ($statusCode >= 400 && $statusCode < 500) {
                        $payload = $this->decodeResponse($response);
                        throw RenamedExceptionBase::fromHttpStatus(
                            $statusCode,
                            $response->getReasonPhrase(),
                            $payload
                        );
                    }

                    $lastException = RenamedExceptionBase::fromHttpStatus(
                        $statusCode,
                        $response->getReasonPhrase(),
                        $this->decodeResponse($response)
                    );
                } else {
                    $lastException = new NetworkException($e->getMessage());
                }
            } catch (\GuzzleHttp\Exception\TransferException $e) {
                if (str_contains($e->getMessage(), 'timeout')) {
                    $lastException = new TimeoutException($e->getMessage());
                } else {
                    $lastException = new NetworkException($e->getMessage());
                }
            }

            $attempts++;
            if ($attempts <= $this->maxRetries) {
                usleep((int) (pow(2, $attempts) * 100_000)); // Exponential backoff
            }
        }

        throw $lastException ?? new NetworkException();
    }

    /**
     * Upload a file to the API.
     *
     * @param string $path API path
     * @param string|resource $file File path or resource
     * @param array<string, string> $additionalFields Additional form fields
     * @return array<string, mixed> Response data
     * @throws RenamedExceptionBase
     */
    private function uploadFile(string $path, string|mixed $file, array $additionalFields = []): array
    {
        [$filename, $content, $mimeType] = $this->prepareFile($file);

        $multipart = [
            [
                'name' => 'file',
                'contents' => $content,
                'filename' => $filename,
                'headers' => ['Content-Type' => $mimeType],
            ],
        ];

        foreach ($additionalFields as $name => $value) {
            $multipart[] = [
                'name' => $name,
                'contents' => $value,
            ];
        }

        return $this->request('POST', $path, ['multipart' => $multipart]);
    }

    /**
     * Prepare file for upload.
     *
     * @param string|resource $file File path or resource
     * @return array{0: string, 1: string, 2: string} [filename, content, mimeType]
     */
    private function prepareFile(string|mixed $file): array
    {
        if (is_string($file)) {
            // File path
            if (!file_exists($file)) {
                throw new \InvalidArgumentException("File not found: {$file}");
            }
            $filename = basename($file);
            $content = file_get_contents($file);
            if ($content === false) {
                throw new \InvalidArgumentException("Failed to read file: {$file}");
            }
            return [$filename, $content, $this->getMimeType($filename)];
        }

        if (is_resource($file)) {
            // Resource (stream)
            $metadata = stream_get_meta_data($file);
            $filename = isset($metadata['uri']) ? basename($metadata['uri']) : 'file';
            $content = stream_get_contents($file);
            if ($content === false) {
                throw new \InvalidArgumentException('Failed to read stream');
            }
            return [$filename, $content, $this->getMimeType($filename)];
        }

        throw new \InvalidArgumentException('File must be a string path or resource');
    }

    /**
     * Get MIME type from filename.
     */
    private function getMimeType(string $filename): string
    {
        $extension = strtolower(pathinfo($filename, PATHINFO_EXTENSION));
        return self::MIME_TYPES[$extension] ?? 'application/octet-stream';
    }

    /**
     * Build full URL from path.
     */
    private function buildUrl(string $path): string
    {
        if (str_starts_with($path, 'http://') || str_starts_with($path, 'https://')) {
            return $path;
        }

        if (str_starts_with($path, '/')) {
            return "{$this->baseUrl}{$path}";
        }

        return "{$this->baseUrl}/{$path}";
    }

    /**
     * Handle response and return decoded data.
     *
     * @return array<string, mixed>
     */
    private function handleResponse(ResponseInterface $response): array
    {
        $statusCode = $response->getStatusCode();

        if ($statusCode >= 400) {
            throw RenamedExceptionBase::fromHttpStatus(
                $statusCode,
                $response->getReasonPhrase(),
                $this->decodeResponse($response)
            );
        }

        return $this->decodeResponse($response);
    }

    /**
     * Decode response body.
     *
     * @return array<string, mixed>
     */
    private function decodeResponse(ResponseInterface $response): array
    {
        $body = (string) $response->getBody();
        if (empty($body)) {
            return [];
        }

        $data = json_decode($body, true);
        return is_array($data) ? $data : [];
    }
}
