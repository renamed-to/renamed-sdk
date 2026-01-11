# frozen_string_literal: true

require "faraday"
require "faraday/multipart"
require "json"
require "logger"
require "uri"

require_relative "errors"
require_relative "models"
require_relative "async_job"

module Renamed
  # renamed.to API client.
  #
  # @example Basic usage
  #   client = Renamed::Client.new(api_key: "rt_your_api_key")
  #   result = client.rename("invoice.pdf")
  #   puts result.suggested_filename
  #
  # @example With custom configuration
  #   client = Renamed::Client.new(
  #     api_key: "rt_your_api_key",
  #     timeout: 60,
  #     max_retries: 3
  #   )
  class Client
    DEFAULT_BASE_URL = "https://www.renamed.to/api/v1"
    DEFAULT_TIMEOUT = 30
    DEFAULT_MAX_RETRIES = 2

    # @param api_key [String] API key for authentication (starts with rt_)
    # @param base_url [String] Base URL for the API
    # @param timeout [Integer] Request timeout in seconds
    # @param max_retries [Integer] Maximum number of retries for failed requests
    # @param debug [Boolean] Enable debug logging to stderr
    # @param logger [Logger, nil] Custom logger instance (overrides debug flag)
    def initialize(api_key:, base_url: DEFAULT_BASE_URL, timeout: DEFAULT_TIMEOUT, max_retries: DEFAULT_MAX_RETRIES, debug: false, logger: nil)
      raise AuthenticationError, "API key is required" if api_key.nil? || api_key.empty?

      @api_key = api_key
      @base_url = base_url.chomp("/")
      @timeout = timeout
      @max_retries = max_retries
      @logger = setup_logger(debug, logger)

      @connection = build_connection
    end

    # Get current user profile and credits.
    #
    # @return [User] User profile with credits balance
    #
    # @example
    #   user = client.get_user
    #   puts "Credits remaining: #{user.credits}"
    def get_user
      response = request(:get, "/user")
      User.from_response(response)
    end

    # Rename a file using AI.
    #
    # @param file [String, File, IO] File to rename (path, File object, or IO)
    # @param options [Hash] Additional options
    # @option options [String] :template Custom template for filename generation
    #
    # @return [RenameResult] Result with suggested filename and folder path
    #
    # @example Rename a file by path
    #   result = client.rename("invoice.pdf")
    #   puts result.suggested_filename  # "2025-01-15_AcmeCorp_INV-12345.pdf"
    #
    # @example Rename with custom template
    #   result = client.rename("invoice.pdf", template: "{date}_{vendor}_{type}")
    #   puts result.suggested_filename
    def rename(file, options: {})
      additional_fields = {}
      additional_fields["template"] = options[:template] if options[:template]

      response = upload_file("/rename", file, additional_fields: additional_fields)
      RenameResult.from_response(response)
    end

    # Split a PDF into multiple documents.
    #
    # Returns an AsyncJob that can be polled for completion.
    #
    # @param file [String, File, IO] PDF file to split
    # @param options [Hash] Additional options
    # @option options [String] :mode Split mode ('auto', 'pages', or 'blank')
    # @option options [Integer] :pages_per_split Number of pages per split (for 'pages' mode)
    #
    # @return [AsyncJob] Job that can be waited on for the result
    #
    # @example Split a PDF with auto-detection
    #   job = client.pdf_split("multi-page.pdf", mode: "auto")
    #   result = job.wait { |s| puts "Progress: #{s.progress}%" }
    #   result.documents.each do |doc|
    #     puts "#{doc.filename}: #{doc.download_url}"
    #   end
    #
    # @example Split into fixed page chunks
    #   job = client.pdf_split("document.pdf", mode: "pages", pages_per_split: 5)
    #   result = job.wait
    def pdf_split(file, options: {})
      additional_fields = {}
      additional_fields["mode"] = options[:mode] if options[:mode]
      additional_fields["pagesPerSplit"] = options[:pages_per_split].to_s if options[:pages_per_split]

      response = upload_file("/pdf-split", file, additional_fields: additional_fields)
      AsyncJob.new(self, response["statusUrl"])
    end

    # Extract structured data from a document.
    #
    # @param file [String, File, IO] Document to extract data from
    # @param options [Hash] Additional options
    # @option options [String] :prompt Natural language description of what to extract
    # @option options [Hash] :schema JSON schema defining the structure of data to extract
    #
    # @return [ExtractResult] Result with extracted data
    #
    # @example Extract with a prompt
    #   result = client.extract("invoice.pdf", prompt: "Extract invoice details")
    #   puts result.data
    #
    # @example Extract with a schema
    #   schema = {
    #     type: "object",
    #     properties: {
    #       vendor: { type: "string" },
    #       total: { type: "number" }
    #     }
    #   }
    #   result = client.extract("invoice.pdf", schema: schema)
    def extract(file, options: {})
      additional_fields = {}
      additional_fields["prompt"] = options[:prompt] if options[:prompt]
      additional_fields["schema"] = JSON.generate(options[:schema]) if options[:schema]

      response = upload_file("/extract", file, additional_fields: additional_fields)
      ExtractResult.from_response(response)
    end

    # Download a file from a URL (e.g., split document).
    #
    # @param url [String] URL to download from
    # @return [String] File content as binary string
    #
    # @example Download split documents
    #   result = job.wait
    #   result.documents.each do |doc|
    #     content = client.download_file(doc.download_url)
    #     File.binwrite(doc.filename, content)
    #   end
    def download_file(url)
      response = @connection.get(url)
      handle_download_response(response)
    end

    private

    def build_connection
      Faraday.new(url: @base_url) do |conn|
        conn.request :multipart
        conn.request :url_encoded
        conn.headers["Authorization"] = "Bearer #{@api_key}"
        conn.options.timeout = @timeout
        conn.options.open_timeout = @timeout
        conn.adapter Faraday.default_adapter
      end
    end

    def build_url(path)
      return path if path.start_with?("http://", "https://")

      path = "/#{path}" unless path.start_with?("/")
      "#{@base_url}#{path}"
    end

    def request(method, path, **options)
      url = build_url(path)
      last_error = nil
      attempts = 0

      while attempts <= @max_retries
        begin
          start_time = monotonic_time
          response = @connection.send(method, url, options[:body], options[:headers])
          log_request(method, path, response.status, start_time)
          return handle_response(response)
        rescue Faraday::ConnectionFailed => e
          last_error = NetworkError.new(e.message)
        rescue Faraday::TimeoutError => e
          last_error = TimeoutError.new(e.message)
        rescue Renamed::Error => e
          # Don't retry client errors (4xx)
          raise if e.status_code && e.status_code >= 400 && e.status_code < 500

          last_error = e
        end

        attempts += 1
        if attempts <= @max_retries
          backoff_ms = (2**attempts * 100).to_i
          log_retry(attempts, @max_retries, backoff_ms)
          sleep(backoff_ms / 1000.0)
        end
      end

      raise last_error || NetworkError.new
    end

    def handle_response(response)
      if response.status >= 400
        payload = parse_response_body(response)
        raise Renamed.error_from_http_status(response.status, response.reason_phrase || "Error", payload)
      end

      parse_response_body(response)
    end

    def handle_download_response(response)
      if response.status >= 400
        raise Renamed.error_from_http_status(response.status, response.reason_phrase || "Error")
      end

      response.body
    end

    def parse_response_body(response)
      return {} if response.body.nil? || response.body.empty?

      JSON.parse(response.body)
    rescue JSON::ParserError
      response.body
    end

    def upload_file(path, file, additional_fields: {})
      filename, content, mime_type = prepare_file(file)

      log_upload(filename, content.bytesize)

      payload = {}
      payload[:file] = Faraday::Multipart::FilePart.new(
        StringIO.new(content),
        mime_type,
        filename
      )

      additional_fields.each do |key, value|
        payload[key] = value
      end

      url = build_url(path)
      last_error = nil
      attempts = 0

      while attempts <= @max_retries
        begin
          start_time = monotonic_time
          response = @connection.post(url, payload)
          log_request(:post, path, response.status, start_time)
          return handle_response(response)
        rescue Faraday::ConnectionFailed => e
          last_error = NetworkError.new(e.message)
        rescue Faraday::TimeoutError => e
          last_error = TimeoutError.new(e.message)
        rescue Renamed::Error => e
          raise if e.status_code && e.status_code >= 400 && e.status_code < 500

          last_error = e
        end

        attempts += 1
        if attempts <= @max_retries
          backoff_ms = (2**attempts * 100).to_i
          log_retry(attempts, @max_retries, backoff_ms)
          sleep(backoff_ms / 1000.0)
        end
      end

      raise last_error || NetworkError.new
    end

    def prepare_file(file)
      case file
      when String
        # File path
        path = File.expand_path(file)
        content = File.binread(path)
        filename = File.basename(path)
        [filename, content, get_mime_type(filename)]
      when File, IO
        content = file.read
        filename = file.respond_to?(:path) && file.path ? File.basename(file.path) : "file"
        [filename, content, get_mime_type(filename)]
      else
        raise ValidationError.new("Invalid file input: expected String path, File, or IO object")
      end
    end

    def get_mime_type(filename)
      ext = File.extname(filename).downcase
      MIME_TYPES[ext] || "application/octet-stream"
    end

    # Logging helpers

    def setup_logger(debug, logger)
      return logger if logger

      return nil unless debug

      new_logger = Logger.new($stderr)
      new_logger.level = Logger::DEBUG
      new_logger.formatter = proc { |_severity, _datetime, _progname, msg| "#{msg}\n" }
      new_logger
    end

    def log_debug(message)
      @logger&.debug(message)
    end

    def log_request(method, path, status, start_time)
      return unless @logger

      elapsed_ms = ((monotonic_time - start_time) * 1000).round
      # Normalize path to just the path portion (not full URL)
      display_path = path.start_with?("http") ? URI.parse(path).path : path
      log_debug("[Renamed] #{method.to_s.upcase} #{display_path} -> #{status} (#{elapsed_ms}ms)")
    end

    def log_retry(attempt, max_retries, backoff_ms)
      log_debug("[Renamed] Retry attempt #{attempt}/#{max_retries}, waiting #{backoff_ms}ms")
    end

    def log_upload(filename, size_bytes)
      log_debug("[Renamed] Upload: #{filename} (#{format_size(size_bytes)})")
    end

    def log_job_status(job_id, status, progress = nil)
      message = "[Renamed] Job #{job_id}: #{status}"
      message += " (#{progress}%)" if progress
      log_debug(message)
    end

    def format_size(bytes)
      if bytes >= 1_000_000
        format("%.1f MB", bytes / 1_000_000.0)
      elsif bytes >= 1_000
        format("%.1f KB", bytes / 1_000.0)
      else
        "#{bytes} B"
      end
    end

    def mask_api_key(key)
      return "***" if key.nil? || key.length < 7

      "#{key[0, 3]}...#{key[-4, 4]}"
    end

    def monotonic_time
      Process.clock_gettime(Process::CLOCK_MONOTONIC)
    end
  end
end
