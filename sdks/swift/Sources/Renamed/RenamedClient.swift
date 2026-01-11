import Foundation

/// Configuration options for the Renamed client
public struct RenamedClientOptions: Sendable {
    /// API key for authentication (starts with rt_)
    public let apiKey: String

    /// Base URL for the API
    public let baseUrl: String

    /// Request timeout in seconds
    public let timeout: TimeInterval

    /// Maximum number of retries for failed requests
    public let maxRetries: Int

    /// Enable debug logging (uses print() by default)
    public let debug: Bool

    /// Custom logger implementation (overrides debug flag when set)
    public let logger: (any RenamedLogger)?

    /// Default base URL
    public static let defaultBaseUrl = "https://www.renamed.to/api/v1"

    /// Default timeout in seconds
    public static let defaultTimeout: TimeInterval = 30

    /// Default max retries
    public static let defaultMaxRetries = 2

    public init(
        apiKey: String,
        baseUrl: String = RenamedClientOptions.defaultBaseUrl,
        timeout: TimeInterval = RenamedClientOptions.defaultTimeout,
        maxRetries: Int = RenamedClientOptions.defaultMaxRetries,
        debug: Bool = false,
        logger: (any RenamedLogger)? = nil
    ) {
        self.apiKey = apiKey
        self.baseUrl = baseUrl
        self.timeout = timeout
        self.maxRetries = maxRetries
        self.debug = debug
        self.logger = logger
    }
}

/// Main client for the renamed.to API
public final class RenamedClient: @unchecked Sendable {
    private let apiKey: String
    private let baseUrl: String
    private let timeout: TimeInterval
    private let maxRetries: Int
    private let session: URLSession
    private let logger: (any RenamedLogger)?

    /// Create a new client with the given options
    public init(options: RenamedClientOptions) throws {
        guard !options.apiKey.isEmpty else {
            throw RenamedError.authentication(message: "API key is required")
        }

        self.apiKey = options.apiKey
        self.baseUrl = options.baseUrl
        self.timeout = options.timeout
        self.maxRetries = options.maxRetries

        // Set up logger: custom logger takes precedence, then debug flag
        if let customLogger = options.logger {
            self.logger = customLogger
        } else if options.debug {
            self.logger = DefaultLogger.shared
        } else {
            self.logger = nil
        }

        let config = URLSessionConfiguration.default
        config.timeoutIntervalForRequest = timeout
        config.timeoutIntervalForResource = timeout * 2
        self.session = URLSession(configuration: config)
    }

    /// Create a new client with just an API key using default options
    public convenience init(apiKey: String) throws {
        try self.init(options: RenamedClientOptions(apiKey: apiKey))
    }

    // MARK: - Logging Helpers

    /// Log a debug message if logging is enabled
    internal func log(_ message: String) {
        logger?.debug("\(LogHelper.prefix) \(message)")
    }

    // MARK: - Public API Methods

    /// Get current user profile and credits
    ///
    /// ```swift
    /// let user = try await client.getUser()
    /// print("Credits remaining: \(user.credits ?? 0)")
    /// ```
    public func getUser() async throws -> User {
        try await request(path: "/user", method: "GET")
    }

    /// Rename a file using AI
    ///
    /// ```swift
    /// let file = FileInput(url: URL(fileURLWithPath: "invoice.pdf"))
    /// let result = try await client.rename(file: file)
    /// print(result.suggestedFilename) // "2025-01-15_AcmeCorp_INV-12345.pdf"
    /// ```
    ///
    /// - Parameters:
    ///   - file: The file to analyze
    ///   - options: Optional rename options (template, etc.)
    /// - Returns: Rename result with suggested filename
    public func rename(
        file: FileInput,
        options: RenameOptions? = nil
    ) async throws -> RenameResult {
        var additionalFields: [String: String] = [:]

        if let template = options?.template {
            additionalFields["template"] = template
        }

        return try await uploadFile(
            path: "/rename",
            file: file,
            additionalFields: additionalFields.isEmpty ? nil : additionalFields
        )
    }

