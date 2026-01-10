<?php

declare(strict_types=1);

namespace Renamed\Models;

/**
 * Response from job status endpoint.
 */
final class JobStatusResponse
{
    public function __construct(
        /** Unique job identifier. */
        public readonly string $jobId,
        /** Current job status: pending, processing, completed, or failed. */
        public readonly string $status,
        /** Progress percentage (0-100). */
        public readonly ?int $progress = null,
        /** Error message if job failed. */
        public readonly ?string $error = null,
        /** Result data when job is completed. */
        public readonly ?PdfSplitResult $result = null,
    ) {}

    /**
     * Create instance from API response array.
     *
     * @param array<string, mixed> $data
     */
    public static function fromArray(array $data): self
    {
        return new self(
            jobId: $data['jobId'],
            status: $data['status'],
            progress: isset($data['progress']) ? (int) $data['progress'] : null,
            error: $data['error'] ?? null,
            result: isset($data['result']) ? PdfSplitResult::fromArray($data['result']) : null,
        );
    }

    /**
     * Check if job is completed.
     */
    public function isCompleted(): bool
    {
        return $this->status === 'completed';
    }

    /**
     * Check if job has failed.
     */
    public function isFailed(): bool
    {
        return $this->status === 'failed';
    }

    /**
     * Check if job is still processing.
     */
    public function isProcessing(): bool
    {
        return in_array($this->status, ['pending', 'processing'], true);
    }

    /**
     * Convert to array.
     *
     * @return array<string, mixed>
     */
    public function toArray(): array
    {
        return [
            'jobId' => $this->jobId,
            'status' => $this->status,
            'progress' => $this->progress,
            'error' => $this->error,
            'result' => $this->result?->toArray(),
        ];
    }
}
