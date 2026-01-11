// Main client
export { RenamedClient, AsyncJob } from "./client.js";

// Types
export type {
  RenamedClientOptions,
  RenameResult,
  RenameOptions,
  PdfSplitOptions,
  PdfSplitResult,
  JobStatusResponse,
  JobStatus,
  SplitDocument,
  ExtractOptions,
  ExtractResult,
  User,
  FileInput,
  Logger,
} from "./types.js";

// Errors
export {
  RenamedError,
  AuthenticationError,
  RateLimitError,
  ValidationError,
  NetworkError,
  TimeoutError,
  InsufficientCreditsError,
  JobError,
} from "./errors.js";
