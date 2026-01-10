<?php

declare(strict_types=1);

namespace Renamed\Tests;

use PHPUnit\Framework\Attributes\Test;
use PHPUnit\Framework\TestCase;
use Renamed\Models\ExtractResult;
use Renamed\Models\JobStatusResponse;
use Renamed\Models\PdfSplitResult;
use Renamed\Models\RenameResult;
use Renamed\Models\SplitDocument;
use Renamed\Models\Team;
use Renamed\Models\User;

final class ModelsTest extends TestCase
{
    // Team Model Tests

    #[Test]
    public function team_from_array_creates_instance_with_required_fields(): void
    {
        $data = [
            'id' => 'team_123',
            'name' => 'Acme Corp',
        ];

        $team = Team::fromArray($data);

        $this->assertSame('team_123', $team->id);
        $this->assertSame('Acme Corp', $team->name);
    }

    #[Test]
    public function team_to_array_returns_correct_structure(): void
    {
        $team = new Team('team_456', 'Test Team');

        $array = $team->toArray();

        $this->assertSame([
            'id' => 'team_456',
            'name' => 'Test Team',
        ], $array);
    }

    // User Model Tests

    #[Test]
    public function user_from_array_creates_instance_with_required_fields(): void
    {
        $data = [
            'id' => 'user_123',
            'email' => 'test@example.com',
        ];

        $user = User::fromArray($data);

        $this->assertSame('user_123', $user->id);
        $this->assertSame('test@example.com', $user->email);
        $this->assertNull($user->name);
        $this->assertNull($user->credits);
        $this->assertNull($user->team);
    }

    #[Test]
    public function user_from_array_creates_instance_with_all_fields(): void
    {
        $data = [
            'id' => 'user_456',
            'email' => 'john@example.com',
            'name' => 'John Doe',
            'credits' => 100,
            'team' => [
                'id' => 'team_789',
                'name' => 'Development',
            ],
        ];

        $user = User::fromArray($data);

        $this->assertSame('user_456', $user->id);
        $this->assertSame('john@example.com', $user->email);
        $this->assertSame('John Doe', $user->name);
        $this->assertSame(100, $user->credits);
        $this->assertNotNull($user->team);
        $this->assertSame('team_789', $user->team->id);
        $this->assertSame('Development', $user->team->name);
    }

    #[Test]
    public function user_from_array_casts_credits_to_int(): void
    {
        $data = [
            'id' => 'user_123',
            'email' => 'test@example.com',
            'credits' => '50', // String value
        ];

        $user = User::fromArray($data);

        $this->assertSame(50, $user->credits);
    }

    #[Test]
    public function user_to_array_returns_correct_structure(): void
    {
        $team = new Team('team_123', 'Test Team');
        $user = new User('user_456', 'test@example.com', 'Test User', 200, $team);

        $array = $user->toArray();

        $this->assertSame([
            'id' => 'user_456',
            'email' => 'test@example.com',
            'name' => 'Test User',
            'credits' => 200,
            'team' => [
                'id' => 'team_123',
                'name' => 'Test Team',
            ],
        ], $array);
    }

    #[Test]
    public function user_to_array_handles_null_team(): void
    {
        $user = new User('user_123', 'test@example.com');

        $array = $user->toArray();

        $this->assertNull($array['team']);
    }

    // RenameResult Model Tests

    #[Test]
    public function rename_result_from_array_creates_instance_with_required_fields(): void
    {
        $data = [
            'originalFilename' => 'document.pdf',
            'suggestedFilename' => '2025-01-15_Invoice_12345.pdf',
        ];

        $result = RenameResult::fromArray($data);

        $this->assertSame('document.pdf', $result->originalFilename);
        $this->assertSame('2025-01-15_Invoice_12345.pdf', $result->suggestedFilename);
        $this->assertNull($result->folderPath);
        $this->assertNull($result->confidence);
    }

    #[Test]
    public function rename_result_from_array_creates_instance_with_all_fields(): void
    {
        $data = [
            'originalFilename' => 'scan.pdf',
            'suggestedFilename' => '2025-01-15_AcmeCorp_Invoice.pdf',
            'folderPath' => 'Invoices/2025/January',
            'confidence' => 0.95,
        ];

        $result = RenameResult::fromArray($data);

        $this->assertSame('scan.pdf', $result->originalFilename);
        $this->assertSame('2025-01-15_AcmeCorp_Invoice.pdf', $result->suggestedFilename);
        $this->assertSame('Invoices/2025/January', $result->folderPath);
        $this->assertSame(0.95, $result->confidence);
    }

    #[Test]
    public function rename_result_from_array_casts_confidence_to_float(): void
    {
        $data = [
            'originalFilename' => 'doc.pdf',
            'suggestedFilename' => 'renamed.pdf',
            'confidence' => '0.85', // String value
        ];

        $result = RenameResult::fromArray($data);

        $this->assertSame(0.85, $result->confidence);
    }

