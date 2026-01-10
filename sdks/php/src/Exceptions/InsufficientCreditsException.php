<?php

declare(strict_types=1);

namespace Renamed\Exceptions;

/**
 * Insufficient credits error.
 */
class InsufficientCreditsException extends RenamedExceptionBase
{
    public function __construct(string $message = 'Insufficient credits')
    {
        parent::__construct($message, 'INSUFFICIENT_CREDITS', 402);
    }
}
