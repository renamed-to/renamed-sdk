<?php

declare(strict_types=1);

namespace Renamed;

use Psr\Log\AbstractLogger;
use Stringable;

/**
 * Simple PSR-3 logger that writes to stderr.
 *
 * Used as the default logger when debug=true and no custom logger is provided.
 * Output format: [Renamed] message {context}
 *
 * @internal
 */
final class StderrLogger extends AbstractLogger
{
    /**
     * Log a message to stderr.
     *
     * @param mixed $level Log level
     * @param string|Stringable $message Log message
     * @param array<string, mixed> $context Additional context
     */
    public function log(mixed $level, string|Stringable $message, array $context = []): void
    {
        $output = (string) $message;

        // Append context if non-empty
        if (!empty($context)) {
            $output .= ' ' . json_encode($context, JSON_UNESCAPED_SLASHES);
        }

        fwrite(STDERR, $output . PHP_EOL);
    }
}
