<?php

declare(strict_types=1);

namespace Renamed\Exceptions;

/**
 * Network error - connection failed.
 */
class NetworkException extends RenamedExceptionBase
{
    public function __construct(string $message = 'Network request failed')
    {
        parent::__construct($message, 'NETWORK_ERROR');
    }
}
