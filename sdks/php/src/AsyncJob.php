<?php

declare(strict_types=1);

namespace Renamed;

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

    private Client $client;
    private string $statusUrl;
    private float $pollInterval;
    private int $maxAttempts;

    public function __construct(
        Client $client,
        string $statusUrl,
        float $pollInterval = self::DEFAULT_POLL_INTERVAL,
        int $maxAttempts = self::DEFAULT_MAX_ATTEMPTS,
    ) {
        $this->client = $client;
        $this->statusUrl = $statusUrl;
        $this->pollInterval = $pollInterval;
        $this->maxAttempts = $maxAttempts;
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

        while ($attempts < $this->maxAttempts) {
            $status = $this->status();

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
}
