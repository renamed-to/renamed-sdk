import { describe, it, expect, vi, beforeEach } from "vitest";
import { RenamedClient, AsyncJob } from "./client.js";
import {
  AuthenticationError,
  ValidationError,
  RateLimitError,
  InsufficientCreditsError,
  NetworkError,
  TimeoutError,
} from "./errors.js";

describe("RenamedClient", () => {
  describe("constructor", () => {
    it("throws AuthenticationError when API key is missing", () => {
      expect(() => new RenamedClient({ apiKey: "" })).toThrow(AuthenticationError);
    });

    it("creates client with valid API key", () => {
      const client = new RenamedClient({ apiKey: "rt_test123" });
      expect(client).toBeInstanceOf(RenamedClient);
    });

    it("uses default base URL", () => {
      const client = new RenamedClient({ apiKey: "rt_test123" });
      expect(client).toBeDefined();
    });

    it("accepts custom options", () => {
      const client = new RenamedClient({
        apiKey: "rt_test123",
        baseUrl: "https://custom.api.com",
        timeout: 60000,
        maxRetries: 5,
      });
      expect(client).toBeDefined();
    });
  });

  describe("request", () => {
    it("includes Authorization header", async () => {
      const mockFetch = vi.fn().mockResolvedValue({
        ok: true,
        text: () => Promise.resolve('{"id": "user123"}'),
      });

      const client = new RenamedClient({
        apiKey: "rt_test123",
        fetch: mockFetch,
      });

      await client.getUser();

      expect(mockFetch).toHaveBeenCalledWith(
        expect.stringContaining("/user"),
        expect.objectContaining({
          headers: expect.any(Headers),
        })
      );

      const headers = mockFetch.mock.calls[0][1].headers;
      expect(headers.get("Authorization")).toBe("Bearer rt_test123");
    });

    it("handles 401 as AuthenticationError", async () => {
      const mockFetch = vi.fn().mockResolvedValue({
        ok: false,
        status: 401,
        statusText: "Unauthorized",
        text: () => Promise.resolve('{"error": "Invalid API key"}'),
      });

      const client = new RenamedClient({
        apiKey: "rt_invalid",
        fetch: mockFetch,
      });

      await expect(client.getUser()).rejects.toThrow(AuthenticationError);
    });

    it("handles 402 as InsufficientCreditsError", async () => {
      const mockFetch = vi.fn().mockResolvedValue({
        ok: false,
        status: 402,
        statusText: "Payment Required",
        text: () => Promise.resolve('{"error": "Insufficient credits"}'),
      });

      const client = new RenamedClient({
        apiKey: "rt_test123",
        fetch: mockFetch,
      });

      await expect(client.getUser()).rejects.toThrow(InsufficientCreditsError);
    });

    it("handles 429 as RateLimitError", async () => {
      const mockFetch = vi.fn().mockResolvedValue({
        ok: false,
        status: 429,
        statusText: "Too Many Requests",
        text: () => Promise.resolve('{"error": "Rate limit exceeded", "retryAfter": 60}'),
      });

      const client = new RenamedClient({
        apiKey: "rt_test123",
        fetch: mockFetch,
      });

      await expect(client.getUser()).rejects.toThrow(RateLimitError);
    });

    it("handles 400 as ValidationError", async () => {
      const mockFetch = vi.fn().mockResolvedValue({
        ok: false,
        status: 400,
        statusText: "Bad Request",
        text: () => Promise.resolve('{"error": "Invalid file format"}'),
      });

      const client = new RenamedClient({
        apiKey: "rt_test123",
        fetch: mockFetch,
      });

      await expect(client.getUser()).rejects.toThrow(ValidationError);
    });
  });

  describe("getUser", () => {
    it("returns user data", async () => {
      const mockUser = {
        id: "user123",
        email: "test@example.com",
        name: "Test User",
        credits: 100,
      };

      const mockFetch = vi.fn().mockResolvedValue({
        ok: true,
        text: () => Promise.resolve(JSON.stringify(mockUser)),
      });

      const client = new RenamedClient({
        apiKey: "rt_test123",
        fetch: mockFetch,
      });

      const user = await client.getUser();
      expect(user).toEqual(mockUser);
    });
  });

  describe("rename", () => {
    it("uploads file and returns result", async () => {
      const mockResult = {
        originalFilename: "doc.pdf",
        suggestedFilename: "2025-01-15_Invoice.pdf",
        folderPath: "2025/Invoices",
        confidence: 0.95,
      };

      const mockFetch = vi.fn().mockResolvedValue({
        ok: true,
        text: () => Promise.resolve(JSON.stringify(mockResult)),
      });

      const client = new RenamedClient({
        apiKey: "rt_test123",
        fetch: mockFetch,
      });

      // Test with Buffer
      const buffer = Buffer.from("fake pdf content");
      const result = await client.rename(buffer);

      expect(result).toEqual(mockResult);
      expect(mockFetch).toHaveBeenCalledWith(
        expect.stringContaining("/rename"),
        expect.objectContaining({
          method: "POST",
        })
      );
    });
  });

  describe("pdfSplit", () => {
    it("returns AsyncJob for polling", async () => {
      const mockFetch = vi.fn().mockResolvedValue({
        ok: true,
        text: () => Promise.resolve('{"statusUrl": "https://api.example.com/status/job123"}'),
      });

      const client = new RenamedClient({
        apiKey: "rt_test123",
        fetch: mockFetch,
      });

      const buffer = Buffer.from("fake pdf content");
      const job = await client.pdfSplit(buffer);

      expect(job).toBeInstanceOf(AsyncJob);
    });

    it("passes mode value through to API request", async () => {
      const mockFetch = vi.fn().mockResolvedValue({
        ok: true,
        text: () => Promise.resolve('{"statusUrl": "https://api.example.com/status/job456"}'),
      });

      const client = new RenamedClient({
        apiKey: "rt_test123",
        fetch: mockFetch,
      });

      const buffer = Buffer.from("fake pdf content");
      await client.pdfSplit(buffer, { mode: "smart" });

      expect(mockFetch).toHaveBeenCalledOnce();

      const [, requestInit] = mockFetch.mock.calls[0];
      const body = requestInit.body as FormData;
      expect(body.get("mode")).toBe("smart");
    });
  });
});

