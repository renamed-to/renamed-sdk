# frozen_string_literal: true

require "test_helper"

class ClientTest < Minitest::Test
  def setup
    @api_key = "rt_test_api_key_12345"
    @base_url = "https://www.renamed.to/api/v1"
  end

  # Initialization tests

  def test_initialize_with_valid_api_key
    client = Renamed::Client.new(api_key: @api_key)

    assert_instance_of Renamed::Client, client
  end

  def test_initialize_with_custom_base_url
    custom_url = "https://custom.api.com/v2"
    client = Renamed::Client.new(api_key: @api_key, base_url: custom_url)

    assert_instance_of Renamed::Client, client
  end

  def test_initialize_with_custom_timeout
    client = Renamed::Client.new(api_key: @api_key, timeout: 60)

    assert_instance_of Renamed::Client, client
  end

  def test_initialize_with_custom_max_retries
    client = Renamed::Client.new(api_key: @api_key, max_retries: 5)

    assert_instance_of Renamed::Client, client
  end

  def test_initialize_raises_error_when_api_key_is_nil
    error = assert_raises(Renamed::AuthenticationError) do
      Renamed::Client.new(api_key: nil)
    end

    assert_equal "API key is required", error.message
  end

  def test_initialize_raises_error_when_api_key_is_empty
    error = assert_raises(Renamed::AuthenticationError) do
      Renamed::Client.new(api_key: "")
    end

    assert_equal "API key is required", error.message
  end

  # get_user tests

  def test_get_user_returns_user_object
    stub_request(:get, "#{@base_url}/user")
      .with(headers: { "Authorization" => "Bearer #{@api_key}" })
      .to_return(
        status: 200,
        body: {
          id: "user_123",
          email: "test@example.com",
          name: "Test User",
          credits: 100
        }.to_json,
        headers: { "Content-Type" => "application/json" }
      )

    client = Renamed::Client.new(api_key: @api_key)
    user = client.get_user

    assert_instance_of Renamed::User, user
    assert_equal "user_123", user.id
    assert_equal "test@example.com", user.email
    assert_equal "Test User", user.name
    assert_equal 100, user.credits
  end

  def test_get_user_with_team
    stub_request(:get, "#{@base_url}/user")
      .with(headers: { "Authorization" => "Bearer #{@api_key}" })
      .to_return(
        status: 200,
        body: {
          id: "user_123",
          email: "test@example.com",
          credits: 50,
          team: {
            id: "team_456",
            name: "Acme Corp"
          }
        }.to_json,
        headers: { "Content-Type" => "application/json" }
      )

    client = Renamed::Client.new(api_key: @api_key)
    user = client.get_user

    assert_instance_of Renamed::Team, user.team
    assert_equal "team_456", user.team.id
    assert_equal "Acme Corp", user.team.name
  end

  def test_get_user_raises_authentication_error_on_401
    stub_request(:get, "#{@base_url}/user")
      .to_return(
        status: 401,
        body: { error: "Invalid API key" }.to_json,
        headers: { "Content-Type" => "application/json" }
      )

    client = Renamed::Client.new(api_key: @api_key)

    assert_raises(Renamed::AuthenticationError) do
      client.get_user
    end
  end

  # rename tests

  def test_rename_returns_rename_result
    stub_request(:post, "#{@base_url}/rename")
      .with(headers: { "Authorization" => "Bearer #{@api_key}" })
      .to_return(
        status: 200,
        body: {
          originalFilename: "document.pdf",
          suggestedFilename: "2025-01-15_Invoice_AcmeCorp.pdf",
          folderPath: "Invoices/2025",
          confidence: 0.95
        }.to_json,
        headers: { "Content-Type" => "application/json" }
      )

    # Create a temporary file for testing
    temp_file = Tempfile.new(["test", ".pdf"])
    temp_file.write("test content")
    temp_file.rewind

    client = Renamed::Client.new(api_key: @api_key)
    result = client.rename(temp_file.path)

    assert_instance_of Renamed::RenameResult, result
    assert_equal "document.pdf", result.original_filename
    assert_equal "2025-01-15_Invoice_AcmeCorp.pdf", result.suggested_filename
    assert_equal "Invoices/2025", result.folder_path
    assert_equal 0.95, result.confidence
  ensure
    temp_file&.close
    temp_file&.unlink
  end

  def test_rename_raises_insufficient_credits_error_on_402
    stub_request(:post, "#{@base_url}/rename")
      .to_return(
        status: 402,
        body: { error: "Insufficient credits" }.to_json,
        headers: { "Content-Type" => "application/json" }
      )

    temp_file = Tempfile.new(["test", ".pdf"])
    temp_file.write("test content")
    temp_file.rewind

    client = Renamed::Client.new(api_key: @api_key)

    assert_raises(Renamed::InsufficientCreditsError) do
      client.rename(temp_file.path)
    end
  ensure
    temp_file&.close
    temp_file&.unlink
  end

  def test_rename_raises_rate_limit_error_on_429
    stub_request(:post, "#{@base_url}/rename")
      .to_return(
        status: 429,
        body: { error: "Rate limit exceeded", retryAfter: 60 }.to_json,
        headers: { "Content-Type" => "application/json" }
      )

    temp_file = Tempfile.new(["test", ".pdf"])
    temp_file.write("test content")
    temp_file.rewind

    client = Renamed::Client.new(api_key: @api_key)

    error = assert_raises(Renamed::RateLimitError) do
      client.rename(temp_file.path)
    end

    assert_equal 60, error.retry_after
  ensure
    temp_file&.close
    temp_file&.unlink
  end

  # extract tests

  def test_extract_returns_extract_result
    stub_request(:post, "#{@base_url}/extract")
      .with(headers: { "Authorization" => "Bearer #{@api_key}" })
      .to_return(
        status: 200,
        body: {
          data: { vendor: "Acme Corp", total: 150.00 },
          confidence: 0.92
        }.to_json,
        headers: { "Content-Type" => "application/json" }
      )

    temp_file = Tempfile.new(["test", ".pdf"])
    temp_file.write("test content")
    temp_file.rewind

    client = Renamed::Client.new(api_key: @api_key)
    result = client.extract(temp_file.path, options: { prompt: "Extract invoice details" })

    assert_instance_of Renamed::ExtractResult, result
    assert_equal({ "vendor" => "Acme Corp", "total" => 150.00 }, result.data)
    assert_equal 0.92, result.confidence
  ensure
    temp_file&.close
    temp_file&.unlink
  end

  # pdf_split tests

  def test_pdf_split_returns_async_job
    stub_request(:post, "#{@base_url}/pdf-split")
      .with(headers: { "Authorization" => "Bearer #{@api_key}" })
      .to_return(
        status: 200,
        body: {
          statusUrl: "https://www.renamed.to/api/v1/jobs/job_123/status"
        }.to_json,
        headers: { "Content-Type" => "application/json" }
      )

    temp_file = Tempfile.new(["test", ".pdf"])
    temp_file.write("test content")
    temp_file.rewind

    client = Renamed::Client.new(api_key: @api_key)
    job = client.pdf_split(temp_file.path, options: { mode: "auto" })

    assert_instance_of Renamed::AsyncJob, job
    assert_equal "https://www.renamed.to/api/v1/jobs/job_123/status", job.status_url
  ensure
    temp_file&.close
    temp_file&.unlink
  end

  # download_file tests

  def test_download_file_returns_content
    download_url = "https://www.renamed.to/api/v1/downloads/file_123"
    file_content = "PDF binary content here"

    stub_request(:get, download_url)
      .with(headers: { "Authorization" => "Bearer #{@api_key}" })
      .to_return(
        status: 200,
        body: file_content,
        headers: { "Content-Type" => "application/pdf" }
      )

    client = Renamed::Client.new(api_key: @api_key)
    content = client.download_file(download_url)

    assert_equal file_content, content
  end

  def test_download_file_raises_error_on_404
    download_url = "https://www.renamed.to/api/v1/downloads/file_notfound"

    stub_request(:get, download_url)
      .to_return(
        status: 404,
        body: { error: "File not found" }.to_json,
        headers: { "Content-Type" => "application/json" }
      )

    client = Renamed::Client.new(api_key: @api_key)

    assert_raises(Renamed::Error) do
      client.download_file(download_url)
    end
  end

  # Retry behavior tests

  def test_retries_on_network_error
    stub_request(:get, "#{@base_url}/user")
      .with(headers: { "Authorization" => "Bearer #{@api_key}" })
      .to_timeout
      .then
      .to_return(
        status: 200,
        body: { id: "user_123", email: "test@example.com" }.to_json,
        headers: { "Content-Type" => "application/json" }
      )

    client = Renamed::Client.new(api_key: @api_key, max_retries: 2)
    user = client.get_user

    assert_equal "user_123", user.id
  end

  def test_does_not_retry_on_client_error
    stub_request(:get, "#{@base_url}/user")
      .to_return(
        status: 400,
        body: { error: "Bad request" }.to_json,
        headers: { "Content-Type" => "application/json" }
      )

    client = Renamed::Client.new(api_key: @api_key, max_retries: 2)

    assert_raises(Renamed::ValidationError) do
      client.get_user
    end

    # Verify only one request was made (no retries)
    assert_requested :get, "#{@base_url}/user", times: 1
  end
end
