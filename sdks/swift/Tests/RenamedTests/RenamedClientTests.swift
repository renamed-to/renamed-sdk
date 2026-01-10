import XCTest
@testable import Renamed

final class RenamedClientTests: XCTestCase {

    // MARK: - Client Initialization Tests

    func testClientRequiresAPIKey() {
        XCTAssertThrowsError(try RenamedClient(apiKey: "")) { error in
            guard case RenamedError.authentication = error else {
                XCTFail("Expected authentication error, got \(error)")
                return
            }
        }
    }

    func testClientInitializesWithValidAPIKey() {
        XCTAssertNoThrow(try RenamedClient(apiKey: "rt_test_key"))
    }

    func testClientOptionsDefaults() {
        let options = RenamedClientOptions(apiKey: "rt_test")
        XCTAssertEqual(options.baseUrl, "https://www.renamed.to/api/v1")
        XCTAssertEqual(options.timeout, 30)
        XCTAssertEqual(options.maxRetries, 2)
    }

    // MARK: - FileInput Tests

    func testFileInputDetectsMimeType() {
        let pdfInput = FileInput(data: Data(), filename: "test.pdf")
        XCTAssertEqual(pdfInput.mimeType, "application/pdf")

        let jpegInput = FileInput(data: Data(), filename: "image.jpg")
        XCTAssertEqual(jpegInput.mimeType, "image/jpeg")

        let jpegInput2 = FileInput(data: Data(), filename: "image.jpeg")
        XCTAssertEqual(jpegInput2.mimeType, "image/jpeg")

        let pngInput = FileInput(data: Data(), filename: "image.png")
        XCTAssertEqual(pngInput.mimeType, "image/png")

        let tiffInput = FileInput(data: Data(), filename: "image.tiff")
        XCTAssertEqual(tiffInput.mimeType, "image/tiff")

        let unknownInput = FileInput(data: Data(), filename: "file.xyz")
        XCTAssertEqual(unknownInput.mimeType, "application/octet-stream")
    }

    func testFileInputAcceptsExplicitMimeType() {
        let input = FileInput(data: Data(), filename: "test.bin", mimeType: "application/custom")
        XCTAssertEqual(input.mimeType, "application/custom")
    }

    // MARK: - Error Tests

    func testErrorFromHTTPStatus401() {
        let error = RenamedError.fromHTTPStatus(401, message: "Invalid token")
        guard case .authentication(let message) = error else {
            XCTFail("Expected authentication error")
            return
        }
        XCTAssertEqual(message, "Invalid token")
    }

    func testErrorFromHTTPStatus402() {
        let error = RenamedError.fromHTTPStatus(402, message: "No credits")
        guard case .insufficientCredits(let message) = error else {
            XCTFail("Expected insufficientCredits error")
            return
        }
        XCTAssertEqual(message, "No credits")
    }

    func testErrorFromHTTPStatus429() {
        let error = RenamedError.fromHTTPStatus(429, message: "Too many requests", payload: ["retryAfter": 60])
        guard case .rateLimit(let message, let retryAfter) = error else {
            XCTFail("Expected rateLimit error")
            return
        }
        XCTAssertEqual(message, "Too many requests")
        XCTAssertEqual(retryAfter, 60)
    }

    func testErrorFromHTTPStatus400() {
        let error = RenamedError.fromHTTPStatus(400, message: "Invalid request")
        guard case .validation(let message, _) = error else {
            XCTFail("Expected validation error")
            return
        }
        XCTAssertEqual(message, "Invalid request")
    }

    func testErrorFromHTTPStatus500() {
        let error = RenamedError.fromHTTPStatus(500, message: "Server error")
        guard case .api(let message, let statusCode) = error else {
            XCTFail("Expected api error")
            return
        }
        XCTAssertEqual(message, "Server error")
        XCTAssertEqual(statusCode, 500)
    }

    // MARK: - Model Tests

    func testRenameResultDecoding() throws {
        let json = """
        {
            "originalFilename": "doc.pdf",
            "suggestedFilename": "2025-01-15_Invoice.pdf",
            "folderPath": "Invoices/2025",
            "confidence": 0.95
        }
        """.data(using: .utf8)!

        let result = try JSONDecoder().decode(RenameResult.self, from: json)
        XCTAssertEqual(result.originalFilename, "doc.pdf")
        XCTAssertEqual(result.suggestedFilename, "2025-01-15_Invoice.pdf")
        XCTAssertEqual(result.folderPath, "Invoices/2025")
        XCTAssertEqual(result.confidence, 0.95)
    }

    func testUserDecoding() throws {
        let json = """
        {
            "id": "user_123",
            "email": "test@example.com",
            "name": "Test User",
            "credits": 100,
            "team": {
                "id": "team_456",
                "name": "Acme Corp"
            }
        }
        """.data(using: .utf8)!

        let user = try JSONDecoder().decode(User.self, from: json)
        XCTAssertEqual(user.id, "user_123")
        XCTAssertEqual(user.email, "test@example.com")
        XCTAssertEqual(user.name, "Test User")
        XCTAssertEqual(user.credits, 100)
        XCTAssertEqual(user.team?.id, "team_456")
        XCTAssertEqual(user.team?.name, "Acme Corp")
    }

    func testJobStatusDecoding() throws {
        let json = """
        {
            "jobId": "job_789",
            "status": "processing",
            "progress": 50
        }
        """.data(using: .utf8)!

        let status = try JSONDecoder().decode(JobStatusResponse.self, from: json)
        XCTAssertEqual(status.jobId, "job_789")
        XCTAssertEqual(status.status, .processing)
        XCTAssertEqual(status.progress, 50)
    }

    func testPdfSplitResultDecoding() throws {
        let json = """
        {
            "originalFilename": "multi.pdf",
            "totalPages": 10,
            "documents": [
                {
                    "index": 0,
                    "filename": "multi_1.pdf",
                    "pages": "1-5",
                    "downloadUrl": "https://example.com/doc1.pdf",
                    "size": 12345
                },
                {
                    "index": 1,
                    "filename": "multi_2.pdf",
                    "pages": "6-10",
                    "downloadUrl": "https://example.com/doc2.pdf",
                    "size": 67890
                }
            ]
        }
        """.data(using: .utf8)!

        let result = try JSONDecoder().decode(PdfSplitResult.self, from: json)
        XCTAssertEqual(result.originalFilename, "multi.pdf")
        XCTAssertEqual(result.totalPages, 10)
        XCTAssertEqual(result.documents.count, 2)
        XCTAssertEqual(result.documents[0].filename, "multi_1.pdf")
        XCTAssertEqual(result.documents[0].pages, "1-5")
        XCTAssertEqual(result.documents[1].index, 1)
    }
}
