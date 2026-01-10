<?php

declare(strict_types=1);

namespace Renamed\Models;

/**
 * Result of PDF split operation.
 */
final class PdfSplitResult
{
    /**
     * @param SplitDocument[] $documents Split documents.
     */
    public function __construct(
        /** Original filename. */
        public readonly string $originalFilename,
        /** Split documents. */
        public readonly array $documents,
        /** Total number of pages in original document. */
        public readonly int $totalPages,
    ) {}

    /**
     * Create instance from API response array.
     *
     * @param array<string, mixed> $data
     */
    public static function fromArray(array $data): self
    {
        $documents = array_map(
            fn(array $doc) => SplitDocument::fromArray($doc),
            $data['documents'] ?? []
        );

        return new self(
            originalFilename: $data['originalFilename'],
            documents: $documents,
            totalPages: (int) $data['totalPages'],
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
            'documents' => array_map(fn(SplitDocument $doc) => $doc->toArray(), $this->documents),
            'totalPages' => $this->totalPages,
        ];
    }
}
