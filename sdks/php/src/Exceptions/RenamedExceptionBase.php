<?php

declare(strict_types=1);

namespace Renamed\Exceptions;

use Exception;
use Throwable;

/**
 * Base exception for all renamed.to SDK errors.
 */
class RenamedExceptionBase extends Exception
{
    protected string $errorCode;
    protected ?int $statusCode;
    protected mixed $details;

    public function __construct(
        string $message = 'An error occurred',
        string $errorCode = 'UNKNOWN_ERROR',
        ?int $statusCode = null,
        mixed $details = null,
        ?Throwable $previous = null
    ) {
        parent::__construct($message, 0, $previous);
        $this->errorCode = $errorCode;
        $this->statusCode = $statusCode;
        $this->details = $details;
    }

    public function getErrorCode(): string
    {
        return $this->errorCode;
    }

    public function getStatusCode(): ?int
    {
        return $this->statusCode;
    }

    public function getDetails(): mixed
    {
        return $this->details;
    }

    /**
     * Create appropriate exception from HTTP status code.
     */
    public static function fromHttpStatus(int $status, string $statusText, mixed $payload = null): self
    {
        $message = $statusText;
        if (is_array($payload) && isset($payload['error'])) {
            $message = (string) $payload['error'];
        }

        return match ($status) {
            401 => new AuthenticationException($message),
            402 => new InsufficientCreditsException($message),
            400, 422 => new ValidationException($message, $payload),
            429 => new RateLimitException(
                $message,
                is_array($payload) ? ($payload['retryAfter'] ?? null) : null
            ),
            default => new self($message, 'API_ERROR', $status, $payload),
        };
    }
}
