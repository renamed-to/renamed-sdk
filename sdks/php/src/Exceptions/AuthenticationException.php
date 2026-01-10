<?php

declare(strict_types=1);

namespace Renamed\Exceptions;

/**
 * Authentication error - invalid or missing API key.
 */
class AuthenticationException extends RenamedExceptionBase
{
    public function __construct(string $message = 'Invalid or missing API key')
    {
        parent::__construct($message, 'AUTHENTICATION_ERROR', 401);
    }
}
