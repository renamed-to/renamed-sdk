<?php

declare(strict_types=1);

namespace Renamed\Tests;

use PHPUnit\Framework\Attributes\Test;
use PHPUnit\Framework\TestCase;
use Renamed\Exceptions\AuthenticationException;
use Renamed\Exceptions\InsufficientCreditsException;
use Renamed\Exceptions\JobException;
use Renamed\Exceptions\NetworkException;
use Renamed\Exceptions\RateLimitException;
use Renamed\Exceptions\RenamedExceptionBase;
use Renamed\Exceptions\TimeoutException;
use Renamed\Exceptions\ValidationException;

final class ExceptionsTest extends TestCase
{
    // RenamedExceptionBase Tests

    #[Test]
    public function base_exception_has_default_values(): void
    {
        $exception = new RenamedExceptionBase();

        $this->assertSame('An error occurred', $exception->getMessage());
        $this->assertSame('UNKNOWN_ERROR', $exception->getErrorCode());
        $this->assertNull($exception->getStatusCode());
        $this->assertNull($exception->getDetails());
    }

    #[Test]
    public function base_exception_accepts_custom_values(): void
    {
        $details = ['field' => 'email', 'reason' => 'invalid'];
        $exception = new RenamedExceptionBase(
            'Custom message',
            'CUSTOM_ERROR',
            500,
            $details
        );

        $this->assertSame('Custom message', $exception->getMessage());
        $this->assertSame('CUSTOM_ERROR', $exception->getErrorCode());
        $this->assertSame(500, $exception->getStatusCode());
        $this->assertSame($details, $exception->getDetails());
    }

    #[Test]
    public function from_http_status_returns_authentication_exception_for_401(): void
    {
        $exception = RenamedExceptionBase::fromHttpStatus(401, 'Unauthorized');

        $this->assertInstanceOf(AuthenticationException::class, $exception);
        $this->assertSame('Unauthorized', $exception->getMessage());
    }

    #[Test]
    public function from_http_status_returns_insufficient_credits_for_402(): void
    {
        $exception = RenamedExceptionBase::fromHttpStatus(402, 'Payment Required');

        $this->assertInstanceOf(InsufficientCreditsException::class, $exception);
    }

    #[Test]
    public function from_http_status_returns_validation_exception_for_400(): void
    {
        $payload = ['errors' => ['file' => 'File is required']];
        $exception = RenamedExceptionBase::fromHttpStatus(400, 'Bad Request', $payload);

        $this->assertInstanceOf(ValidationException::class, $exception);
        $this->assertSame($payload, $exception->getDetails());
    }

    #[Test]
    public function from_http_status_returns_validation_exception_for_422(): void
    {
        $exception = RenamedExceptionBase::fromHttpStatus(422, 'Unprocessable Entity');

        $this->assertInstanceOf(ValidationException::class, $exception);
    }

    #[Test]
    public function from_http_status_returns_rate_limit_exception_for_429(): void
    {
        $payload = ['retryAfter' => 60];
        $exception = RenamedExceptionBase::fromHttpStatus(429, 'Too Many Requests', $payload);

        $this->assertInstanceOf(RateLimitException::class, $exception);
        $this->assertSame(60, $exception->getRetryAfter());
    }

    #[Test]
    public function from_http_status_returns_base_exception_for_unknown_status(): void
    {
        $exception = RenamedExceptionBase::fromHttpStatus(503, 'Service Unavailable');

        $this->assertInstanceOf(RenamedExceptionBase::class, $exception);
        $this->assertNotInstanceOf(AuthenticationException::class, $exception);
        $this->assertSame('API_ERROR', $exception->getErrorCode());
        $this->assertSame(503, $exception->getStatusCode());
    }

    #[Test]
    public function from_http_status_uses_error_message_from_payload(): void
    {
        $payload = ['error' => 'Custom error from API'];
        $exception = RenamedExceptionBase::fromHttpStatus(500, 'Internal Server Error', $payload);

        $this->assertSame('Custom error from API', $exception->getMessage());
    }

    // AuthenticationException Tests

    #[Test]
    public function authentication_exception_has_correct_defaults(): void
    {
        $exception = new AuthenticationException();

        $this->assertSame('Invalid or missing API key', $exception->getMessage());
        $this->assertSame('AUTHENTICATION_ERROR', $exception->getErrorCode());
        $this->assertSame(401, $exception->getStatusCode());
    }

    #[Test]
    public function authentication_exception_accepts_custom_message(): void
    {
        $exception = new AuthenticationException('API key expired');

        $this->assertSame('API key expired', $exception->getMessage());
    }

