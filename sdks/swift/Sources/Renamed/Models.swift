import Foundation

// MARK: - User Models

/// Team information for a user
public struct Team: Codable, Equatable, Sendable {
    /// Unique team identifier
    public let id: String

    /// Team display name
    public let name: String

    public init(id: String, name: String) {
        self.id = id
        self.name = name
    }
}

/// User profile information
public struct User: Codable, Equatable, Sendable {
    /// Unique user identifier
    public let id: String

    /// User email address
    public let email: String

    /// User display name
    public let name: String?

    /// Available credits for API operations
    public let credits: Int?

    /// Team information if user belongs to a team
    public let team: Team?

    public init(
        id: String,
        email: String,
        name: String? = nil,
        credits: Int? = nil,
        team: Team? = nil
    ) {
        self.id = id
        self.email = email
        self.name = name
        self.credits = credits
        self.team = team
    }
}

// MARK: - Rename Models

/// Options for rename operation
public struct RenameOptions: Sendable {
    /// Custom template for filename generation
    public let template: String?

    public init(template: String? = nil) {
        self.template = template
    }
}

/// Result of a rename operation
public struct RenameResult: Codable, Equatable, Sendable {
    /// Original filename that was uploaded
    public let originalFilename: String

    /// AI-suggested new filename
    public let suggestedFilename: String

    /// Suggested folder path for organization
    public let folderPath: String?

    /// Confidence score (0-1) of the suggestion
    public let confidence: Double?

    public init(
        originalFilename: String,
        suggestedFilename: String,
        folderPath: String? = nil,
        confidence: Double? = nil
    ) {
        self.originalFilename = originalFilename
        self.suggestedFilename = suggestedFilename
        self.folderPath = folderPath
        self.confidence = confidence
    }
}

// MARK: - PDF Split Models

/// Split mode for PDF operations
public enum PdfSplitMode: String, Sendable {
    /// AI-detected document boundaries
    case auto
    /// Split every N pages
    case pages
    /// Split at blank pages
    case blank
}

/// Options for PDF split operation
public struct PdfSplitOptions: Sendable {
    /// Split mode
    public let mode: PdfSplitMode?

    /// Number of pages per split (for 'pages' mode)
    public let pagesPerSplit: Int?

    public init(mode: PdfSplitMode? = nil, pagesPerSplit: Int? = nil) {
        self.mode = mode
        self.pagesPerSplit = pagesPerSplit
    }
}

/// A single document from PDF split
public struct SplitDocument: Codable, Equatable, Sendable {
    /// Document index (0-based)
    public let index: Int

    /// Suggested filename for this document
    public let filename: String

    /// Page range included in this document (e.g., "1-5")
    public let pages: String

    /// URL to download this document
    public let downloadUrl: String

    /// Size in bytes
    public let size: Int

    public init(
        index: Int,
        filename: String,
        pages: String,
        downloadUrl: String,
        size: Int
    ) {
        self.index = index
        self.filename = filename
        self.pages = pages
        self.downloadUrl = downloadUrl
        self.size = size
    }
}

/// Result of PDF split operation
public struct PdfSplitResult: Codable, Equatable, Sendable {
    /// Original filename
    public let originalFilename: String

    /// Split documents
    public let documents: [SplitDocument]

    /// Total number of pages in original document
    public let totalPages: Int

    public init(
        originalFilename: String,
        documents: [SplitDocument],
        totalPages: Int
    ) {
        self.originalFilename = originalFilename
        self.documents = documents
        self.totalPages = totalPages
    }
}

// MARK: - Extract Models

/// Options for extract operation
public struct ExtractOptions: Sendable {
    /// Schema defining what to extract (as JSON-encodable dictionary)
    public let schema: [String: Any]?

    /// Prompt describing what to extract
    public let prompt: String?

    public init(schema: [String: Any]? = nil, prompt: String? = nil) {
        self.schema = schema
        self.prompt = prompt
    }
}

/// Result of extract operation
public struct ExtractResult: Codable, Equatable, Sendable {
    /// Extracted data matching the schema
    public let data: [String: AnyCodable]

    /// Confidence score (0-1)
    public let confidence: Double

