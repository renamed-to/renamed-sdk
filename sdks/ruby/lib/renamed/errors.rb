# frozen_string_literal: true

module Renamed
  # Base exception for all renamed.to SDK errors.
  class Error < StandardError
    attr_reader :message, :code, :status_code, :details

    def initialize(message = "An error occurred", code: "UNKNOWN_ERROR", status_code: nil, details: nil)
      @message = message
      @code = code
      @status_code = status_code
      @details = details
      super(message)
    end
  end

  # Authentication error - invalid or missing API key.
  class AuthenticationError < Error
    def initialize(message = "Invalid or missing API key")
      super(message, code: "AUTHENTICATION_ERROR", status_code: 401)
    end
  end

  # Insufficient credits error.
  class InsufficientCreditsError < Error
    def initialize(message = "Insufficient credits")
      super(message, code: "INSUFFICIENT_CREDITS", status_code: 402)
    end
  end

  # Rate limit exceeded.
  class RateLimitError < Error
    attr_reader :retry_after

    def initialize(message = "Rate limit exceeded", retry_after: nil)
      super(message, code: "RATE_LIMIT_ERROR", status_code: 429)
      @retry_after = retry_after
    end
  end

  # Validation error - invalid request parameters.
  class ValidationError < Error
    def initialize(message, details: nil)
      super(message, code: "VALIDATION_ERROR", status_code: 400, details: details)
    end
  end

  # Network error - connection failed.
  class NetworkError < Error
    def initialize(message = "Network request failed")
      super(message, code: "NETWORK_ERROR")
    end
  end

  # Timeout error - request took too long.
  class TimeoutError < Error
    def initialize(message = "Request timed out")
      super(message, code: "TIMEOUT_ERROR")
    end
  end

  # Job error - async job failed.
  class JobError < Error
    attr_reader :job_id

    def initialize(message, job_id: nil)
      super(message, code: "JOB_ERROR")
      @job_id = job_id
    end
  end

  # Create appropriate error from HTTP status code.
  #
  # @param status [Integer] HTTP status code
  # @param status_text [String] HTTP status text
  # @param payload [Hash, String, nil] Response payload
  # @return [Error] Appropriate error instance
  def self.error_from_http_status(status, status_text, payload = nil)
    message = status_text
    if payload.is_a?(Hash) && payload["error"]
      message = payload["error"].to_s
    end

    case status
    when 401
      AuthenticationError.new(message)
    when 402
      InsufficientCreditsError.new(message)
    when 400, 422
      ValidationError.new(message, details: payload)
    when 429
      retry_after = payload.is_a?(Hash) ? payload["retryAfter"] : nil
      RateLimitError.new(message, retry_after: retry_after)
    else
      Error.new(message, code: "API_ERROR", status_code: status, details: payload)
    end
  end
end
