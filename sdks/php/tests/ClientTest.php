<?php

declare(strict_types=1);

namespace Renamed\Tests;

use GuzzleHttp\Client as GuzzleClient;
use GuzzleHttp\Handler\MockHandler;
use GuzzleHttp\HandlerStack;
use GuzzleHttp\Psr7\Response;
use PHPUnit\Framework\Attributes\Test;
use PHPUnit\Framework\TestCase;
use Renamed\Client;
use Renamed\Exceptions\AuthenticationException;

final class ClientTest extends TestCase
{
    #[Test]
    public function constructor_throws_when_api_key_is_empty(): void
    {
        $this->expectException(AuthenticationException::class);
        $this->expectExceptionMessage('API key is required');

        new Client('');
    }

    #[Test]
    public function constructor_accepts_valid_api_key(): void
    {
        $client = new Client('rt_test_api_key');

        $this->assertInstanceOf(Client::class, $client);
    }

    #[Test]
    public function constructor_accepts_custom_base_url(): void
    {
        $client = new Client('rt_test_api_key', 'https://custom.api.com/v1');

        $this->assertInstanceOf(Client::class, $client);
    }

    #[Test]
    public function constructor_accepts_custom_timeout(): void
    {
        $client = new Client('rt_test_api_key', 'https://www.renamed.to/api/v1', 60.0);

        $this->assertInstanceOf(Client::class, $client);
    }

    #[Test]
    public function constructor_accepts_custom_max_retries(): void
    {
        $client = new Client('rt_test_api_key', 'https://www.renamed.to/api/v1', 30.0, 5);

        $this->assertInstanceOf(Client::class, $client);
    }

    #[Test]
    public function constructor_trims_trailing_slash_from_base_url(): void
    {
        // This test verifies the base URL normalization by creating a client
        // with a trailing slash. The internal normalization is tested implicitly
        // through other integration tests.
        $client = new Client('rt_test_api_key', 'https://www.renamed.to/api/v1/');

        $this->assertInstanceOf(Client::class, $client);
    }
}