    public init(data: [String: AnyCodable], confidence: Double) {
        self.data = data
        self.confidence = confidence
    }
}

// MARK: - Job Status Models

/// Status of an async job
public enum JobStatus: String, Codable, Sendable {
    case pending
    case processing
    case completed
    case failed
}

/// Response from job status endpoint
public struct JobStatusResponse: Codable, Sendable {
    /// Unique job identifier
    public let jobId: String

    /// Current job status
    public let status: JobStatus

    /// Progress percentage (0-100)
    public let progress: Int?

    /// Error message if job failed
    public let error: String?

    /// Result data when job is completed
    public let result: PdfSplitResult?

    public init(
        jobId: String,
        status: JobStatus,
        progress: Int? = nil,
        error: String? = nil,
        result: PdfSplitResult? = nil
    ) {
        self.jobId = jobId
        self.status = status
        self.progress = progress
        self.error = error
        self.result = result
    }
}

// MARK: - File Input

/// Represents file data for upload
public struct FileInput: Sendable {
    /// File data
    public let data: Data

    /// Original filename
    public let filename: String

    /// MIME type of the file
    public let mimeType: String

    public init(data: Data, filename: String, mimeType: String? = nil) {
        self.data = data
        self.filename = filename
        self.mimeType = mimeType ?? FileInput.detectMimeType(for: filename)
    }

    /// Create from URL (reads file synchronously)
    public init(url: URL) throws {
        self.data = try Data(contentsOf: url)
        self.filename = url.lastPathComponent
        self.mimeType = FileInput.detectMimeType(for: url.lastPathComponent)
    }

    private static func detectMimeType(for filename: String) -> String {
        let ext = (filename as NSString).pathExtension.lowercased()
        switch ext {
        case "pdf":
            return "application/pdf"
        case "jpg", "jpeg":
            return "image/jpeg"
        case "png":
            return "image/png"
        case "tiff", "tif":
            return "image/tiff"
        default:
            return "application/octet-stream"
        }
    }
}

// MARK: - AnyCodable Helper

/// A type-erased Codable value for dynamic JSON data
public struct AnyCodable: Codable, Equatable, Sendable {
    public let value: Any

    public init(_ value: Any) {
        self.value = value
    }

    public init(from decoder: Decoder) throws {
        let container = try decoder.singleValueContainer()

        if container.decodeNil() {
            self.value = NSNull()
        } else if let bool = try? container.decode(Bool.self) {
            self.value = bool
        } else if let int = try? container.decode(Int.self) {
            self.value = int
        } else if let double = try? container.decode(Double.self) {
            self.value = double
        } else if let string = try? container.decode(String.self) {
            self.value = string
        } else if let array = try? container.decode([AnyCodable].self) {
            self.value = array.map { $0.value }
        } else if let dictionary = try? container.decode([String: AnyCodable].self) {
            self.value = dictionary.mapValues { $0.value }
        } else {
            throw DecodingError.dataCorruptedError(
                in: container,
                debugDescription: "Unable to decode value"
            )
        }
    }

    public func encode(to encoder: Encoder) throws {
        var container = encoder.singleValueContainer()

        switch value {
        case is NSNull:
            try container.encodeNil()
        case let bool as Bool:
            try container.encode(bool)
        case let int as Int:
            try container.encode(int)
        case let double as Double:
            try container.encode(double)
        case let string as String:
            try container.encode(string)
        case let array as [Any]:
            try container.encode(array.map { AnyCodable($0) })
        case let dictionary as [String: Any]:
            try container.encode(dictionary.mapValues { AnyCodable($0) })
        default:
            throw EncodingError.invalidValue(
                value,
                EncodingError.Context(
                    codingPath: container.codingPath,
                    debugDescription: "Unable to encode value"
                )
            )
        }
    }

    public static func == (lhs: AnyCodable, rhs: AnyCodable) -> Bool {
        switch (lhs.value, rhs.value) {
        case is (NSNull, NSNull):
            return true
        case let (l as Bool, r as Bool):
            return l == r
        case let (l as Int, r as Int):
            return l == r
        case let (l as Double, r as Double):
            return l == r
        case let (l as String, r as String):
            return l == r
        default:
            return false
        }
    }
}
