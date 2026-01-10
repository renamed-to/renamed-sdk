<?php

declare(strict_types=1);

namespace Renamed\Exceptions;

/**
 * Rate limit exceeded.
 */
class RateLimitException extends RenamedExceptionBase
{
    private ?int $retryAfter;

    public function __construct(string $message = 'Rate limit exceeded', ?int $retryAfter = null)
    {
        parent::__construct($message, 'RATE_LIMIT_ERROR', 429);
        $this->retryAfter = $retryAfter;
    }

    public function getRetryAfter(): ?int
    {
        return $this->retryAfter;
    }
}
