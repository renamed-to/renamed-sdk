# frozen_string_literal: true

require_relative "models"
require_relative "errors"

module Renamed
  # Async job handle for long-running operations like PDF split.
  class AsyncJob
    DEFAULT_POLL_INTERVAL = 2.0
    MAX_POLL_ATTEMPTS = 150 # 5 minutes at 2s intervals

    attr_reader :status_url

    # @param client [Client] The Renamed client instance
    # @param status_url [String] URL to poll for job status
    # @param poll_interval [Float] Interval between status checks in seconds
    # @param max_attempts [Integer] Maximum number of polling attempts
    def initialize(client, status_url, poll_interval: DEFAULT_POLL_INTERVAL, max_attempts: MAX_POLL_ATTEMPTS)
      @client = client
      @status_url = status_url
      @poll_interval = poll_interval
      @max_attempts = max_attempts
    end

    # Get current job status.
    #
    # @return [JobStatusResponse] Current job status
    def status
      response = @client.send(:request, :get, @status_url)
      JobStatusResponse.from_response(response)
    end

    # Wait for job completion, polling at regular intervals.
    #
    # @yield [JobStatusResponse] Called with status updates during polling
    # @return [PdfSplitResult] The completed job result
    # @raise [JobError] If the job fails or times out
    #
    # @example Wait for job completion
    #   job = client.pdf_split("multi-page.pdf", mode: "auto")
    #   result = job.wait { |status| puts "Progress: #{status.progress}%" }
    #   result.documents.each do |doc|
    #     puts "#{doc.filename}: #{doc.download_url}"
    #   end
    def wait
      attempts = 0

      while attempts < @max_attempts
        current_status = status

        log_job_status(current_status)
        yield current_status if block_given?

        if current_status.status == "completed" && current_status.result
          return current_status.result
        end

        if current_status.status == "failed"
          raise JobError.new(current_status.error || "Job failed", job_id: current_status.job_id)
        end

        attempts += 1
        sleep(@poll_interval)
      end

      raise JobError.new("Job polling timeout exceeded")
    end

    private

    def log_job_status(job_status)
      @client.send(:log_job_status, job_status.job_id, job_status.status, job_status.progress)
    end
  end
end