    #[Test]
    public function rename_result_to_array_returns_correct_structure(): void
    {
        $result = new RenameResult(
            'original.pdf',
            'suggested.pdf',
            'Documents/Archive',
            0.9
        );

        $array = $result->toArray();

        $this->assertSame([
            'originalFilename' => 'original.pdf',
            'suggestedFilename' => 'suggested.pdf',
            'folderPath' => 'Documents/Archive',
            'confidence' => 0.9,
        ], $array);
    }

    // ExtractResult Model Tests

    #[Test]
    public function extract_result_from_array_creates_instance_with_data(): void
    {
        $data = [
            'data' => [
                'vendor' => 'Acme Corp',
                'amount' => 150.00,
                'date' => '2025-01-15',
            ],
            'confidence' => 0.92,
        ];

        $result = ExtractResult::fromArray($data);

        $this->assertSame([
            'vendor' => 'Acme Corp',
            'amount' => 150.00,
            'date' => '2025-01-15',
        ], $result->data);
        $this->assertSame(0.92, $result->confidence);
    }

    #[Test]
    public function extract_result_from_array_defaults_to_empty_data(): void
    {
        $data = [
            'confidence' => 0.5,
        ];

        $result = ExtractResult::fromArray($data);

        $this->assertSame([], $result->data);
    }

    #[Test]
    public function extract_result_from_array_defaults_confidence_to_zero(): void
    {
        $data = [
            'data' => ['key' => 'value'],
        ];

        $result = ExtractResult::fromArray($data);

        $this->assertSame(0.0, $result->confidence);
    }

    #[Test]
    public function extract_result_to_array_returns_correct_structure(): void
    {
        $result = new ExtractResult(['field' => 'value'], 0.88);

        $array = $result->toArray();

        $this->assertSame([
            'data' => ['field' => 'value'],
            'confidence' => 0.88,
        ], $array);
    }

    // SplitDocument Model Tests

    #[Test]
    public function split_document_from_array_creates_instance(): void
    {
        $data = [
            'index' => 0,
            'filename' => 'document_part1.pdf',
            'pages' => '1-5',
            'downloadUrl' => 'https://api.renamed.to/download/abc123',
            'size' => 1024000,
        ];

        $doc = SplitDocument::fromArray($data);

        $this->assertSame(0, $doc->index);
        $this->assertSame('document_part1.pdf', $doc->filename);
        $this->assertSame('1-5', $doc->pages);
        $this->assertSame('https://api.renamed.to/download/abc123', $doc->downloadUrl);
        $this->assertSame(1024000, $doc->size);
    }

    #[Test]
    public function split_document_from_array_casts_numeric_fields(): void
    {
        $data = [
            'index' => '2', // String
            'filename' => 'doc.pdf',
            'pages' => '10-15',
            'downloadUrl' => 'https://example.com/download',
            'size' => '512000', // String
        ];

        $doc = SplitDocument::fromArray($data);

        $this->assertSame(2, $doc->index);
        $this->assertSame(512000, $doc->size);
    }

    #[Test]
    public function split_document_to_array_returns_correct_structure(): void
    {
        $doc = new SplitDocument(
            1,
            'part2.pdf',
            '6-10',
            'https://api.renamed.to/dl/xyz',
            2048000
        );

        $array = $doc->toArray();

        $this->assertSame([
            'index' => 1,
            'filename' => 'part2.pdf',
            'pages' => '6-10',
            'downloadUrl' => 'https://api.renamed.to/dl/xyz',
            'size' => 2048000,
        ], $array);
    }

    // PdfSplitResult Model Tests

    #[Test]
    public function pdf_split_result_from_array_creates_instance(): void
    {
        $data = [
            'originalFilename' => 'combined.pdf',
            'totalPages' => 20,
            'documents' => [
                [
                    'index' => 0,
                    'filename' => 'part1.pdf',
                    'pages' => '1-10',
                    'downloadUrl' => 'https://api.renamed.to/dl/a',
                    'size' => 1000000,
                ],
                [
                    'index' => 1,
                    'filename' => 'part2.pdf',
                    'pages' => '11-20',
                    'downloadUrl' => 'https://api.renamed.to/dl/b',
                    'size' => 1100000,
                ],
            ],
        ];

        $result = PdfSplitResult::fromArray($data);

        $this->assertSame('combined.pdf', $result->originalFilename);
        $this->assertSame(20, $result->totalPages);
        $this->assertCount(2, $result->documents);
        $this->assertInstanceOf(SplitDocument::class, $result->documents[0]);
        $this->assertSame('part1.pdf', $result->documents[0]->filename);
        $this->assertSame('part2.pdf', $result->documents[1]->filename);
    }

    #[Test]
    public function pdf_split_result_from_array_handles_empty_documents(): void
    {
        $data = [
            'originalFilename' => 'empty.pdf',
            'totalPages' => 0,
        ];

        $result = PdfSplitResult::fromArray($data);

        $this->assertSame([], $result->documents);
    }

