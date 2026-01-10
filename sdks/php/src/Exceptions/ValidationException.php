<?php

declare(strict_types=1);

namespace Renamed\Exceptions;

/**
 * Validation error - invalid request parameters.
 */
class ValidationException extends RenamedExceptionBase
{
    public function __construct(string $message, mixed $details = null)
    {
        parent::__construct($message, 'VALIDATION_ERROR', 400, $details);
    }
}
