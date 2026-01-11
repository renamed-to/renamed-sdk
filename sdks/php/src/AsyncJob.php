<?php

declare(strict_types=1);

namespace Renamed;

use Psr\Log\LoggerInterface;
use Renamed\Exceptions\JobException;
use Renamed\Models\JobStatusResponse;
use Renamed\Models\PdfSplitResult;

/**
 * Async job handle for long-running operations like PDF split.
 */
final class AsyncJob
{
    private const DEFAULT_POLL_INTERVAL = 2.0;
    private const DEFAULT_MAX_ATTEMPTS = 150; // 5 minutes at 2s intervals
    private const LOG_PREFIX = '[Renamed]';

    private Client $client;
    private string $statusUrl;
    private float $pollInterval;
    private int $maxAttempts;
    private ?LoggerInterface $logger;

    public function __construct(
        Client $client,
        string $statusUrl,
        float $pollInterval = self::DEFAULT_POLL_INTERVAL,
        int $maxAttempts = self::DEFAULT_MAX_ATTEMPTS,
        ?LoggerInterface $logger = null,
    ) {
        $this->client = $client;
        $this->statusUrl = $statusUrl;
        $this->pollInterval = $pollInterval;
        $this->maxAttempts = $maxAttempts;
        $this->logger = $logger;
    }

    /**
     * Get current job status.
     *
     * @throws \Renamed\Exceptions\RenamedExceptionBase
     */
    public function status(): JobStatusResponse
    {
        $response = $this->client->request('GET', $this->statusUrl);
        return JobStatusResponse::fromArray($response);
    }

    /**
     * Wait for job completion, polling at regular intervals.
     *
     * @param callable(JobStatusResponse): void|null $onProgress Optional callback called with status updates
     * @return PdfSplitResult The completed job result
     * @throws JobException If the job fails or times out
     * @throws \Renamed\Exceptions\RenamedExceptionBase
     */
    public function wait(?callable $onProgress = null): PdfSplitResult
    {
        $attempts = 0;
        $lastLoggedStatus = '';

        while ($attempts < $this->maxAttempts) {
            $status = $this->status();

            // Log status changes to avoid spam
            $currentStatusKey = $status->status . ':' . ($status->progress ?? 0);
            if ($this->logger !== null && $currentStatusKey !== $lastLoggedStatus) {
                $this->logJobStatus($status);
                $lastLoggedStatus = $currentStatusKey;
            }

            if ($onProgress !== null) {
                $onProgress($status);
            }

            if ($status->isCompleted() && $status->result !== null) {
                return $status->result;
            }

            if ($status->isFailed()) {
                throw new JobException(
                    $status->error ?? 'Job failed',
                    $status->jobId
                );
            }

            $attempts++;
            usleep((int) ($this->pollInterval * 1_000_000));
        }

        throw new JobException('Job polling timeout exceeded');
    }

    /**
     * Get the status URL.
     */
    public function getStatusUrl(): string
    {
        return $this->statusUrl;
    }

    /**
     * Log job status update.
     */
    private function logJobStatus(JobStatusResponse $status): void
    {
        $jobId = $this->truncateJobId($status->jobId);
        $statusText = $status->status;

        $message = $status->progress !== null
            ? sprintf('%s Job %s: %s (%d%%)', self::LOG_PREFIX, $jobId, $statusText, $status->progress)
            : sprintf('%s Job %s: %s', self::LOG_PREFIX, $jobId, $statusText);

        $this->logger->debug($message);
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
}
