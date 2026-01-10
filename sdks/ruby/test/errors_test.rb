# frozen_string_literal: true

require "test_helper"

class ErrorsTest < Minitest::Test
  # Base Error tests

  def test_error_default_message
    error = Renamed::Error.new

    assert_equal "An error occurred", error.message
    assert_equal "UNKNOWN_ERROR", error.code
    assert_nil error.status_code
    assert_nil error.details
  end

  def test_error_with_custom_attributes
    error = Renamed::Error.new(
      "Custom error message",
      code: "CUSTOM_CODE",
      status_code: 500,
      details: { field: "value" }
    )

    assert_equal "Custom error message", error.message
    assert_equal "CUSTOM_CODE", error.code
    assert_equal 500, error.status_code
    assert_equal({ field: "value" }, error.details)
  end

  def test_error_inherits_from_standard_error
    error = Renamed::Error.new("test")

    assert_kind_of StandardError, error
  end

  # AuthenticationError tests

  def test_authentication_error_default
    error = Renamed::AuthenticationError.new

    assert_equal "Invalid or missing API key", error.message
    assert_equal "AUTHENTICATION_ERROR", error.code
    assert_equal 401, error.status_code
  end

  def test_authentication_error_with_custom_message
    error = Renamed::AuthenticationError.new("API key is expired")

    assert_equal "API key is expired", error.message
    assert_equal "AUTHENTICATION_ERROR", error.code
    assert_equal 401, error.status_code
  end

  def test_authentication_error_inherits_from_error
    error = Renamed::AuthenticationError.new

    assert_kind_of Renamed::Error, error
  end

  # InsufficientCreditsError tests

  def test_insufficient_credits_error_default
    error = Renamed::InsufficientCreditsError.new

    assert_equal "Insufficient credits", error.message
    assert_equal "INSUFFICIENT_CREDITS", error.code
    assert_equal 402, error.status_code
  end

  def test_insufficient_credits_error_with_custom_message
    error = Renamed::InsufficientCreditsError.new("You need 10 more credits")

    assert_equal "You need 10 more credits", error.message
    assert_equal "INSUFFICIENT_CREDITS", error.code
  end

  def test_insufficient_credits_error_inherits_from_error
    error = Renamed::InsufficientCreditsError.new

    assert_kind_of Renamed::Error, error
  end

  # RateLimitError tests

  def test_rate_limit_error_default
    error = Renamed::RateLimitError.new

    assert_equal "Rate limit exceeded", error.message
    assert_equal "RATE_LIMIT_ERROR", error.code
    assert_equal 429, error.status_code
    assert_nil error.retry_after
  end

  def test_rate_limit_error_with_retry_after
    error = Renamed::RateLimitError.new("Too many requests", retry_after: 60)

    assert_equal "Too many requests", error.message
    assert_equal 60, error.retry_after
  end

  def test_rate_limit_error_inherits_from_error
    error = Renamed::RateLimitError.new

    assert_kind_of Renamed::Error, error
  end

  # ValidationError tests

  def test_validation_error
    error = Renamed::ValidationError.new("Invalid file format")

    assert_equal "Invalid file format", error.message
    assert_equal "VALIDATION_ERROR", error.code
    assert_equal 400, error.status_code
    assert_nil error.details
  end

  def test_validation_error_with_details
    details = { field: "file", errors: ["must be PDF or image"] }
    error = Renamed::ValidationError.new("Validation failed", details: details)

    assert_equal "Validation failed", error.message
    assert_equal details, error.details
  end

  def test_validation_error_inherits_from_error
    error = Renamed::ValidationError.new("test")

    assert_kind_of Renamed::Error, error
  end

  # NetworkError tests

  def test_network_error_default
    error = Renamed::NetworkError.new

    assert_equal "Network request failed", error.message
    assert_equal "NETWORK_ERROR", error.code
    assert_nil error.status_code
  end

  def test_network_error_with_custom_message
    error = Renamed::NetworkError.new("Connection refused")

    assert_equal "Connection refused", error.message
    assert_equal "NETWORK_ERROR", error.code
  end

  def test_network_error_inherits_from_error
    error = Renamed::NetworkError.new

    assert_kind_of Renamed::Error, error
  end

  # TimeoutError tests

  def test_timeout_error_default
    error = Renamed::TimeoutError.new

    assert_equal "Request timed out", error.message
    assert_equal "TIMEOUT_ERROR", error.code
    assert_nil error.status_code
  end

  def test_timeout_error_with_custom_message
    error = Renamed::TimeoutError.new("Connection timed out after 30s")

    assert_equal "Connection timed out after 30s", error.message
  end

  def test_timeout_error_inherits_from_error
    error = Renamed::TimeoutError.new

    assert_kind_of Renamed::Error, error
  end

  # JobError tests

  def test_job_error
    error = Renamed::JobError.new("Job processing failed")

    assert_equal "Job processing failed", error.message
    assert_equal "JOB_ERROR", error.code
    assert_nil error.job_id
  end

  def test_job_error_with_job_id
    error = Renamed::JobError.new("Job failed", job_id: "job_123")

    assert_equal "Job failed", error.message
    assert_equal "job_123", error.job_id
  end

  def test_job_error_inherits_from_error
    error = Renamed::JobError.new("test")

    assert_kind_of Renamed::Error, error
  end

  # error_from_http_status tests

  def test_error_from_http_status_401
    error = Renamed.error_from_http_status(401, "Unauthorized")

    assert_instance_of Renamed::AuthenticationError, error
    assert_equal "Unauthorized", error.message
  end

  def test_error_from_http_status_401_with_payload
    payload = { "error" => "Invalid API key" }
    error = Renamed.error_from_http_status(401, "Unauthorized", payload)

    assert_instance_of Renamed::AuthenticationError, error
    assert_equal "Invalid API key", error.message
  end

  def test_error_from_http_status_402
    error = Renamed.error_from_http_status(402, "Payment Required")

    assert_instance_of Renamed::InsufficientCreditsError, error
  end

  def test_error_from_http_status_400
    error = Renamed.error_from_http_status(400, "Bad Request")

    assert_instance_of Renamed::ValidationError, error
  end

  def test_error_from_http_status_422
    payload = { "error" => "Unprocessable entity", "field" => "file" }
    error = Renamed.error_from_http_status(422, "Unprocessable Entity", payload)

    assert_instance_of Renamed::ValidationError, error
    assert_equal "Unprocessable entity", error.message
    assert_equal payload, error.details
  end

  def test_error_from_http_status_429
    error = Renamed.error_from_http_status(429, "Too Many Requests")

    assert_instance_of Renamed::RateLimitError, error
  end

  def test_error_from_http_status_429_with_retry_after
    payload = { "error" => "Rate limited", "retryAfter" => 120 }
    error = Renamed.error_from_http_status(429, "Too Many Requests", payload)

    assert_instance_of Renamed::RateLimitError, error
    assert_equal 120, error.retry_after
  end

  def test_error_from_http_status_500
    error = Renamed.error_from_http_status(500, "Internal Server Error")

    assert_instance_of Renamed::Error, error
    assert_equal "Internal Server Error", error.message
    assert_equal "API_ERROR", error.code
    assert_equal 500, error.status_code
  end

  def test_error_from_http_status_503
    payload = { "error" => "Service temporarily unavailable" }
    error = Renamed.error_from_http_status(503, "Service Unavailable", payload)

    assert_instance_of Renamed::Error, error
    assert_equal "Service temporarily unavailable", error.message
    assert_equal 503, error.status_code
    assert_equal payload, error.details
  end

  def test_error_from_http_status_with_string_payload
    error = Renamed.error_from_http_status(500, "Internal Server Error", "Something went wrong")

    assert_instance_of Renamed::Error, error
    assert_equal "Internal Server Error", error.message
    assert_equal "Something went wrong", error.details
  end

  # Errors can be raised and caught

  def test_errors_can_be_raised_and_caught_by_base_class
    assert_raises(Renamed::Error) do
      raise Renamed::AuthenticationError.new
    end

    assert_raises(Renamed::Error) do
      raise Renamed::ValidationError.new("test")
    end

    assert_raises(Renamed::Error) do
      raise Renamed::RateLimitError.new
    end
  end

  def test_errors_can_be_caught_by_standard_error
    assert_raises(StandardError) do
      raise Renamed::Error.new
    end
  end
end
