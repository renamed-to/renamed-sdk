# frozen_string_literal: true

module Renamed
  # Result of a rename operation.
  class RenameResult
    attr_reader :original_filename, :suggested_filename, :folder_path, :confidence

    # @param original_filename [String] Original filename that was uploaded
    # @param suggested_filename [String] AI-suggested new filename
    # @param folder_path [String, nil] Suggested folder path for organization
    # @param confidence [Float, nil] Confidence score (0-1) of the suggestion
    def initialize(original_filename:, suggested_filename:, folder_path: nil, confidence: nil)
      @original_filename = original_filename
      @suggested_filename = suggested_filename
      @folder_path = folder_path
      @confidence = confidence
    end

    # Create instance from API response hash.
    #
    # @param data [Hash] API response data
    # @return [RenameResult]
    def self.from_response(data)
      new(
        original_filename: data["originalFilename"],
        suggested_filename: data["suggestedFilename"],
        folder_path: data["folderPath"],
        confidence: data["confidence"]
      )
    end

    def to_h
      {
        original_filename: @original_filename,
        suggested_filename: @suggested_filename,
        folder_path: @folder_path,
        confidence: @confidence
      }.compact
    end
  end

  # A single document from PDF split.
  class SplitDocument
    attr_reader :index, :filename, :pages, :download_url, :size

    # @param index [Integer] Document index (0-based)
    # @param filename [String] Suggested filename for this document
    # @param pages [String] Page range included in this document
    # @param download_url [String] URL to download this document
    # @param size [Integer] Size in bytes
    def initialize(index:, filename:, pages:, download_url:, size:)
      @index = index
      @filename = filename
      @pages = pages
      @download_url = download_url
      @size = size
    end

    # Create instance from API response hash.
    #
    # @param data [Hash] API response data
    # @return [SplitDocument]
    def self.from_response(data)
      new(
        index: data["index"],
        filename: data["filename"],
        pages: data["pages"],
        download_url: data["downloadUrl"],
        size: data["size"]
      )
    end

    def to_h
      {
        index: @index,
        filename: @filename,
        pages: @pages,
        download_url: @download_url,
        size: @size
      }
    end
  end

  # Result of PDF split operation.
  class PdfSplitResult
    attr_reader :original_filename, :documents, :total_pages

    # @param original_filename [String] Original filename
    # @param documents [Array<SplitDocument>] Split documents
    # @param total_pages [Integer] Total number of pages in original document
    def initialize(original_filename:, documents:, total_pages:)
      @original_filename = original_filename
      @documents = documents
      @total_pages = total_pages
    end

    # Create instance from API response hash.
    #
    # @param data [Hash] API response data
    # @return [PdfSplitResult]
    def self.from_response(data)
      documents = (data["documents"] || []).map { |doc| SplitDocument.from_response(doc) }
      new(
        original_filename: data["originalFilename"],
        documents: documents,
        total_pages: data["totalPages"]
      )
    end

    def to_h
      {
        original_filename: @original_filename,
        documents: @documents.map(&:to_h),
        total_pages: @total_pages
      }
    end
  end

  # Response from job status endpoint.
  class JobStatusResponse
    attr_reader :job_id, :status, :progress, :error, :result

    # @param job_id [String] Unique job identifier
    # @param status [String] Current job status (pending, processing, completed, failed)
    # @param progress [Integer, nil] Progress percentage (0-100)
    # @param error [String, nil] Error message if job failed
    # @param result [PdfSplitResult, nil] Result data when job is completed
    def initialize(job_id:, status:, progress: nil, error: nil, result: nil)
      @job_id = job_id
      @status = status
      @progress = progress
      @error = error
      @result = result
    end

    # Create instance from API response hash.
    #
    # @param data [Hash] API response data
    # @return [JobStatusResponse]
    def self.from_response(data)
      result = data["result"] ? PdfSplitResult.from_response(data["result"]) : nil
      new(
        job_id: data["jobId"],
        status: data["status"],
        progress: data["progress"],
        error: data["error"],
        result: result
      )
    end

    def to_h
      {
        job_id: @job_id,
        status: @status,
        progress: @progress,
        error: @error,
        result: @result&.to_h
      }.compact
    end
  end

  # Result of extract operation.
  class ExtractResult
    attr_reader :data, :confidence

    # @param data [Hash] Extracted data matching the schema
    # @param confidence [Float] Confidence score (0-1)
    def initialize(data:, confidence:)
      @data = data
      @confidence = confidence
    end

    # Create instance from API response hash.
    #
    # @param response_data [Hash] API response data
    # @return [ExtractResult]
    def self.from_response(response_data)
      new(
        data: response_data["data"],
        confidence: response_data["confidence"]
      )
    end

    def to_h
      {
        data: @data,
        confidence: @confidence
      }
    end
  end

  # Team information.
  class Team
    attr_reader :id, :name

    # @param id [String] Team ID
    # @param name [String] Team name
    def initialize(id:, name:)
      @id = id
      @name = name
    end

    # Create instance from API response hash.
    #
    # @param data [Hash] API response data
    # @return [Team]
    def self.from_response(data)
      new(
        id: data["id"],
        name: data["name"]
      )
    end

    def to_h
      {
        id: @id,
        name: @name
      }
    end
  end

  # User profile information.
  class User
    attr_reader :id, :email, :name, :credits, :team

    # @param id [String] User ID
    # @param email [String] Email address
    # @param name [String, nil] Display name
    # @param credits [Integer, nil] Available credits
    # @param team [Team, nil] Team information (if applicable)
    def initialize(id:, email:, name: nil, credits: nil, team: nil)
      @id = id
      @email = email
      @name = name
      @credits = credits
      @team = team
    end

    # Create instance from API response hash.
    #
    # @param data [Hash] API response data
    # @return [User]
    def self.from_response(data)
      team = data["team"] ? Team.from_response(data["team"]) : nil
      new(
        id: data["id"],
        email: data["email"],
        name: data["name"],
        credits: data["credits"],
        team: team
      )
    end

    def to_h
      {
        id: @id,
        email: @email,
        name: @name,
        credits: @credits,
        team: @team&.to_h
      }.compact
    end
  end

  # MIME types for supported file formats.
  MIME_TYPES = {
    ".pdf" => "application/pdf",
    ".jpg" => "image/jpeg",
    ".jpeg" => "image/jpeg",
    ".png" => "image/png",
    ".tiff" => "image/tiff",
    ".tif" => "image/tiff"
  }.freeze
end