    #[Test]
    public function pdf_split_result_to_array_returns_correct_structure(): void
    {
        $doc = new SplitDocument(0, 'doc.pdf', '1-5', 'https://dl.com/x', 500000);
        $result = new PdfSplitResult('original.pdf', [$doc], 5);

        $array = $result->toArray();

        $this->assertSame('original.pdf', $array['originalFilename']);
        $this->assertSame(5, $array['totalPages']);
        $this->assertCount(1, $array['documents']);
        $this->assertSame('doc.pdf', $array['documents'][0]['filename']);
    }

    // JobStatusResponse Model Tests

    #[Test]
    public function job_status_response_from_array_creates_instance_with_required_fields(): void
    {
        $data = [
            'jobId' => 'job_abc123',
            'status' => 'pending',
        ];

        $response = JobStatusResponse::fromArray($data);

        $this->assertSame('job_abc123', $response->jobId);
        $this->assertSame('pending', $response->status);
        $this->assertNull($response->progress);
        $this->assertNull($response->error);
        $this->assertNull($response->result);
    }

    #[Test]
    public function job_status_response_from_array_creates_instance_with_progress(): void
    {
        $data = [
            'jobId' => 'job_xyz',
            'status' => 'processing',
            'progress' => 50,
        ];

        $response = JobStatusResponse::fromArray($data);

        $this->assertSame(50, $response->progress);
    }

    #[Test]
    public function job_status_response_from_array_creates_instance_with_error(): void
    {
        $data = [
            'jobId' => 'job_failed',
            'status' => 'failed',
            'error' => 'Processing failed due to corrupt PDF',
        ];

        $response = JobStatusResponse::fromArray($data);

        $this->assertSame('failed', $response->status);
        $this->assertSame('Processing failed due to corrupt PDF', $response->error);
    }

    #[Test]
    public function job_status_response_from_array_creates_instance_with_result(): void
    {
        $data = [
            'jobId' => 'job_complete',
            'status' => 'completed',
            'progress' => 100,
            'result' => [
                'originalFilename' => 'multi.pdf',
                'totalPages' => 10,
                'documents' => [
                    [
                        'index' => 0,
                        'filename' => 'split.pdf',
                        'pages' => '1-10',
                        'downloadUrl' => 'https://dl.com/file',
                        'size' => 100000,
                    ],
                ],
            ],
        ];

        $response = JobStatusResponse::fromArray($data);

        $this->assertNotNull($response->result);
        $this->assertInstanceOf(PdfSplitResult::class, $response->result);
        $this->assertSame('multi.pdf', $response->result->originalFilename);
    }

    #[Test]
    public function job_status_response_is_completed_returns_true_for_completed(): void
    {
        $response = new JobStatusResponse('job_1', 'completed');

        $this->assertTrue($response->isCompleted());
        $this->assertFalse($response->isFailed());
        $this->assertFalse($response->isProcessing());
    }

    #[Test]
    public function job_status_response_is_failed_returns_true_for_failed(): void
    {
        $response = new JobStatusResponse('job_2', 'failed', null, 'Error message');

        $this->assertFalse($response->isCompleted());
        $this->assertTrue($response->isFailed());
        $this->assertFalse($response->isProcessing());
    }

    #[Test]
    public function job_status_response_is_processing_returns_true_for_pending(): void
    {
        $response = new JobStatusResponse('job_3', 'pending');

        $this->assertFalse($response->isCompleted());
        $this->assertFalse($response->isFailed());
        $this->assertTrue($response->isProcessing());
    }

    #[Test]
    public function job_status_response_is_processing_returns_true_for_processing(): void
    {
        $response = new JobStatusResponse('job_4', 'processing', 50);

        $this->assertFalse($response->isCompleted());
        $this->assertFalse($response->isFailed());
        $this->assertTrue($response->isProcessing());
    }

    #[Test]
    public function job_status_response_to_array_returns_correct_structure(): void
    {
        $doc = new SplitDocument(0, 'output.pdf', '1-5', 'https://dl.com/f', 50000);
        $splitResult = new PdfSplitResult('input.pdf', [$doc], 5);
        $response = new JobStatusResponse('job_5', 'completed', 100, null, $splitResult);

        $array = $response->toArray();

        $this->assertSame('job_5', $array['jobId']);
        $this->assertSame('completed', $array['status']);
        $this->assertSame(100, $array['progress']);
        $this->assertNull($array['error']);
        $this->assertIsArray($array['result']);
        $this->assertSame('input.pdf', $array['result']['originalFilename']);
    }

    #[Test]
    public function job_status_response_to_array_handles_null_result(): void
    {
        $response = new JobStatusResponse('job_6', 'pending');

        $array = $response->toArray();

        $this->assertNull($array['result']);
    }
}
