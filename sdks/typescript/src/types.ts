/**
 * Logger interface for debug logging
 */
export interface Logger {
  debug(message: string, ...args: unknown[]): void;
  info(message: string, ...args: unknown[]): void;
  warn(message: string, ...args: unknown[]): void;
  error(message: string, ...args: unknown[]): void;
}

/**
 * Configuration options for the Renamed client
 */
export interface RenamedClientOptions {
  /**
   * API key for authentication (starts with rt_)
   */
  apiKey: string;

  /**
   * Base URL for the API (default: https://www.renamed.to/api/v1)
   */
  baseUrl?: string;

  /**
   * Request timeout in milliseconds (default: 30000)
   */
  timeout?: number;

  /**
   * Maximum number of retries for failed requests (default: 2)
   */
  maxRetries?: number;

  /**
   * Custom fetch implementation (for testing or special environments)
   */
  fetch?: typeof globalThis.fetch;

  /**
   * Enable debug logging (uses console by default)
   */
  debug?: boolean;

  /**
   * Custom logger implementation (overrides debug flag)
   */
  logger?: Logger;
}

/**
 * Result of a rename operation
 */
export interface RenameResult {
  /**
   * Original filename that was uploaded
   */
  originalFilename: string;

  /**
   * AI-suggested new filename
   */
  suggestedFilename: string;

  /**
   * Suggested folder path for organization
   */
  folderPath?: string;

  /**
   * Confidence score (0-1) of the suggestion
   */
  confidence?: number;
}

/**
 * Options for rename operation
 */
export interface RenameOptions {
  /**
   * Custom template for filename generation
   */
  template?: string;
}

/**
 * Options for PDF split operation
 */
export interface PdfSplitOptions {
  /**
   * Split mode: 'auto' (AI-detected), 'pages' (every N pages), 'blank' (at blank pages)
   */
  mode?: "auto" | "pages" | "blank";

  /**
   * Number of pages per split (for 'pages' mode)
   */
  pagesPerSplit?: number;
}

/**
 * Status of an async job
 */
export type JobStatus = "pending" | "processing" | "completed" | "failed";

/**
 * Response from job status endpoint
 */
export interface JobStatusResponse {
  /**
   * Unique job identifier
   */
  jobId: string;

  /**
   * Current job status
   */
  status: JobStatus;

  /**
   * Progress percentage (0-100)
   */
  progress?: number;

  /**
   * Error message if job failed
   */
  error?: string;

  /**
   * Result data when job is completed
   */
  result?: PdfSplitResult;
}

/**
 * A single document from PDF split
 */
export interface SplitDocument {
  /**
   * Document index (0-based)
   */
  index: number;

  /**
   * Suggested filename for this document
   */
  filename: string;

  /**
   * Page range included in this document
   */
  pages: string;

  /**
   * URL to download this document
   */
  downloadUrl: string;

  /**
   * Size in bytes
   */
  size: number;
}

/**
 * Result of PDF split operation
 */
export interface PdfSplitResult {
  /**
   * Original filename
   */
  originalFilename: string;

  /**
   * Split documents
   */
  documents: SplitDocument[];

  /**
   * Total number of pages in original document
   */
  totalPages: number;
}

/**
 * Extract operation options
 */
export interface ExtractOptions {
  /**
   * Schema defining what to extract
   */
  schema?: Record<string, unknown>;

  /**
   * Prompt describing what to extract
   */
  prompt?: string;
}

/**
 * Result of extract operation
 */
export interface ExtractResult {
  /**
   * Extracted data matching the schema
   */
  data: Record<string, unknown>;

  /**
   * Confidence score (0-1)
   */
  confidence: number;
}

/**
 * User profile information
 */
export interface User {
  /**
   * User ID
   */
  id: string;

  /**
   * Email address
   */
  email: string;

  /**
   * Display name
   */
  name?: string;

  /**
   * Available credits
   */
  credits?: number;

  /**
   * Team information (if applicable)
   */
  team?: {
    id: string;
    name: string;
  };
}

/**
 * File input - can be a path, Buffer, Blob, or File
 */
export type FileInput = string | Buffer | Blob | File;

/**
 * MIME types for supported file formats
 */
export const MIME_TYPES: Record<string, string> = {
  ".pdf": "application/pdf",
  ".jpg": "image/jpeg",
  ".jpeg": "image/jpeg",
  ".png": "image/png",
  ".tiff": "image/tiff",
  ".tif": "image/tiff",
};
