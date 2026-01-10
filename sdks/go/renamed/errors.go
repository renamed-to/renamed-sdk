// Package renamed provides the official Go SDK for the renamed.to API.
package renamed

import (
	"fmt"
)

// RenamedError is the base error type for all SDK errors.
type RenamedError struct {
	Message    string
	Code       string
	StatusCode int
	Details    any
}

func (e *RenamedError) Error() string {
	if e.StatusCode > 0 {
		return fmt.Sprintf("%s (status %d): %s", e.Code, e.StatusCode, e.Message)
	}
	return fmt.Sprintf("%s: %s", e.Code, e.Message)
}

// AuthenticationError indicates invalid or missing API key.
type AuthenticationError struct {
	RenamedError
}

// NewAuthenticationError creates a new authentication error.
func NewAuthenticationError(message string) *AuthenticationError {
	if message == "" {
		message = "Invalid or missing API key"
	}
	return &AuthenticationError{
		RenamedError: RenamedError{
			Message:    message,
			Code:       "AUTHENTICATION_ERROR",
			StatusCode: 401,
		},
	}
}

// RateLimitError indicates rate limit exceeded.
type RateLimitError struct {
	RenamedError
	RetryAfter int
}

// NewRateLimitError creates a new rate limit error.
func NewRateLimitError(message string, retryAfter int) *RateLimitError {
	if message == "" {
		message = "Rate limit exceeded"
	}
	return &RateLimitError{
		RenamedError: RenamedError{
			Message:    message,
			Code:       "RATE_LIMIT_ERROR",
			StatusCode: 429,
		},
		RetryAfter: retryAfter,
	}
}

// ValidationError indicates invalid request parameters.
type ValidationError struct {
	RenamedError
}

// NewValidationError creates a new validation error.
func NewValidationError(message string, details any) *ValidationError {
	return &ValidationError{
		RenamedError: RenamedError{
			Message:    message,
			Code:       "VALIDATION_ERROR",
			StatusCode: 400,
			Details:    details,
		},
	}
}

// NetworkError indicates a network connection failure.
type NetworkError struct {
	RenamedError
}

// NewNetworkError creates a new network error.
func NewNetworkError(message string) *NetworkError {
	if message == "" {
		message = "Network request failed"
	}
	return &NetworkError{
		RenamedError: RenamedError{
			Message: message,
			Code:    "NETWORK_ERROR",
		},
	}
}

// TimeoutError indicates a request timeout.
type TimeoutError struct {
	RenamedError
}

// NewTimeoutError creates a new timeout error.
func NewTimeoutError(message string) *TimeoutError {
	if message == "" {
		message = "Request timed out"
	}
	return &TimeoutError{
		RenamedError: RenamedError{
			Message: message,
			Code:    "TIMEOUT_ERROR",
		},
	}
}

// InsufficientCreditsError indicates not enough credits.
type InsufficientCreditsError struct {
	RenamedError
}

// NewInsufficientCreditsError creates a new insufficient credits error.
func NewInsufficientCreditsError(message string) *InsufficientCreditsError {
	if message == "" {
		message = "Insufficient credits"
	}
	return &InsufficientCreditsError{
		RenamedError: RenamedError{
			Message:    message,
			Code:       "INSUFFICIENT_CREDITS",
			StatusCode: 402,
		},
	}
}

// JobError indicates an async job failure.
type JobError struct {
	RenamedError
	JobID string
}

// NewJobError creates a new job error.
func NewJobError(message string, jobID string) *JobError {
	return &JobError{
		RenamedError: RenamedError{
			Message: message,
			Code:    "JOB_ERROR",
		},
		JobID: jobID,
	}
}

// ErrorFromHTTPStatus creates an appropriate error from an HTTP status code.
func ErrorFromHTTPStatus(status int, statusText string, payload map[string]any) error {
	message := statusText
	if payload != nil {
		if errMsg, ok := payload["error"].(string); ok {
			message = errMsg
		}
	}

	switch status {
	case 401:
		return NewAuthenticationError(message)
	case 402:
		return NewInsufficientCreditsError(message)
	case 400, 422:
		return NewValidationError(message, payload)
	case 429:
		retryAfter := 0
		if payload != nil {
			if ra, ok := payload["retryAfter"].(float64); ok {
				retryAfter = int(ra)
			}
		}
		return NewRateLimitError(message, retryAfter)
	default:
		return &RenamedError{
			Message:    message,
			Code:       "API_ERROR",
			StatusCode: status,
			Details:    payload,
		}
	}
}