    /// Split a PDF into multiple documents
    ///
    /// Returns an AsyncJob that can be polled for completion.
    ///
    /// ```swift
    /// let file = try FileInput(url: URL(fileURLWithPath: "multi-page.pdf"))
    /// let job = try await client.pdfSplit(file: file, options: PdfSplitOptions(mode: .auto))
    /// let result = try await job.wait { status in
    ///     print("Progress: \(status.progress ?? 0)%")
    /// }
    /// for doc in result.documents {
    ///     print("\(doc.filename): \(doc.downloadUrl)")
    /// }
    /// ```
    ///
    /// - Parameters:
    ///   - file: The PDF file to split
    ///   - options: Split options (mode, pages per split)
    /// - Returns: An AsyncJob that resolves to PdfSplitResult
    public func pdfSplit(
        file: FileInput,
        options: PdfSplitOptions? = nil
    ) async throws -> AsyncJob<PdfSplitResult> {
        var additionalFields: [String: String] = [:]

        if let mode = options?.mode {
            additionalFields["mode"] = mode.rawValue
        }
        if let pagesPerSplit = options?.pagesPerSplit {
            additionalFields["pagesPerSplit"] = String(pagesPerSplit)
        }

        struct JobResponse: Decodable {
            let statusUrl: String
        }

        let response: JobResponse = try await uploadFile(
            path: "/pdf-split",
            file: file,
            additionalFields: additionalFields.isEmpty ? nil : additionalFields
        )

        return AsyncJob<PdfSplitResult>(client: self, statusUrl: response.statusUrl)
    }

    /// Extract structured data from a document
    ///
    /// ```swift
    /// let file = try FileInput(url: URL(fileURLWithPath: "invoice.pdf"))
    /// let result = try await client.extract(
    ///     file: file,
    ///     options: ExtractOptions(prompt: "Extract invoice number, date, and total amount")
    /// )
    /// print(result.data)
    /// ```
    ///
    /// - Parameters:
    ///   - file: The document to extract from
    ///   - options: Extract options (schema, prompt)
    /// - Returns: Extracted data with confidence score
    public func extract(
        file: FileInput,
        options: ExtractOptions? = nil
    ) async throws -> ExtractResult {
        var additionalFields: [String: String] = [:]

        if let schema = options?.schema {
            let data = try JSONSerialization.data(withJSONObject: schema)
            additionalFields["schema"] = String(data: data, encoding: .utf8)
        }
        if let prompt = options?.prompt {
            additionalFields["prompt"] = prompt
        }

        return try await uploadFile(
            path: "/extract",
            file: file,
            additionalFields: additionalFields.isEmpty ? nil : additionalFields
        )
    }

    /// Download a file from a URL (e.g., split document)
    ///
    /// ```swift
    /// let result = try await job.wait()
    /// for doc in result.documents {
    ///     let data = try await client.downloadFile(url: doc.downloadUrl)
    ///     try data.write(to: URL(fileURLWithPath: doc.filename))
    /// }
    /// ```
    ///
    /// - Parameter url: The download URL
    /// - Returns: File data
    public func downloadFile(url: String) async throws -> Data {
        var request = URLRequest(url: URL(string: url)!)
        request.setValue("Bearer \(apiKey)", forHTTPHeaderField: "Authorization")

        let (data, response) = try await session.data(for: request)

        guard let httpResponse = response as? HTTPURLResponse else {
            throw RenamedError.network(message: "Invalid response")
        }

        guard httpResponse.statusCode == 200 else {
            throw RenamedError.fromHTTPStatus(
                httpResponse.statusCode,
                message: HTTPURLResponse.localizedString(forStatusCode: httpResponse.statusCode)
            )
        }

        return data
    }

    // MARK: - Internal Request Methods

    /// Make an authenticated JSON request to the API
    func request<T: Decodable>(
        path: String,
        method: String,
        body: Data? = nil
    ) async throws -> T {
        let url = buildURL(path: path)
        var request = URLRequest(url: url)
        request.httpMethod = method
        request.setValue("Bearer \(apiKey)", forHTTPHeaderField: "Authorization")

        if let body = body {
            request.setValue("application/json", forHTTPHeaderField: "Content-Type")
            request.httpBody = body
        }

        // Extract path for logging (strip base URL if present)
        let logPath = extractPath(from: path)

        return try await executeWithRetry(request: request, method: method, path: logPath)
    }

