import { readFileSync } from "node:fs";
import { basename, extname } from "node:path";
import {
  AuthenticationError,
  NetworkError,
  TimeoutError,
  JobError,
  fromHttpStatus,
} from "./errors.js";
import {
  RenamedClientOptions,
  RenameResult,
  RenameOptions,
  PdfSplitOptions,
  PdfSplitResult,
  JobStatusResponse,
  ExtractOptions,
  ExtractResult,
  User,
  FileInput,
  Logger,
  MIME_TYPES,
} from "./types.js";

const DEFAULT_BASE_URL = "https://www.renamed.to/api/v1";
const DEFAULT_TIMEOUT = 30_000;
const DEFAULT_MAX_RETRIES = 2;
const POLL_INTERVAL = 2_000;
const MAX_POLL_ATTEMPTS = 150; // 5 minutes at 2s intervals
const LOG_PREFIX = "[Renamed]";

/**
 * Mask API key for logging: rt_abc...wxyz -> rt_...wxyz
 */
function maskApiKey(apiKey: string): string {
  if (apiKey.length <= 7) {
    return "***";
  }
  const prefix = apiKey.slice(0, 3);
  const suffix = apiKey.slice(-4);
  return `${prefix}...${suffix}`;
}

/**
 * Format file size in human-readable format
 */
function formatFileSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

/**
 * Extract path from URL for logging (don't log full URL which may contain sensitive data)
 */
function extractUrlPath(url: string): string {
  try {
    const parsed = new URL(url);
    return parsed.pathname;
  } catch {
    // If not a valid URL, return as-is (likely already a path)
    return url;
  }
}

function getMimeType(filename: string): string {
  const ext = extname(filename).toLowerCase();
  return MIME_TYPES[ext] ?? "application/octet-stream";
}

function isAbsoluteUrl(path: string): boolean {
  return path.startsWith("http://") || path.startsWith("https://");
}

/**
 * Async job handle for long-running operations like PDF split
 */
export class AsyncJob<T> {
  private pollInterval: number;
  private maxAttempts: number;
  private logger: Logger | undefined;
  private jobId: string | undefined;

  constructor(
    private readonly client: RenamedClient,
    private readonly statusUrl: string,
    pollInterval = POLL_INTERVAL,
    maxAttempts = MAX_POLL_ATTEMPTS,
    logger?: Logger
  ) {
    this.pollInterval = pollInterval;
    this.maxAttempts = maxAttempts;
    this.logger = logger;
  }

  /**
   * Get current job status
   */
  async status(): Promise<JobStatusResponse> {
    return this.client.request<JobStatusResponse>(this.statusUrl, { method: "GET" });
  }

  /**
   * Wait for job completion, polling at regular intervals
   */
  async wait(onProgress?: (status: JobStatusResponse) => void): Promise<T> {
    let attempts = 0;

    while (attempts < this.maxAttempts) {
      const status = await this.status();

      // Capture jobId for logging on first status check
      if (!this.jobId && status.jobId) {
        this.jobId = status.jobId;
      }

      // Log job status with progress
      if (this.logger) {
        const progressStr = status.progress !== undefined ? ` (${status.progress}%)` : "";
        this.logger.debug(`${LOG_PREFIX} Job ${status.jobId}: ${status.status}${progressStr}`);
      }

      if (onProgress) {
        onProgress(status);
      }

      if (status.status === "completed" && status.result) {
        return status.result as T;
      }

      if (status.status === "failed") {
        throw new JobError(status.error ?? "Job failed", status.jobId);
      }

      attempts++;
      await new Promise((resolve) => setTimeout(resolve, this.pollInterval));
    }

    throw new JobError("Job polling timeout exceeded");
  }
}

/**
 * renamed.to API client
 */
export class RenamedClient {
  private readonly apiKey: string;
  private readonly baseUrl: string;
  private readonly timeout: number;
  private readonly maxRetries: number;
  private readonly fetchImpl: typeof globalThis.fetch;
  private readonly logger: Logger | undefined;

  constructor(options: RenamedClientOptions) {
    if (!options.apiKey) {
      throw new AuthenticationError("API key is required");
    }

    this.apiKey = options.apiKey;
    this.baseUrl = options.baseUrl ?? DEFAULT_BASE_URL;
    this.timeout = options.timeout ?? DEFAULT_TIMEOUT;
    this.maxRetries = options.maxRetries ?? DEFAULT_MAX_RETRIES;
    this.fetchImpl = options.fetch ?? globalThis.fetch;

    // Logger setup: explicit logger takes precedence, then debug flag
    if (options.logger) {
      this.logger = options.logger;
    } else if (options.debug) {
      this.logger = console;
    }

    if (this.logger) {
      this.logger.debug(`${LOG_PREFIX} Client initialized (apiKey: ${maskApiKey(this.apiKey)})`);
    }
  }

