<?php

declare(strict_types=1);

namespace Renamed\Models;

/**
 * Result of a rename operation.
 */
final class RenameResult
{
    public function __construct(
        /** Original filename that was uploaded. */
        public readonly string $originalFilename,
        /** AI-suggested new filename. */
        public readonly string $suggestedFilename,
        /** Suggested folder path for organization. */
        public readonly ?string $folderPath = null,
        /** Confidence score (0-1) of the suggestion. */
        public readonly ?float $confidence = null,
    ) {}

    /**
     * Create instance from API response array.
     *
     * @param array<string, mixed> $data
     */
    public static function fromArray(array $data): self
    {
        return new self(
            originalFilename: $data['originalFilename'],
            suggestedFilename: $data['suggestedFilename'],
            folderPath: $data['folderPath'] ?? null,
            confidence: isset($data['confidence']) ? (float) $data['confidence'] : null,
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
            'originalFilename' => $this->originalFilename,
            'suggestedFilename' => $this->suggestedFilename,
            'folderPath' => $this->folderPath,
            'confidence' => $this->confidence,
        ];
    }
}
