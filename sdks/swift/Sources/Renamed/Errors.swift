import Foundation

/// Error types for the Renamed SDK
public enum RenamedError: Error, LocalizedError, Equatable {
    /// Invalid or missing API key
    case authentication(message: String)

    /// Insufficient credits to perform operation
    case insufficientCredits(message: String)

    /// Rate limit exceeded - includes optional retry delay in seconds
    case rateLimit(message: String, retryAfter: Int?)

    /// Invalid request parameters
    case validation(message: String, details: String?)

    /// Network connection failed
    case network(message: String)

    /// Request timed out
    case timeout(message: String)

    /// Async job failed
    case job(message: String, jobId: String?)

    /// Generic API error with status code
    case api(message: String, statusCode: Int)

    public var errorDescription: String? {
        switch self {
        case .authentication(let message):
            return "Authentication error: \(message)"
        case .insufficientCredits(let message):
            return "Insufficient credits: \(message)"
        case .rateLimit(let message, _):
            return "Rate limit exceeded: \(message)"
        case .validation(let message, _):
            return "Validation error: \(message)"
        case .network(let message):
            return "Network error: \(message)"
        case .timeout(let message):
            return "Timeout: \(message)"
        case .job(let message, _):
            return "Job error: \(message)"
        case .api(let message, let statusCode):
            return "API error (\(statusCode)): \(message)"
        }
    }

    /// Create appropriate error from HTTP status code and response
    static func fromHTTPStatus(
        _ statusCode: Int,
        message: String?,
        payload: [String: Any]? = nil
    ) -> RenamedError {
        let errorMessage = message ?? "Unknown error"

        switch statusCode {
        case 401:
            return .authentication(message: errorMessage)
        case 402:
            return .insufficientCredits(message: errorMessage)
        case 400, 422:
            let details = payload.flatMap { try? JSONSerialization.data(withJSONObject: $0) }
                .flatMap { String(data: $0, encoding: .utf8) }
            return .validation(message: errorMessage, details: details)
        case 429:
            let retryAfter = payload?["retryAfter"] as? Int
            return .rateLimit(message: errorMessage, retryAfter: retryAfter)
        default:
            return .api(message: errorMessage, statusCode: statusCode)
        }
    }
}
