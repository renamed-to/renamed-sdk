import Foundation

/// Protocol for custom logger implementations
///
/// Implement this protocol to provide custom logging behavior.
/// When no custom logger is provided and debug mode is enabled,
/// the SDK uses `print()` for simplicity.
///
/// ```swift
/// class MyLogger: RenamedLogger {
///     func debug(_ message: String) {
///         os_log(.debug, "%{public}@", message)
///     }
/// }
///
/// let client = try RenamedClient(options: RenamedClientOptions(
///     apiKey: "rt_...",
///     logger: MyLogger()
/// ))
/// ```
public protocol RenamedLogger: Sendable {
    /// Log a debug message
    func debug(_ message: String)
}

/// Default logger that uses print() for output
internal final class DefaultLogger: RenamedLogger, @unchecked Sendable {
    static let shared = DefaultLogger()

    private init() {}

    func debug(_ message: String) {
        print(message)
    }
}

/// Internal helper for consistent log formatting and API key masking
internal enum LogHelper {
    /// Log prefix for all SDK messages
    static let prefix = "[Renamed]"

    /// Mask an API key for safe logging
    /// Shows only first 3 chars and last 4 chars: "rt_...xxxx"
    static func maskApiKey(_ apiKey: String) -> String {
        guard apiKey.count > 7 else {
            return "***"
        }
        let prefix = String(apiKey.prefix(3))
        let suffix = String(apiKey.suffix(4))
        return "\(prefix)...\(suffix)"
    }

    /// Format file size in human-readable format
    static func formatFileSize(_ bytes: Int) -> String {
        let kb = Double(bytes) / 1024
        if kb < 1024 {
            return String(format: "%.1f KB", kb)
        }
        let mb = kb / 1024
        if mb < 1024 {
            return String(format: "%.1f MB", mb)
        }
        let gb = mb / 1024
        return String(format: "%.1f GB", gb)
    }

    /// Format duration in milliseconds
    static func formatDuration(_ start: CFAbsoluteTime) -> String {
        let elapsed = CFAbsoluteTimeGetCurrent() - start
        let ms = Int(elapsed * 1000)
        return "\(ms)ms"
    }
}
