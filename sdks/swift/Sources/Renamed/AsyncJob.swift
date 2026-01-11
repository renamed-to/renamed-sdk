import Foundation

/// Handle for long-running async operations like PDF split
public final class AsyncJob<T: Decodable & Sendable>: @unchecked Sendable {
    private let client: RenamedClient
    private let statusUrl: String
    private let pollInterval: TimeInterval
    private let maxAttempts: Int

    /// Default polling interval in seconds
    public static var defaultPollInterval: TimeInterval { 2.0 }

    /// Default maximum poll attempts (5 minutes at 2s intervals)
    public static var defaultMaxAttempts: Int { 150 }

    init(
        client: RenamedClient,
        statusUrl: String,
        pollInterval: TimeInterval = AsyncJob.defaultPollInterval,
        maxAttempts: Int = AsyncJob.defaultMaxAttempts
    ) {
        self.client = client
        self.statusUrl = statusUrl
        self.pollInterval = pollInterval
        self.maxAttempts = maxAttempts
    }

    /// Get current job status
    public func status() async throws -> JobStatusResponse {
        try await client.request(path: statusUrl, method: "GET")
    }

    /// Wait for job completion, polling at regular intervals
    /// - Parameter onProgress: Optional callback invoked with each status update
    /// - Returns: The completed result
    /// - Throws: `RenamedError.job` if the job fails or times out
    public func wait(onProgress: ((JobStatusResponse) -> Void)? = nil) async throws -> T {
        var attempts = 0

        while attempts < maxAttempts {
            let jobStatus = try await status()

            // Log job status with progress if available
            logJobStatus(jobStatus)

            onProgress?(jobStatus)

            switch jobStatus.status {
            case .completed:
                // For PdfSplitResult, the result is embedded in the status response
                if let result = jobStatus.result as? T {
                    return result
                }

                // Try to decode the result from the response
                // This handles the case where T is PdfSplitResult
                if T.self == PdfSplitResult.self, let result = jobStatus.result {
                    // Force cast is safe because we checked T.self == PdfSplitResult.self
                    return result as! T
                }

                throw RenamedError.job(
                    message: "Job completed but result is missing or invalid",
                    jobId: jobStatus.jobId
                )

            case .failed:
                throw RenamedError.job(
                    message: jobStatus.error ?? "Job failed",
                    jobId: jobStatus.jobId
                )

            case .pending, .processing:
                attempts += 1
                try await Task.sleep(nanoseconds: UInt64(pollInterval * 1_000_000_000))
            }
        }

        throw RenamedError.job(message: "Job polling timeout exceeded", jobId: nil)
    }

    // MARK: - Private Helpers

    private func logJobStatus(_ status: JobStatusResponse) {
        var message = "Job \(status.jobId): \(status.status.rawValue)"
        if let progress = status.progress {
            message += " (\(progress)%)"
        }
        client.log(message)
    }
}
