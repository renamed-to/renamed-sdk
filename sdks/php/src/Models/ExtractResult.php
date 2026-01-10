<?php

declare(strict_types=1);

namespace Renamed\Models;

/**
 * Result of extract operation.
 */
final class ExtractResult
{
    /**
     * @param array<string, mixed> $data Extracted data matching the schema.
     */
    public function __construct(
        /** Extracted data matching the schema. */
        public readonly array $data,
        /** Confidence score (0-1). */
        public readonly float $confidence,
    ) {}

    /**
     * Create instance from API response array.
     *
     * @param array<string, mixed> $data
     */
    public static function fromArray(array $data): self
    {
        return new self(
            data: $data['data'] ?? [],
            confidence: (float) ($data['confidence'] ?? 0.0),
        );
    }

    /**
     * Convert to array.
     *
     * @return array<string, mixed>
     */
    public function toArray(): array
    {
        return [
            'data' => $this->data,
            'confidence' => $this->confidence,
        ];
    }
}
