# frozen_string_literal: true

require "test_helper"

class ModelsTest < Minitest::Test
  # RenameResult tests

  def test_rename_result_from_response
    data = {
      "originalFilename" => "document.pdf",
      "suggestedFilename" => "2025-01-15_Invoice.pdf",
      "folderPath" => "Invoices/2025",
      "confidence" => 0.95
    }

    result = Renamed::RenameResult.from_response(data)

    assert_equal "document.pdf", result.original_filename
    assert_equal "2025-01-15_Invoice.pdf", result.suggested_filename
    assert_equal "Invoices/2025", result.folder_path
    assert_equal 0.95, result.confidence
  end

  def test_rename_result_from_response_with_nil_optional_fields
    data = {
      "originalFilename" => "document.pdf",
      "suggestedFilename" => "renamed.pdf"
    }

    result = Renamed::RenameResult.from_response(data)

    assert_equal "document.pdf", result.original_filename
    assert_equal "renamed.pdf", result.suggested_filename
    assert_nil result.folder_path
    assert_nil result.confidence
  end

  def test_rename_result_to_h
    result = Renamed::RenameResult.new(
      original_filename: "test.pdf",
      suggested_filename: "renamed.pdf",
      folder_path: "Documents",
      confidence: 0.9
    )

    hash = result.to_h

    assert_equal "test.pdf", hash[:original_filename]
    assert_equal "renamed.pdf", hash[:suggested_filename]
    assert_equal "Documents", hash[:folder_path]
    assert_equal 0.9, hash[:confidence]
  end

  def test_rename_result_to_h_omits_nil_values
    result = Renamed::RenameResult.new(
      original_filename: "test.pdf",
      suggested_filename: "renamed.pdf"
    )

    hash = result.to_h

    assert_equal({ original_filename: "test.pdf", suggested_filename: "renamed.pdf" }, hash)
    refute hash.key?(:folder_path)
    refute hash.key?(:confidence)
  end

  # SplitDocument tests

  def test_split_document_from_response
    data = {
      "index" => 0,
      "filename" => "document_part1.pdf",
      "pages" => "1-5",
      "downloadUrl" => "https://example.com/download/123",
      "size" => 12345
    }

    doc = Renamed::SplitDocument.from_response(data)

    assert_equal 0, doc.index
    assert_equal "document_part1.pdf", doc.filename
    assert_equal "1-5", doc.pages
    assert_equal "https://example.com/download/123", doc.download_url
    assert_equal 12_345, doc.size
  end

  def test_split_document_to_h
    doc = Renamed::SplitDocument.new(
      index: 1,
      filename: "part2.pdf",
      pages: "6-10",
      download_url: "https://example.com/file",
      size: 5000
    )

    hash = doc.to_h

    assert_equal 1, hash[:index]
    assert_equal "part2.pdf", hash[:filename]
    assert_equal "6-10", hash[:pages]
    assert_equal "https://example.com/file", hash[:download_url]
    assert_equal 5000, hash[:size]
  end

  # PdfSplitResult tests

  def test_pdf_split_result_from_response
    data = {
      "originalFilename" => "multi-page.pdf",
      "totalPages" => 10,
      "documents" => [
        {
          "index" => 0,
          "filename" => "part1.pdf",
          "pages" => "1-5",
          "downloadUrl" => "https://example.com/1",
          "size" => 1000
        },
        {
          "index" => 1,
          "filename" => "part2.pdf",
          "pages" => "6-10",
          "downloadUrl" => "https://example.com/2",
          "size" => 2000
        }
      ]
    }

    result = Renamed::PdfSplitResult.from_response(data)

    assert_equal "multi-page.pdf", result.original_filename
    assert_equal 10, result.total_pages
    assert_equal 2, result.documents.length
    assert_instance_of Renamed::SplitDocument, result.documents[0]
    assert_equal "part1.pdf", result.documents[0].filename
    assert_equal "part2.pdf", result.documents[1].filename
  end

  def test_pdf_split_result_from_response_with_empty_documents
    data = {
      "originalFilename" => "empty.pdf",
      "totalPages" => 0
    }

    result = Renamed::PdfSplitResult.from_response(data)

    assert_equal "empty.pdf", result.original_filename
    assert_equal 0, result.total_pages
    assert_empty result.documents
  end

  def test_pdf_split_result_to_h
    result = Renamed::PdfSplitResult.new(
      original_filename: "test.pdf",
      total_pages: 5,
      documents: [
        Renamed::SplitDocument.new(
          index: 0,
          filename: "part1.pdf",
          pages: "1-5",
          download_url: "https://example.com/1",
          size: 1000
        )
      ]
    )

    hash = result.to_h

    assert_equal "test.pdf", hash[:original_filename]
    assert_equal 5, hash[:total_pages]
    assert_equal 1, hash[:documents].length
    assert_equal "part1.pdf", hash[:documents][0][:filename]
  end

  # JobStatusResponse tests

  def test_job_status_response_from_response_pending
    data = {
      "jobId" => "job_123",
      "status" => "pending",
      "progress" => 0
    }

    response = Renamed::JobStatusResponse.from_response(data)

    assert_equal "job_123", response.job_id
    assert_equal "pending", response.status
    assert_equal 0, response.progress
    assert_nil response.error
    assert_nil response.result
  end

  def test_job_status_response_from_response_processing
    data = {
      "jobId" => "job_123",
      "status" => "processing",
      "progress" => 50
    }

    response = Renamed::JobStatusResponse.from_response(data)

    assert_equal "job_123", response.job_id
    assert_equal "processing", response.status
    assert_equal 50, response.progress
  end

  def test_job_status_response_from_response_completed
    data = {
      "jobId" => "job_123",
      "status" => "completed",
      "progress" => 100,
      "result" => {
        "originalFilename" => "doc.pdf",
        "totalPages" => 5,
        "documents" => []
      }
    }

    response = Renamed::JobStatusResponse.from_response(data)

    assert_equal "job_123", response.job_id
    assert_equal "completed", response.status
    assert_equal 100, response.progress
    assert_instance_of Renamed::PdfSplitResult, response.result
  end

  def test_job_status_response_from_response_failed
    data = {
      "jobId" => "job_123",
      "status" => "failed",
      "error" => "Processing failed: invalid PDF"
    }

    response = Renamed::JobStatusResponse.from_response(data)

    assert_equal "job_123", response.job_id
    assert_equal "failed", response.status
    assert_equal "Processing failed: invalid PDF", response.error
  end

  def test_job_status_response_to_h
    response = Renamed::JobStatusResponse.new(
      job_id: "job_456",
      status: "processing",
      progress: 75
    )

    hash = response.to_h

    assert_equal "job_456", hash[:job_id]
    assert_equal "processing", hash[:status]
    assert_equal 75, hash[:progress]
  end

  # ExtractResult tests

  def test_extract_result_from_response
    data = {
      "data" => {
        "vendor" => "Acme Corp",
        "amount" => 150.00,
        "date" => "2025-01-15"
      },
      "confidence" => 0.88
    }

    result = Renamed::ExtractResult.from_response(data)

    assert_equal({ "vendor" => "Acme Corp", "amount" => 150.00, "date" => "2025-01-15" }, result.data)
    assert_equal 0.88, result.confidence
  end

  def test_extract_result_to_h
    result = Renamed::ExtractResult.new(
      data: { "key" => "value" },
      confidence: 0.95
    )

    hash = result.to_h

    assert_equal({ "key" => "value" }, hash[:data])
    assert_equal 0.95, hash[:confidence]
  end

  # Team tests

  def test_team_from_response
    data = {
      "id" => "team_789",
      "name" => "Engineering Team"
    }

    team = Renamed::Team.from_response(data)

    assert_equal "team_789", team.id
    assert_equal "Engineering Team", team.name
  end

  def test_team_to_h
    team = Renamed::Team.new(id: "team_123", name: "Test Team")

    hash = team.to_h

    assert_equal({ id: "team_123", name: "Test Team" }, hash)
  end

  # User tests

  def test_user_from_response
    data = {
      "id" => "user_123",
      "email" => "user@example.com",
      "name" => "Test User",
      "credits" => 500
    }

    user = Renamed::User.from_response(data)

    assert_equal "user_123", user.id
    assert_equal "user@example.com", user.email
    assert_equal "Test User", user.name
    assert_equal 500, user.credits
    assert_nil user.team
  end

  def test_user_from_response_with_team
    data = {
      "id" => "user_123",
      "email" => "user@example.com",
      "team" => {
        "id" => "team_456",
        "name" => "Acme Corp"
      }
    }

    user = Renamed::User.from_response(data)

    assert_instance_of Renamed::Team, user.team
    assert_equal "team_456", user.team.id
    assert_equal "Acme Corp", user.team.name
  end

  def test_user_to_h
    user = Renamed::User.new(
      id: "user_123",
      email: "test@example.com",
      name: "Test User",
      credits: 100
    )

    hash = user.to_h

    assert_equal "user_123", hash[:id]
    assert_equal "test@example.com", hash[:email]
    assert_equal "Test User", hash[:name]
    assert_equal 100, hash[:credits]
  end

  def test_user_to_h_with_team
    user = Renamed::User.new(
      id: "user_123",
      email: "test@example.com",
      team: Renamed::Team.new(id: "team_456", name: "My Team")
    )

    hash = user.to_h

    assert_equal "team_456", hash[:team][:id]
    assert_equal "My Team", hash[:team][:name]
  end

  def test_user_to_h_omits_nil_values
    user = Renamed::User.new(id: "user_123", email: "test@example.com")

    hash = user.to_h

    assert_equal({ id: "user_123", email: "test@example.com" }, hash)
    refute hash.key?(:name)
    refute hash.key?(:credits)
    refute hash.key?(:team)
  end

  # MIME_TYPES constant test

  def test_mime_types_constant
    assert_equal "application/pdf", Renamed::MIME_TYPES[".pdf"]
    assert_equal "image/jpeg", Renamed::MIME_TYPES[".jpg"]
    assert_equal "image/jpeg", Renamed::MIME_TYPES[".jpeg"]
    assert_equal "image/png", Renamed::MIME_TYPES[".png"]
    assert_equal "image/tiff", Renamed::MIME_TYPES[".tiff"]
    assert_equal "image/tiff", Renamed::MIME_TYPES[".tif"]
  end

  def test_mime_types_is_frozen
    assert Renamed::MIME_TYPES.frozen?
  end
end