describe("AsyncJob", () => {
  it("polls until completed", async () => {
    let callCount = 0;
    const mockResult = {
      originalFilename: "multi.pdf",
      documents: [{ index: 0, filename: "doc1.pdf", pages: "1-5", downloadUrl: "https://...", size: 1000 }],
      totalPages: 10,
    };

    const mockFetch = vi.fn().mockImplementation(() => {
      callCount++;
      if (callCount < 3) {
        return Promise.resolve({
          ok: true,
          text: () =>
            Promise.resolve(
              JSON.stringify({
                jobId: "job123",
                status: "processing",
                progress: callCount * 33,
              })
            ),
        });
      }
      return Promise.resolve({
        ok: true,
        text: () =>
          Promise.resolve(
            JSON.stringify({
              jobId: "job123",
              status: "completed",
              progress: 100,
              results: mockResult,
            })
          ),
      });
    });

    const client = new RenamedClient({
      apiKey: "rt_test123",
      fetch: mockFetch,
    });

    // Create job manually for testing
    const job = new AsyncJob(client, "https://api.example.com/status/job123", 10, 10);

    const progressUpdates: number[] = [];
    const result = await job.wait((status) => {
      if (status.progress !== undefined) {
        progressUpdates.push(status.progress);
      }
    });

    expect(result).toEqual(mockResult);
    expect(progressUpdates).toContain(33);
    expect(progressUpdates).toContain(66);
  });
});
