<?php

declare(strict_types=1);

namespace Renamed\Exceptions;

/**
 * Job error - async job failed.
 */
class JobException extends RenamedExceptionBase
{
    private ?string $jobId;

    public function __construct(string $message, ?string $jobId = null)
    {
        parent::__construct($message, 'JOB_ERROR');
        $this->jobId = $jobId;
    }

    public function getJobId(): ?string
    {
        return $this->jobId;
    }
}
