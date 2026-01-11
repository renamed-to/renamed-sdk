<?php

declare(strict_types=1);

namespace Renamed\Tests;

use GuzzleHttp\Handler\MockHandler;
use GuzzleHttp\Psr7\Response;
use PHPUnit\Framework\Attributes\Test;
use Renamed\Client;
use Renamed\Exceptions\AuthenticationException;
use Renamed\Exceptions\InsufficientCreditsException;
use Renamed\Exceptions\RateLimitException;
use Renamed\Exceptions\ValidationException;
use Renamed\Exceptions\RenamedExceptionBase;

final class ClientTest extends TestCase
{
    // ==================== Constructor Tests ====================

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
        $client = new Client('rt_test_api_key', 'https://www.renamed.to/api/v1/');

        $this->assertInstanceOf(Client::class, $client);
    }

    // ==================== Error Handling Tests ====================

    #[Test]
    public function request_throws_authentication_exception_on_401(): void
    {
        $client = $this->createMockClient([
            $this->jsonResponse(401, ['error' => 'Invalid API key']),
        ]);

        $this->expectException(AuthenticationException::class);

        $client->getUser();
    }

    #[Test]
    public function request_throws_insufficient_credits_exception_on_402(): void
    {
        $client = $this->createMockClient([
            $this->jsonResponse(402, ['error' => 'Insufficient credits']),
        ]);

        $this->expectException(InsufficientCreditsException::class);

        $client->getUser();
    }

    #[Test]
    public function request_throws_rate_limit_exception_on_429(): void
    {
        $mockHandler = null;
        $client = $this->createMockClient([
            $this->jsonResponse(429, ['error' => 'Rate limit exceeded', 'retryAfter' => 60]),
        ], $mockHandler);

        try {
            $client->getUser();
            $this->fail('Expected RateLimitException');
        } catch (RateLimitException $e) {
            $this->assertSame(60, $e->getRetryAfter());
        }
    }

    #[Test]
    public function request_throws_validation_exception_on_400(): void
    {
        $client = $this->createMockClient([
            $this->jsonResponse(400, ['error' => 'Bad request']),
        ]);

        $this->expectException(ValidationException::class);

        $client->getUser();
    }

    #[Test]
    public function request_throws_validation_exception_on_422(): void
    {
        $client = $this->createMockClient([
            $this->jsonResponse(422, ['error' => 'Unprocessable entity']),
        ]);

        $this->expectException(ValidationException::class);

        $client->getUser();
    }

    #[Test]
    public function request_throws_renamed_exception_on_500(): void
    {
        $client = $this->createMockClient([
            $this->jsonResponse(500, ['error' => 'Internal server error']),
        ]);

        $this->expectException(RenamedExceptionBase::class);

        $client->getUser();
    }

    // ==================== getUser Tests ====================

    #[Test]
    public function get_user_returns_user_model(): void
    {
        $mockData = [
            'id' => 'user_123',
            'email' => 'test@example.com',
            'name' => 'Test User',
            'credits' => 100,
        ];

        $client = $this->createMockClient([
            $this->jsonResponse(200, $mockData),
        ]);

        $user = $client->getUser();

        $this->assertSame('user_123', $user->id);
        $this->assertSame('test@example.com', $user->email);
        $this->assertSame('Test User', $user->name);
        $this->assertSame(100, $user->credits);
    }

    #[Test]
    public function get_user_handles_optional_fields(): void
    {
        $mockData = [
            'id' => 'user_456',
            'email' => 'user@test.com',
        ];

        $client = $this->createMockClient([
            $this->jsonResponse(200, $mockData),
        ]);

        $user = $client->getUser();

        $this->assertSame('user_456', $user->id);
        $this->assertSame('user@test.com', $user->email);
        $this->assertNull($user->name);
        $this->assertNull($user->credits);
    }

    #[Test]
    public function get_user_handles_team_data(): void
    {
        $mockData = [
            'id' => 'user_789',
            'email' => 'team@example.com',
            'team' => [
                'id' => 'team_123',
                'name' => 'Acme Corp',
            ],
        ];

        $client = $this->createMockClient([
            $this->jsonResponse(200, $mockData),
        ]);

        $user = $client->getUser();

        $this->assertSame('user_789', $user->id);
        $this->assertNotNull($user->team);
        $this->assertSame('team_123', $user->team->id);
        $this->assertSame('Acme Corp', $user->team->name);
    }

    // ==================== rename Tests ====================

    #[Test]
    public function rename_returns_rename_result(): void
    {
        $mockData = [
            'originalFilename' => 'doc.pdf',
            'suggestedFilename' => 'Invoice_2025.pdf',
            'confidence' => 0.95,
        ];

        $client = $this->createMockClient([
            $this->jsonResponse(200, $mockData),
        ]);

        // Create a temporary file for testing
        $tempFile = tempnam(sys_get_temp_dir(), 'test_');
        file_put_contents($tempFile, 'fake pdf content');

        try {
            $result = $client->rename($tempFile);

            $this->assertSame('doc.pdf', $result->originalFilename);
            $this->assertSame('Invoice_2025.pdf', $result->suggestedFilename);
            $this->assertSame(0.95, $result->confidence);
        } finally {
            unlink($tempFile);
        }
    }

    #[Test]
    public function rename_returns_result_with_folder_path(): void
    {
        $mockData = [
            'originalFilename' => 'invoice.pdf',
            'suggestedFilename' => '2025-01-15_Invoice.pdf',
            'folderPath' => 'Finances/Invoices/2025',
        ];

        $client = $this->createMockClient([
            $this->jsonResponse(200, $mockData),
        ]);

        $tempFile = tempnam(sys_get_temp_dir(), 'test_');
        file_put_contents($tempFile, 'content');

        try {
            $result = $client->rename($tempFile);

            $this->assertSame('Finances/Invoices/2025', $result->folderPath);
        } finally {
            unlink($tempFile);
        }
    }

    #[Test]
    public function rename_throws_for_nonexistent_file(): void
    {
        $client = $this->createMockClient([]);

        $this->expectException(\InvalidArgumentException::class);
        $this->expectExceptionMessage('File not found');

        $client->rename('/nonexistent/file.pdf');
    }

    #[Test]
    public function rename_accepts_resource(): void
    {
        $mockData = [
            'originalFilename' => 'stream.pdf',
            'suggestedFilename' => 'Renamed.pdf',
        ];

        $client = $this->createMockClient([
            $this->jsonResponse(200, $mockData),
        ]);

        $tempFile = tempnam(sys_get_temp_dir(), 'test_');
        file_put_contents($tempFile, 'stream content');
        $resource = fopen($tempFile, 'r');

        try {
            $result = $client->rename($resource);

            $this->assertSame('Renamed.pdf', $result->suggestedFilename);
        } finally {
            if (is_resource($resource)) {
                fclose($resource);
            }
            unlink($tempFile);
        }
    }

    // ==================== extract Tests ====================

    #[Test]
    public function extract_returns_extract_result(): void
    {
        $mockData = [
            'data' => [
                'invoiceNumber' => 'INV-001',
                'total' => 100.50,
                'vendor' => 'Acme Corp',
            ],
        ];

        $client = $this->createMockClient([
            $this->jsonResponse(200, $mockData),
        ]);

        $tempFile = tempnam(sys_get_temp_dir(), 'test_');
        file_put_contents($tempFile, 'invoice content');

        try {
            $result = $client->extract($tempFile);

            $this->assertIsArray($result->data);
            $this->assertSame('INV-001', $result->data['invoiceNumber']);
            $this->assertSame(100.50, $result->data['total']);
            $this->assertSame('Acme Corp', $result->data['vendor']);
        } finally {
            unlink($tempFile);
        }
    }

    #[Test]
    public function extract_with_prompt_option(): void
    {
        $mockData = [
            'data' => ['key' => 'value'],
        ];

        $client = $this->createMockClient([
            $this->jsonResponse(200, $mockData),
        ]);

        $tempFile = tempnam(sys_get_temp_dir(), 'test_');
        file_put_contents($tempFile, 'content');

        try {
            $result = $client->extract($tempFile, ['prompt' => 'Extract invoice details']);

            $this->assertIsArray($result->data);
        } finally {
            unlink($tempFile);
        }
    }

    #[Test]
    public function extract_with_schema_option(): void
    {
        $mockData = [
            'data' => ['field' => 'value'],
        ];

        $client = $this->createMockClient([
            $this->jsonResponse(200, $mockData),
        ]);

        $tempFile = tempnam(sys_get_temp_dir(), 'test_');
        file_put_contents($tempFile, 'content');

        try {
            $result = $client->extract($tempFile, [
                'schema' => [
                    'type' => 'object',
                    'properties' => [
                        'field' => ['type' => 'string'],
                    ],
                ],
            ]);

            $this->assertIsArray($result->data);
        } finally {
            unlink($tempFile);
        }
    }

    // ==================== pdfSplit Tests ====================

    #[Test]
    public function pdf_split_returns_async_job(): void
    {
        $mockData = [
            'jobId' => 'job_123',
            'statusUrl' => 'https://api.renamed.to/status/job_123',
        ];

        $client = $this->createMockClient([
            $this->jsonResponse(200, $mockData),
        ]);

        $tempFile = tempnam(sys_get_temp_dir(), 'test_') . '.pdf';
        file_put_contents($tempFile, 'pdf content');

        try {
            $job = $client->pdfSplit($tempFile);

            $this->assertSame('https://api.renamed.to/status/job_123', $job->getStatusUrl());
        } finally {
            unlink($tempFile);
        }
    }

    #[Test]
    public function pdf_split_with_mode_option(): void
    {
        $mockData = [
            'jobId' => 'job_456',
            'statusUrl' => 'https://api.renamed.to/status/job_456',
        ];

        $client = $this->createMockClient([
            $this->jsonResponse(200, $mockData),
        ]);

        $tempFile = tempnam(sys_get_temp_dir(), 'test_') . '.pdf';
        file_put_contents($tempFile, 'pdf content');

        try {
            $job = $client->pdfSplit($tempFile, ['mode' => 'auto']);

            $this->assertNotNull($job->getStatusUrl());
        } finally {
            unlink($tempFile);
        }
    }

    #[Test]
    public function pdf_split_with_pages_per_split_option(): void
    {
        $mockData = [
            'jobId' => 'job_789',
            'statusUrl' => 'https://api.renamed.to/status/job_789',
        ];

        $client = $this->createMockClient([
            $this->jsonResponse(200, $mockData),
        ]);

        $tempFile = tempnam(sys_get_temp_dir(), 'test_') . '.pdf';
        file_put_contents($tempFile, 'pdf content');

        try {
            $job = $client->pdfSplit($tempFile, ['mode' => 'pages', 'pagesPerSplit' => 5]);

            $this->assertNotNull($job->getStatusUrl());
        } finally {
            unlink($tempFile);
        }
    }

    // ==================== downloadFile Tests ====================

    #[Test]
    public function download_file_returns_content(): void
    {
        $expectedContent = 'downloaded file content';

        $client = $this->createMockClient([
            $this->binaryResponse(200, $expectedContent, 'application/pdf'),
        ]);

        $result = $client->downloadFile('https://api.renamed.to/download/file.pdf');

        $this->assertSame($expectedContent, $result);
    }

    #[Test]
    public function download_file_throws_on_404(): void
    {
        $client = $this->createMockClient([
            new Response(404, [], 'Not found'),
        ]);

        $this->expectException(RenamedExceptionBase::class);

        $client->downloadFile('https://api.renamed.to/download/missing.pdf');
    }
}
