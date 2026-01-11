<?php

declare(strict_types=1);

namespace Renamed;

use GuzzleHttp\Client as GuzzleClient;
use GuzzleHttp\Exception\ConnectException;
use GuzzleHttp\Exception\RequestException;
use GuzzleHttp\Psr7\Utils;
use Psr\Http\Message\ResponseInterface;
use Psr\Log\LoggerInterface;
use Psr\Log\NullLogger;
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
 *
 * // With debug logging
 * $client = new Client('rt_your_api_key', debug: true);
 * // Logs to stderr: [Renamed] POST /rename -> 200 (234ms)
 *
 * // With custom PSR-3 logger
 * $client = new Client('rt_your_api_key', debug: true, logger: $monolog);
 * ```
 */
final class Client
{
    private const DEFAULT_BASE_URL = 'https://www.renamed.to/api/v1';
    private const DEFAULT_TIMEOUT = 30.0;
    private const DEFAULT_MAX_RETRIES = 2;
    private const LOG_PREFIX = '[Renamed]';

    private const MIME_TYPES = [
        'pdf' => 'application/pdf',
        'jpg' => 'image/jpeg',
        'jpeg' => 'image/jpeg',
        'png' => 'image/png',
        'tiff' => 'image/tiff',
        'tif' => 'image/tiff',
    ];

    private string $baseUrl;
    private int $maxRetries;
    private bool $debug;
    private LoggerInterface $logger;
    private GuzzleClient $httpClient;

    /**
     * Create a new renamed.to API client.
     *
     * @param string $apiKey API key for authentication (starts with rt_)
     * @param string $baseUrl Base URL for the API
     * @param float $timeout Request timeout in seconds
     * @param int $maxRetries Maximum number of retries for failed requests
     * @param bool $debug Enable debug logging
     * @param LoggerInterface|null $logger PSR-3 logger instance (uses stderr when debug=true and no logger provided)
     */
    public function __construct(
        string $apiKey,
        string $baseUrl = self::DEFAULT_BASE_URL,
        float $timeout = self::DEFAULT_TIMEOUT,
        int $maxRetries = self::DEFAULT_MAX_RETRIES,
        bool $debug = false,
        ?LoggerInterface $logger = null,
    ) {
        if (empty($apiKey)) {
            throw new AuthenticationException('API key is required');
        }

        $this->baseUrl = rtrim($baseUrl, '/');
        $this->maxRetries = $maxRetries;
        $this->debug = $debug;
        $this->logger = $logger ?? ($debug ? new StderrLogger() : new NullLogger());

        $this->httpClient = new GuzzleClient([
            'timeout' => $timeout,
            'headers' => [
                'Authorization' => "Bearer {$apiKey}",
            ],
        ]);

        if ($this->debug) {
            $this->log('debug', 'Client initialized', [
                'api_key' => $this->maskApiKey($apiKey),
                'base_url' => $this->baseUrl,
            ]);
        }
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
    public function rename(mixed $file, ?array $options = null): RenameResult
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
    public function pdfSplit(mixed $file, ?array $options = null): AsyncJob
    {
        $additionalFields = [];

        if (isset($options['mode'])) {
            $additionalFields['mode'] = $options['mode'];
        }
        if (isset($options['pagesPerSplit'])) {
            $additionalFields['pagesPerSplit'] = (string) $options['pagesPerSplit'];
        }

        $response = $this->uploadFile('/pdf-split', $file, $additionalFields);

        return new AsyncJob($this, $response['statusUrl'], logger: $this->debug ? $this->logger : null);
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
    public function extract(mixed $file, ?array $options = null): ExtractResult
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
        $startTime = microtime(true);

        try {
            $response = $this->httpClient->get($url);
            $content = (string) $response->getBody();

            if ($this->debug) {
                $durationMs = $this->calculateDurationMs($startTime);
                $path = parse_url($url, PHP_URL_PATH) ?? $url;
                $this->log('debug', sprintf(
                    'GET %s -> %d (%dms)',
                    $path,
                    $response->getStatusCode(),
                    $durationMs
                ));
            }

            return $content;
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
        $logPath = $this->extractPathForLogging($path);
        $lastException = null;
        $attempts = 0;

        while ($attempts <= $this->maxRetries) {
            $startTime = microtime(true);

            try {
                $response = $this->httpClient->request($method, $url, $options);
                $result = $this->handleResponse($response);

                if ($this->debug) {
                    $durationMs = $this->calculateDurationMs($startTime);
                    $this->log('debug', sprintf(
                        '%s %s -> %d (%dms)',
                        $method,
                        $logPath,
                        $response->getStatusCode(),
                        $durationMs
                    ));
                }

                return $result;
            } catch (ConnectException $e) {
                $lastException = new NetworkException($e->getMessage());
            } catch (RequestException $e) {
                if ($e->hasResponse()) {
                    $response = $e->getResponse();
                    $statusCode = $response->getStatusCode();

                    // Don't retry client errors (4xx)
                    if ($statusCode >= 400 && $statusCode < 500) {
                        if ($this->debug) {
                            $durationMs = $this->calculateDurationMs($startTime);
                            $this->log('debug', sprintf(
                                '%s %s -> %d (%dms)',
                                $method,
                                $logPath,
                                $statusCode,
                                $durationMs
                            ));
                        }

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
                $backoffMs = (int) (pow(2, $attempts) * 100);

                if ($this->debug) {
                    $this->log('debug', sprintf(
                        'Retry attempt %d/%d, waiting %dms',
                        $attempts,
                        $this->maxRetries,
                        $backoffMs
                    ));
                }

                usleep($backoffMs * 1000);
            }
        }

        throw $lastException ?? new NetworkException();
    }

    /**
     * Log a job polling status update.
     *
     * @internal Called by AsyncJob for consistent logging.
     */
    public function logJobStatus(string $jobId, string $status, ?int $progress = null): void
    {
        if (!$this->debug) {
            return;
        }

        $message = $progress !== null
            ? sprintf('Job %s: %s (%d%%)', $this->truncateJobId($jobId), $status, $progress)
            : sprintf('Job %s: %s', $this->truncateJobId($jobId), $status);

        $this->log('debug', $message);
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
    private function uploadFile(string $path, mixed $file, array $additionalFields = []): array
    {
        [$filename, $content, $mimeType] = $this->prepareFile($file);

        if ($this->debug) {
            $this->logUpload($filename, strlen($content));
        }

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
    private function prepareFile(mixed $file): array
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
     * Extract path for logging (strips base URL for cleaner logs).
     */
    private function extractPathForLogging(string $path): string
    {
        if (str_starts_with($path, 'http://') || str_starts_with($path, 'https://')) {
            return parse_url($path, PHP_URL_PATH) ?? $path;
        }

        return str_starts_with($path, '/') ? $path : "/{$path}";
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

    /**
     * Log a message with the standard prefix.
     */
    private function log(string $level, string $message, array $context = []): void
    {
        $this->logger->log($level, self::LOG_PREFIX . ' ' . $message, $context);
    }

    /**
     * Log file upload details.
     */
    private function logUpload(string $filename, int $sizeBytes): void
    {
        $sizeFormatted = $this->formatFileSize($sizeBytes);
        $this->log('debug', sprintf('Upload: %s (%s)', $filename, $sizeFormatted));
    }

    /**
     * Mask API key for safe logging: rt_...xxxx (first 3 chars + last 4).
     */
    private function maskApiKey(string $apiKey): string
    {
        if (strlen($apiKey) <= 7) {
            return '***';
        }

        $prefix = substr($apiKey, 0, 3);
        $suffix = substr($apiKey, -4);

        return "{$prefix}...{$suffix}";
    }

    /**
     * Truncate job ID for logging (first 8 characters).
     */
    private function truncateJobId(string $jobId): string
    {
        if (strlen($jobId) <= 8) {
            return $jobId;
        }

        return substr($jobId, 0, 8);
    }

    /**
     * Format file size for human-readable output.
     */
    private function formatFileSize(int $bytes): string
    {
        if ($bytes < 1024) {
            return "{$bytes} B";
        }

        if ($bytes < 1024 * 1024) {
            return sprintf('%.1f KB', $bytes / 1024);
        }

        return sprintf('%.1f MB', $bytes / (1024 * 1024));
    }

    /**
     * Calculate duration in milliseconds from start time.
     */
    private function calculateDurationMs(float $startTime): int
    {
        return (int) ((microtime(true) - $startTime) * 1000);
    }
}
