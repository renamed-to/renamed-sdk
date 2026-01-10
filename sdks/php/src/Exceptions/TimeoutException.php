<?php

declare(strict_types=1);

namespace Renamed\Exceptions;

/**
 * Timeout error - request took too long.
 */
class TimeoutException extends RenamedExceptionBase
{
    public function __construct(string $message = 'Request timed out')
    {
        parent::__construct($message, 'TIMEOUT_ERROR');
    }
}