    // InsufficientCreditsException Tests

    #[Test]
    public function insufficient_credits_exception_has_correct_defaults(): void
    {
        $exception = new InsufficientCreditsException();

        $this->assertSame('Insufficient credits', $exception->getMessage());
        $this->assertSame('INSUFFICIENT_CREDITS', $exception->getErrorCode());
        $this->assertSame(402, $exception->getStatusCode());
    }

    #[Test]
    public function insufficient_credits_exception_accepts_custom_message(): void
    {
        $exception = new InsufficientCreditsException('You need 10 more credits');

        $this->assertSame('You need 10 more credits', $exception->getMessage());
    }

    // RateLimitException Tests

    #[Test]
    public function rate_limit_exception_has_correct_defaults(): void
    {
        $exception = new RateLimitException();

        $this->assertSame('Rate limit exceeded', $exception->getMessage());
        $this->assertSame('RATE_LIMIT_ERROR', $exception->getErrorCode());
        $this->assertSame(429, $exception->getStatusCode());
        $this->assertNull($exception->getRetryAfter());
    }

    #[Test]
    public function rate_limit_exception_stores_retry_after(): void
    {
        $exception = new RateLimitException('Too many requests', 120);

        $this->assertSame(120, $exception->getRetryAfter());
    }

    // ValidationException Tests

    #[Test]
    public function validation_exception_has_correct_defaults(): void
    {
        $exception = new ValidationException('Invalid input');

        $this->assertSame('Invalid input', $exception->getMessage());
        $this->assertSame('VALIDATION_ERROR', $exception->getErrorCode());
        $this->assertSame(400, $exception->getStatusCode());
        $this->assertNull($exception->getDetails());
    }

    #[Test]
    public function validation_exception_stores_details(): void
    {
        $details = [
            'errors' => [
                'file' => 'File must be a PDF',
                'template' => 'Invalid template syntax',
            ],
        ];
        $exception = new ValidationException('Validation failed', $details);

        $this->assertSame($details, $exception->getDetails());
    }

    // NetworkException Tests

    #[Test]
    public function network_exception_has_correct_defaults(): void
    {
        $exception = new NetworkException();

        $this->assertSame('Network request failed', $exception->getMessage());
        $this->assertSame('NETWORK_ERROR', $exception->getErrorCode());
        $this->assertNull($exception->getStatusCode());
    }

    #[Test]
    public function network_exception_accepts_custom_message(): void
    {
        $exception = new NetworkException('Connection refused');

        $this->assertSame('Connection refused', $exception->getMessage());
    }

    // TimeoutException Tests

    #[Test]
    public function timeout_exception_has_correct_defaults(): void
    {
        $exception = new TimeoutException();

        $this->assertSame('Request timed out', $exception->getMessage());
        $this->assertSame('TIMEOUT_ERROR', $exception->getErrorCode());
        $this->assertNull($exception->getStatusCode());
    }

    #[Test]
    public function timeout_exception_accepts_custom_message(): void
    {
        $exception = new TimeoutException('Operation timed out after 30 seconds');

        $this->assertSame('Operation timed out after 30 seconds', $exception->getMessage());
    }

    // JobException Tests

    #[Test]
    public function job_exception_has_correct_defaults(): void
    {
        $exception = new JobException('Job processing failed');

        $this->assertSame('Job processing failed', $exception->getMessage());
        $this->assertSame('JOB_ERROR', $exception->getErrorCode());
        $this->assertNull($exception->getJobId());
    }

    #[Test]
    public function job_exception_stores_job_id(): void
    {
        $exception = new JobException('Job failed', 'job_abc123');

        $this->assertSame('job_abc123', $exception->getJobId());
    }

    // Exception Hierarchy Tests

    #[Test]
    public function all_exceptions_extend_base_exception(): void
    {
        $exceptions = [
            new AuthenticationException(),
            new InsufficientCreditsException(),
            new RateLimitException(),
            new ValidationException('test'),
            new NetworkException(),
            new TimeoutException(),
            new JobException('test'),
        ];

        foreach ($exceptions as $exception) {
            $this->assertInstanceOf(RenamedExceptionBase::class, $exception);
            $this->assertInstanceOf(\Exception::class, $exception);
        }
    }

    #[Test]
    public function exceptions_can_be_caught_by_base_type(): void
    {
        $caught = false;

        try {
            throw new AuthenticationException('Test');
        } catch (RenamedExceptionBase $e) {
            $caught = true;
        }

        $this->assertTrue($caught);
    }
}
