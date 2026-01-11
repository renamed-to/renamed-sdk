<?php

declare(strict_types=1);

namespace Renamed\Tests;

use GuzzleHttp\Handler\MockHandler;
use GuzzleHttp\Psr7\Response;
use PHPUnit\Framework\Attributes\Test;
use Renamed\AsyncJob;
use Renamed\Exceptions\JobException;
use Renamed\Models\JobStatusResponse;

final class AsyncJobTest extends TestCase
{
    #[Test]
    public function constructor_stores_status_url(): void
    {
        $client = $this->createMockClient([]);
        $job = new AsyncJob($client, 'https://api.renamed.to/status/job_123');

        $this->assertSame('https://api.renamed.to/status/job_123', $job->getStatusUrl());
    }

    #[Test]
    public function status_returns_job_status_response(): void
    {
        $mockData = [
            'jobId' => 'job_123',
            'status' => 'processing',
            'progress' => 50,
        ];

        $client = $this->createMockClient([
            $this->jsonResponse(200, $mockData),
        ]);

        $job = new AsyncJob($client, 'https://api.renamed.to/status/job_123');
        $status = $job->status();

        $this->assertInstanceOf(JobStatusResponse::class, $status);
        $this->assertSame('job_123', $status->jobId);
        $this->assertSame('processing', $status->status);
        $this->assertSame(50, $status->progress);
    }

    #[Test]
    public function wait_polls_until_completed(): void
    {
        $mockResult = [
            'originalFilename' => 'multi.pdf',
            'totalPages' => 10,
            'documents' => [
                [
                    'index' => 0,
                    'filename' => 'doc1.pdf',
                    'pages' => '1-5',
                    'downloadUrl' => 'https://example.com/dl/1',
                    'size' => 100000,
                ],
            ],
        ];

        $client = $this->createMockClient([
            $this->jsonResponse(200, ['jobId' => 'job_1', 'status' => 'processing', 'progress' => 33]),
            $this->jsonResponse(200, ['jobId' => 'job_1', 'status' => 'processing', 'progress' => 66]),
            $this->jsonResponse(200, ['jobId' => 'job_1', 'status' => 'completed', 'progress' => 100, 'result' => $mockResult]),
        ]);

        // Use very small poll interval for testing
        $job = new AsyncJob($client, 'https://api.renamed.to/status/job_1', 0.001, 10);

        $result = $job->wait();

        $this->assertSame('multi.pdf', $result->originalFilename);
        $this->assertSame(10, $result->totalPages);
        $this->assertCount(1, $result->documents);
        $this->assertSame('doc1.pdf', $result->documents[0]->filename);
    }

    #[Test]
    public function wait_invokes_progress_callback(): void
    {
        $mockResult = [
            'originalFilename' => 'test.pdf',
            'totalPages' => 5,
            'documents' => [],
        ];

        $client = $this->createMockClient([
            $this->jsonResponse(200, ['jobId' => 'job_1', 'status' => 'processing', 'progress' => 25]),
            $this->jsonResponse(200, ['jobId' => 'job_1', 'status' => 'processing', 'progress' => 75]),
            $this->jsonResponse(200, ['jobId' => 'job_1', 'status' => 'completed', 'progress' => 100, 'result' => $mockResult]),
        ]);

        $job = new AsyncJob($client, 'https://api.renamed.to/status/job_1', 0.001, 10);

        $progressUpdates = [];
        $job->wait(function (JobStatusResponse $status) use (&$progressUpdates) {
            $progressUpdates[] = $status->progress;
        });

        $this->assertContains(25, $progressUpdates);
        $this->assertContains(75, $progressUpdates);
        $this->assertContains(100, $progressUpdates);
    }

    #[Test]
    public function wait_throws_job_exception_on_failure(): void
    {
        $client = $this->createMockClient([
            $this->jsonResponse(200, ['jobId' => 'job_1', 'status' => 'failed', 'error' => 'PDF is corrupted']),
        ]);

        $job = new AsyncJob($client, 'https://api.renamed.to/status/job_1', 0.001, 10);

        $this->expectException(JobException::class);
        $this->expectExceptionMessage('PDF is corrupted');

        $job->wait();
    }

    #[Test]
    public function wait_throws_job_exception_on_timeout(): void
    {
        // Only enqueue processing responses, never completed
        $client = $this->createMockClient([
            $this->jsonResponse(200, ['jobId' => 'job_1', 'status' => 'processing', 'progress' => 10]),
            $this->jsonResponse(200, ['jobId' => 'job_1', 'status' => 'processing', 'progress' => 20]),
            $this->jsonResponse(200, ['jobId' => 'job_1', 'status' => 'processing', 'progress' => 30]),
        ]);

        // Only allow 3 attempts
        $job = new AsyncJob($client, 'https://api.renamed.to/status/job_1', 0.001, 3);

        $this->expectException(JobException::class);
        $this->expectExceptionMessage('timeout');

        $job->wait();
    }

    #[Test]
    public function wait_returns_result_with_multiple_documents(): void
    {
        $mockResult = [
            'originalFilename' => 'big.pdf',
            'totalPages' => 20,
            'documents' => [
                [
                    'index' => 0,
                    'filename' => 'part1.pdf',
                    'pages' => '1-5',
                    'downloadUrl' => 'https://example.com/dl/1',
                    'size' => 50000,
                ],
                [
                    'index' => 1,
                    'filename' => 'part2.pdf',
                    'pages' => '6-10',
                    'downloadUrl' => 'https://example.com/dl/2',
                    'size' => 60000,
                ],
                [
                    'index' => 2,
                    'filename' => 'part3.pdf',
                    'pages' => '11-20',
                    'downloadUrl' => 'https://example.com/dl/3',
                    'size' => 80000,
                ],
            ],
        ];

        $client = $this->createMockClient([
            $this->jsonResponse(200, ['jobId' => 'job_1', 'status' => 'completed', 'progress' => 100, 'result' => $mockResult]),
        ]);

        $job = new AsyncJob($client, 'https://api.renamed.to/status/job_1', 0.001, 10);
        $result = $job->wait();

        $this->assertCount(3, $result->documents);
        $this->assertSame('part1.pdf', $result->documents[0]->filename);
        $this->assertSame('part2.pdf', $result->documents[1]->filename);
        $this->assertSame('part3.pdf', $result->documents[2]->filename);
    }

    #[Test]
    public function get_status_url_returns_stored_url(): void
    {
        $client = $this->createMockClient([]);
        $statusUrl = 'https://api.renamed.to/status/job_xyz';
        $job = new AsyncJob($client, $statusUrl);

        $this->assertSame($statusUrl, $job->getStatusUrl());
    }
}
