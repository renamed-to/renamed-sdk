/**
 * Base error class for all renamed.to SDK errors
 */
export class RenamedError extends Error {
  constructor(
    message: string,
    public readonly code: string,
    public readonly statusCode?: number,
    public readonly details?: unknown
  ) {
    super(message);
    this.name = "RenamedError";
    Object.setPrototypeOf(this, new.target.prototype);
  }
}

/**
 * Authentication error - invalid or missing API key
 */
export class AuthenticationError extends RenamedError {
  constructor(message = "Invalid or missing API key") {
    super(message, "AUTHENTICATION_ERROR", 401);
    this.name = "AuthenticationError";
  }
}

/**
 * Rate limit exceeded
 */
export class RateLimitError extends RenamedError {
  constructor(
    message = "Rate limit exceeded",
    public readonly retryAfter?: number
  ) {
    super(message, "RATE_LIMIT_ERROR", 429);
    this.name = "RateLimitError";
  }
}

/**
 * Validation error - invalid request parameters
 */
export class ValidationError extends RenamedError {
  constructor(message: string, details?: unknown) {
    super(message, "VALIDATION_ERROR", 400, details);
    this.name = "ValidationError";
  }
}

/**
 * Network error - connection failed
 */
export class NetworkError extends RenamedError {
  constructor(message = "Network request failed") {
    super(message, "NETWORK_ERROR");
    this.name = "NetworkError";
  }
}

/**
 * Timeout error - request took too long
 */
export class TimeoutError extends RenamedError {
  constructor(message = "Request timed out") {
    super(message, "TIMEOUT_ERROR");
    this.name = "TimeoutError";
  }
}

/**
 * Insufficient credits error
 */
export class InsufficientCreditsError extends RenamedError {
  constructor(message = "Insufficient credits") {
    super(message, "INSUFFICIENT_CREDITS", 402);
    this.name = "InsufficientCreditsError";
  }
}

/**
 * Job error - async job failed
 */
export class JobError extends RenamedError {
  constructor(message: string, public readonly jobId?: string) {
    super(message, "JOB_ERROR");
    this.name = "JobError";
  }
}

/**
 * Create appropriate error from HTTP status code
 */
export function fromHttpStatus(
  status: number,
  statusText: string,
  payload?: unknown
): RenamedError {
  const message =
    typeof payload === "object" && payload !== null && "error" in payload
      ? String((payload as { error: unknown }).error)
      : statusText;

  switch (status) {
    case 401:
      return new AuthenticationError(message);
    case 402:
      return new InsufficientCreditsError(message);
    case 400:
    case 422:
      return new ValidationError(message, payload);
    case 429: {
      const retryAfter =
        typeof payload === "object" && payload !== null && "retryAfter" in payload
          ? Number((payload as { retryAfter: unknown }).retryAfter)
          : undefined;
      return new RateLimitError(message, retryAfter);
    }
    default:
      return new RenamedError(message, "API_ERROR", status, payload);
  }
}