    /// Upload a file with multipart form data
    private func uploadFile<T: Decodable>(
        path: String,
        file: FileInput,
        fieldName: String = "file",
        additionalFields: [String: String]? = nil
    ) async throws -> T {
        // Log file upload details
        log("Upload: \(file.filename) (\(LogHelper.formatFileSize(file.data.count)))")

        let url = buildURL(path: path)
        let boundary = UUID().uuidString

        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("Bearer \(apiKey)", forHTTPHeaderField: "Authorization")
        request.setValue(
            "multipart/form-data; boundary=\(boundary)",
            forHTTPHeaderField: "Content-Type"
        )

        var body = Data()

        // Add file field
        body.append("--\(boundary)\r\n".data(using: .utf8)!)
        body.append(
            "Content-Disposition: form-data; name=\"\(fieldName)\"; filename=\"\(file.filename)\"\r\n"
                .data(using: .utf8)!
        )
        body.append("Content-Type: \(file.mimeType)\r\n\r\n".data(using: .utf8)!)
        body.append(file.data)
        body.append("\r\n".data(using: .utf8)!)

        // Add additional fields
        if let fields = additionalFields {
            for (key, value) in fields {
                body.append("--\(boundary)\r\n".data(using: .utf8)!)
                body.append(
                    "Content-Disposition: form-data; name=\"\(key)\"\r\n\r\n".data(using: .utf8)!
                )
                body.append("\(value)\r\n".data(using: .utf8)!)
            }
        }

        body.append("--\(boundary)--\r\n".data(using: .utf8)!)
        request.httpBody = body

        // Extract path for logging
        let logPath = extractPath(from: path)

        return try await executeWithRetry(request: request, method: "POST", path: logPath)
    }

    // MARK: - Private Helpers

    private func buildURL(path: String) -> URL {
        if path.hasPrefix("http://") || path.hasPrefix("https://") {
            return URL(string: path)!
        }

        let normalizedPath = path.hasPrefix("/") ? path : "/\(path)"
        return URL(string: "\(baseUrl)\(normalizedPath)")!
    }

    private func executeWithRetry<T: Decodable>(
        request: URLRequest,
        method: String = "GET",
        path: String = ""
    ) async throws -> T {
        var lastError: Error?
        var attempts = 0

        while attempts <= maxRetries {
            let startTime = CFAbsoluteTimeGetCurrent()

            do {
                let (data, response) = try await session.data(for: request)

                guard let httpResponse = response as? HTTPURLResponse else {
                    throw RenamedError.network(message: "Invalid response")
                }

                // Log the request and response
                log("\(method) \(path) -> \(httpResponse.statusCode) (\(LogHelper.formatDuration(startTime)))")

                if httpResponse.statusCode >= 200 && httpResponse.statusCode < 300 {
                    let decoder = JSONDecoder()
                    return try decoder.decode(T.self, from: data)
                }

                // Parse error response
                let errorMessage = parseErrorMessage(from: data) ??
                    HTTPURLResponse.localizedString(forStatusCode: httpResponse.statusCode)
                let payload = try? JSONSerialization.jsonObject(with: data) as? [String: Any]

                let error = RenamedError.fromHTTPStatus(
                    httpResponse.statusCode,
                    message: errorMessage,
                    payload: payload
                )

                // Don't retry 4xx errors
                if httpResponse.statusCode >= 400 && httpResponse.statusCode < 500 {
                    throw error
                }

                lastError = error
            } catch let error as URLError where error.code == .timedOut {
                log("\(method) \(path) -> timeout (\(LogHelper.formatDuration(startTime)))")
                throw RenamedError.timeout(message: "Request timed out")
            } catch let error as URLError {
                log("\(method) \(path) -> network error (\(LogHelper.formatDuration(startTime)))")
                throw RenamedError.network(message: error.localizedDescription)
            } catch let error as RenamedError {
                throw error
            } catch {
                lastError = error
            }

            attempts += 1
            if attempts <= maxRetries {
                // Exponential backoff: 200ms, 400ms, 800ms, etc.
                let delayMs = Int(pow(2.0, Double(attempts)) * 100)
                log("Retry attempt \(attempts)/\(maxRetries), waiting \(delayMs)ms")
                try await Task.sleep(nanoseconds: UInt64(delayMs) * 1_000_000)
            }
        }

        throw lastError ?? RenamedError.network(message: "Request failed")
    }

    private func parseErrorMessage(from data: Data) -> String? {
        guard let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else {
            return nil
        }
        return json["error"] as? String ?? json["message"] as? String
    }

    /// Extract path from a URL string for logging purposes
    /// Returns the path portion without the base URL
    private func extractPath(from path: String) -> String {
        if path.hasPrefix("http://") || path.hasPrefix("https://") {
            // Full URL - extract just the path component
            if let url = URL(string: path) {
                return url.path
            }
            return path
        }
        // Already a path
        return path.hasPrefix("/") ? path : "/\(path)"
    }
}
