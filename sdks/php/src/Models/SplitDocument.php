<?php

declare(strict_types=1);

namespace Renamed\Models;

/**
 * A single document from PDF split.
 */
final class SplitDocument
{
    public function __construct(
        /** Document index (0-based). */
        public readonly int $index,
        /** Suggested filename for this document. */
        public readonly string $filename,
        /** Page range included in this document. */
        public readonly string $pages,
        /** URL to download this document. */
        public readonly string $downloadUrl,
        /** Size in bytes. */
        public readonly int $size,
    ) {}

    /**
     * Create instance from API response array.
     *
     * @param array<string, mixed> $data
     */
    public static function fromArray(array $data): self
    {
        return new self(
            index: (int) $data['index'],
            filename: $data['filename'],
            pages: $data['pages'],
            downloadUrl: $data['downloadUrl'],
            size: (int) $data['size'],
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
            'index' => $this->index,
            'filename' => $this->filename,
            'pages' => $this->pages,
            'downloadUrl' => $this->downloadUrl,
            'size' => $this->size,
        ];
    }
}
