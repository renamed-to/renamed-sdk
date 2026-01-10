<?php

declare(strict_types=1);

namespace Renamed\Models;

/**
 * User profile information.
 */
final class User
{
    public function __construct(
        /** User ID. */
        public readonly string $id,
        /** Email address. */
        public readonly string $email,
        /** Display name. */
        public readonly ?string $name = null,
        /** Available credits. */
        public readonly ?int $credits = null,
        /** Team information (if applicable). */
        public readonly ?Team $team = null,
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
            email: $data['email'],
            name: $data['name'] ?? null,
            credits: isset($data['credits']) ? (int) $data['credits'] : null,
            team: isset($data['team']) ? Team::fromArray($data['team']) : null,
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
            'email' => $this->email,
            'name' => $this->name,
            'credits' => $this->credits,
            'team' => $this->team?->toArray(),
        ];
    }
}