  /**
   * Make an authenticated request to the API
   */
  async request<T>(path: string, init: RequestInit = {}): Promise<T> {
    const url = isAbsoluteUrl(path)
      ? path
      : path.startsWith("/")
        ? `${this.baseUrl}${path}`
        : `${this.baseUrl}/${path}`;

    const method = init.method ?? "GET";
    const logPath = extractUrlPath(url);

    const headers = new Headers(init.headers as HeadersInit | undefined);
    if (!headers.has("Content-Type") && init.body && typeof init.body === "string") {
      headers.set("Content-Type", "application/json");
    }
    headers.set("Authorization", `Bearer ${this.apiKey}`);

    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), this.timeout);

    let lastError: Error | undefined;
    let attempts = 0;

    while (attempts <= this.maxRetries) {
      const startTime = Date.now();

      try {
        const response = await this.fetchImpl(url, {
          ...init,
          headers,
          signal: controller.signal,
        });

        clearTimeout(timeoutId);

        const elapsed = Date.now() - startTime;
        if (this.logger) {
          this.logger.debug(`${LOG_PREFIX} ${method} ${logPath} -> ${response.status} (${elapsed}ms)`);
        }

        const text = await response.text();

        if (!response.ok) {
          let payload: unknown = text;
          try {
            payload = text ? JSON.parse(text) : {};
          } catch {
            // ignore parse error
          }
          throw fromHttpStatus(response.status, response.statusText, payload);
        }

        return (text ? JSON.parse(text) : {}) as T;
      } catch (error) {
        clearTimeout(timeoutId);

        const elapsed = Date.now() - startTime;
        if (this.logger && !(error instanceof Error && error.name === "AbortError")) {
          this.logger.debug(`${LOG_PREFIX} ${method} ${logPath} -> error (${elapsed}ms)`);
        }

        if (error instanceof Error && error.name === "AbortError") {
          throw new TimeoutError();
        }

        if (error instanceof TypeError && error.message.includes("fetch")) {
          throw new NetworkError();
        }

        lastError = error as Error;

        // Don't retry client errors (4xx)
        if (
          error instanceof Error &&
          "statusCode" in error &&
          typeof (error as { statusCode: number }).statusCode === "number"
        ) {
          const statusCode = (error as { statusCode: number }).statusCode;
          if (statusCode >= 400 && statusCode < 500) {
            throw error;
          }
        }

        attempts++;
        if (attempts <= this.maxRetries) {
          // Exponential backoff
          const backoffMs = Math.pow(2, attempts) * 100;
          if (this.logger) {
            this.logger.debug(`${LOG_PREFIX} Retry attempt ${attempts}/${this.maxRetries}, waiting ${backoffMs}ms`);
          }
          await new Promise((resolve) => setTimeout(resolve, backoffMs));
        }
      }
    }

    throw lastError ?? new NetworkError();
  }

  /**
   * Upload a file to the API
   */
  async uploadFile<T>(
    path: string,
    file: FileInput,
    filename?: string,
    fieldName = "file",
    additionalFields?: Record<string, string>
  ): Promise<T> {
    const formData = new FormData();
    let blob: Blob;
    let resolvedFilename: string;

    if (typeof file === "string") {
      // File path - convert to Uint8Array for Blob compatibility
      const buffer = readFileSync(file);
      resolvedFilename = filename ?? basename(file);
      blob = new Blob([new Uint8Array(buffer)], { type: getMimeType(resolvedFilename) });
    } else if (Buffer.isBuffer(file)) {
      // Buffer - convert to Uint8Array for Blob compatibility
      resolvedFilename = filename ?? "file";
      blob = new Blob([new Uint8Array(file)], { type: getMimeType(resolvedFilename) });
    } else if (file instanceof Blob) {
      // Blob or File
      blob = file;
      resolvedFilename = filename ?? (file instanceof File ? file.name : "file");
    } else {
      throw new Error("Invalid file input");
    }

    // Log file upload details
    if (this.logger) {
      this.logger.debug(`${LOG_PREFIX} Upload: ${resolvedFilename} (${formatFileSize(blob.size)})`);
    }

    formData.append(fieldName, blob, resolvedFilename);

    if (additionalFields) {
      for (const [key, value] of Object.entries(additionalFields)) {
        formData.append(key, value);
      }
    }

    const url = isAbsoluteUrl(path)
      ? path
      : path.startsWith("/")
        ? `${this.baseUrl}${path}`
        : `${this.baseUrl}/${path}`;

    const logPath = extractUrlPath(url);
    const startTime = Date.now();
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), this.timeout);

    try {
      const response = await this.fetchImpl(url, {
        method: "POST",
        headers: {
          Authorization: `Bearer ${this.apiKey}`,
        },
        body: formData,
        signal: controller.signal,
      });

      clearTimeout(timeoutId);

      const elapsed = Date.now() - startTime;
      if (this.logger) {
        this.logger.debug(`${LOG_PREFIX} POST ${logPath} -> ${response.status} (${elapsed}ms)`);
      }

      const text = await response.text();

      if (!response.ok) {
        let payload: unknown = text;
        try {
          payload = text ? JSON.parse(text) : {};
        } catch {
          // ignore parse error
        }
        throw fromHttpStatus(response.status, response.statusText, payload);
      }

      return (text ? JSON.parse(text) : {}) as T;
    } catch (error) {
      clearTimeout(timeoutId);

      const elapsed = Date.now() - startTime;
      if (this.logger && !(error instanceof Error && error.name === "AbortError")) {
        this.logger.debug(`${LOG_PREFIX} POST ${logPath} -> error (${elapsed}ms)`);
      }

      if (error instanceof Error && error.name === "AbortError") {
        throw new TimeoutError();
      }

      if (error instanceof TypeError && error.message.includes("fetch")) {
        throw new NetworkError();
      }

      throw error;
    }
  }

  /**
   * Rename a file using AI
   *
   * @example
   * ```ts
   * const result = await client.rename("invoice.pdf");
   * console.log(result.suggestedFilename); // "2025-01-15_AcmeCorp_INV-12345.pdf"
   * ```
   */
  async rename(file: FileInput, options?: RenameOptions): Promise<RenameResult> {
    const additionalFields: Record<string, string> = {};

    if (options?.template) {
      additionalFields.template = options.template;
    }

    return this.uploadFile<RenameResult>(
      "/rename",
      file,
      undefined,
      "file",
      Object.keys(additionalFields).length > 0 ? additionalFields : undefined
    );
  }

  /**
   * Split a PDF into multiple documents
   *
   * Returns an AsyncJob that can be polled for completion.
   *
   * @example
   * ```ts
   * const job = await client.pdfSplit("multi-page.pdf", { mode: "auto" });
   * const result = await job.wait((status) => {
   *   console.log(`Progress: ${status.progress}%`);
   * });
   * for (const doc of result.documents) {
   *   console.log(doc.filename, doc.downloadUrl);
   * }
   * ```
   */
  async pdfSplit(
    file: FileInput,
    options?: PdfSplitOptions
  ): Promise<AsyncJob<PdfSplitResult>> {
    const additionalFields: Record<string, string> = {};

    if (options?.mode) {
      additionalFields.mode = options.mode;
    }
    if (options?.pagesPerSplit !== undefined) {
      additionalFields.pagesPerSplit = String(options.pagesPerSplit);
    }

    const response = await this.uploadFile<{ statusUrl: string }>(
      "/pdf-split",
      file,
      undefined,
      "file",
      Object.keys(additionalFields).length > 0 ? additionalFields : undefined
    );

    return new AsyncJob<PdfSplitResult>(
      this,
      response.statusUrl,
      POLL_INTERVAL,
      MAX_POLL_ATTEMPTS,
      this.logger
    );
  }

  /**
   * Extract structured data from a document
   *
   * @example
   * ```ts
   * const result = await client.extract("invoice.pdf", {
   *   prompt: "Extract invoice number, date, and total amount"
   * });
   * console.log(result.data);
   * ```
   */
  async extract(file: FileInput, options?: ExtractOptions): Promise<ExtractResult> {
    const additionalFields: Record<string, string> = {};

    if (options?.schema) {
      additionalFields.schema = JSON.stringify(options.schema);
    }
    if (options?.prompt) {
      additionalFields.prompt = options.prompt;
    }

    return this.uploadFile<ExtractResult>(
      "/extract",
      file,
      undefined,
      "file",
      Object.keys(additionalFields).length > 0 ? additionalFields : undefined
    );
  }

  /**
   * Get current user profile and credits
   *
   * @example
   * ```ts
   * const user = await client.getUser();
   * console.log(`Credits remaining: ${user.credits}`);
   * ```
   */
  async getUser(): Promise<User> {
    return this.request<User>("/user", { method: "GET" });
  }

  /**
   * Download a file from a URL (e.g., split document)
   *
   * @example
   * ```ts
   * const result = await job.wait();
   * for (const doc of result.documents) {
   *   const buffer = await client.downloadFile(doc.downloadUrl);
   *   fs.writeFileSync(doc.filename, buffer);
   * }
   * ```
   */
  async downloadFile(url: string): Promise<Buffer> {
    const logPath = extractUrlPath(url);
    const startTime = Date.now();
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), this.timeout);

    try {
      const response = await this.fetchImpl(url, {
        headers: {
          Authorization: `Bearer ${this.apiKey}`,
        },
        signal: controller.signal,
      });

      clearTimeout(timeoutId);

      const elapsed = Date.now() - startTime;
      if (this.logger) {
        this.logger.debug(`${LOG_PREFIX} GET ${logPath} -> ${response.status} (${elapsed}ms)`);
      }

      if (!response.ok) {
        throw fromHttpStatus(response.status, response.statusText);
      }

      const arrayBuffer = await response.arrayBuffer();
      return Buffer.from(arrayBuffer);
    } catch (error) {
      clearTimeout(timeoutId);

      const elapsed = Date.now() - startTime;
      if (this.logger && !(error instanceof Error && error.name === "AbortError")) {
        this.logger.debug(`${LOG_PREFIX} GET ${logPath} -> error (${elapsed}ms)`);
      }

      if (error instanceof Error && error.name === "AbortError") {
        throw new TimeoutError();
      }

      if (error instanceof TypeError && error.message.includes("fetch")) {
        throw new NetworkError();
      }

      throw error;
    }
  }
}
