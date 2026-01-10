<?php

declare(strict_types=1);

namespace Renamed\Models;

/**
 * Team information.
 */
final class Team
{
    public function __construct(
        /** Team ID. */
        public readonly string $id,
        /** Team name. */
        public readonly string $name,
    ) {}

    /**
     * Create instance from API response array.
     *
     * @param array<string, mixed> $data
     */
    public static function fromArray(array $data): self
    {
        return new self(
            id: $data['id'],
            name: $data['name'],
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
            'id' => $this->id,
            'name' => $this->name,
        ];
    }
}
