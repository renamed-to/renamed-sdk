package renamed

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"
)

func TestNewClient(t *testing.T) {
	t.Run("creates client with API key", func(t *testing.T) {
		client := NewClient("rt_test123")
		if client == nil {
			t.Error("expected client to be created")
		}
		if client.apiKey != "rt_test123" {
			t.Errorf("expected apiKey to be rt_test123, got %s", client.apiKey)
		}
	})

	t.Run("uses default base URL", func(t *testing.T) {
		client := NewClient("rt_test123")
		if client.baseURL != defaultBaseURL {
			t.Errorf("expected baseURL to be %s, got %s", defaultBaseURL, client.baseURL)
		}
	})

	t.Run("accepts custom options", func(t *testing.T) {
		client := NewClient("rt_test123",
			WithBaseURL("https://custom.api.com"),
			WithMaxRetries(5),
		)
		if client.baseURL != "https://custom.api.com" {
			t.Errorf("expected baseURL to be https://custom.api.com, got %s", client.baseURL)
		}
		if client.maxRetries != 5 {
			t.Errorf("expected maxRetries to be 5, got %d", client.maxRetries)
		}
	})
}

func TestGetUser(t *testing.T) {
	t.Run("returns user data", func(t *testing.T) {
		server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			// Verify Authorization header
			auth := r.Header.Get("Authorization")
			if auth != "Bearer rt_test123" {
				t.Errorf("expected Authorization header, got %s", auth)
			}

			user := User{
				ID:      "user123",
				Email:   "test@example.com",
				Name:    "Test User",
				Credits: 100,
			}
			json.NewEncoder(w).Encode(user)
		}))
		defer server.Close()

		client := NewClient("rt_test123", WithBaseURL(server.URL))
		user, err := client.GetUser(context.Background())

		if err != nil {
			t.Fatalf("unexpected error: %v", err)
		}
		if user.ID != "user123" {
			t.Errorf("expected ID user123, got %s", user.ID)
		}
		if user.Email != "test@example.com" {
			t.Errorf("expected email test@example.com, got %s", user.Email)
		}
		if user.Credits != 100 {
			t.Errorf("expected credits 100, got %d", user.Credits)
		}
	})

	t.Run("returns AuthenticationError on 401", func(t *testing.T) {
		server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			w.WriteHeader(http.StatusUnauthorized)
			json.NewEncoder(w).Encode(map[string]string{"error": "Invalid API key"})
		}))
		defer server.Close()

		client := NewClient("rt_invalid", WithBaseURL(server.URL))
		_, err := client.GetUser(context.Background())

		if err == nil {
			t.Error("expected error")
		}
		if _, ok := err.(*AuthenticationError); !ok {
			t.Errorf("expected AuthenticationError, got %T", err)
		}
	})

	t.Run("returns InsufficientCreditsError on 402", func(t *testing.T) {
		server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			w.WriteHeader(http.StatusPaymentRequired)
			json.NewEncoder(w).Encode(map[string]string{"error": "Insufficient credits"})
		}))
		defer server.Close()

		client := NewClient("rt_test123", WithBaseURL(server.URL))
		_, err := client.GetUser(context.Background())

		if err == nil {
			t.Error("expected error")
		}
		if _, ok := err.(*InsufficientCreditsError); !ok {
			t.Errorf("expected InsufficientCreditsError, got %T", err)
		}
	})

	t.Run("returns RateLimitError on 429", func(t *testing.T) {
		server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			w.WriteHeader(http.StatusTooManyRequests)
			json.NewEncoder(w).Encode(map[string]any{"error": "Rate limit exceeded", "retryAfter": 60.0})
		}))
		defer server.Close()

		client := NewClient("rt_test123", WithBaseURL(server.URL))
		_, err := client.GetUser(context.Background())

		if err == nil {
			t.Error("expected error")
		}
		rateLimitErr, ok := err.(*RateLimitError)
		if !ok {
			t.Errorf("expected RateLimitError, got %T", err)
		}
		if rateLimitErr.RetryAfter != 60 {
			t.Errorf("expected RetryAfter 60, got %d", rateLimitErr.RetryAfter)
		}
	})

	t.Run("returns ValidationError on 400", func(t *testing.T) {
		server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			w.WriteHeader(http.StatusBadRequest)
			json.NewEncoder(w).Encode(map[string]string{"error": "Invalid request"})
		}))
		defer server.Close()

		client := NewClient("rt_test123", WithBaseURL(server.URL))
		_, err := client.GetUser(context.Background())

		if err == nil {
			t.Error("expected error")
		}
		if _, ok := err.(*ValidationError); !ok {
			t.Errorf("expected ValidationError, got %T", err)
		}
	})
}

func TestRename(t *testing.T) {
	t.Run("uploads file and returns result", func(t *testing.T) {
		server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			if r.Method != http.MethodPost {
				t.Errorf("expected POST, got %s", r.Method)
			}

			// Verify multipart form
			if err := r.ParseMultipartForm(32 << 20); err != nil {
				t.Errorf("failed to parse multipart form: %v", err)
			}

			file, header, err := r.FormFile("file")
			if err != nil {
				t.Errorf("failed to get file: %v", err)
			}
			defer file.Close()

			if header.Filename == "" {
				t.Error("expected filename")
			}

			result := RenameResult{
				OriginalFilename:  header.Filename,
				SuggestedFilename: "2025-01-15_Invoice.pdf",
				FolderPath:        "2025/Invoices",
				Confidence:        0.95,
			}
			json.NewEncoder(w).Encode(result)
		}))
		defer server.Close()

		client := NewClient("rt_test123", WithBaseURL(server.URL))

		// Create temp file for testing
		result, err := client.RenameReader(
			context.Background(),
			&mockReader{data: []byte("fake pdf content")},
			"test.pdf",
			nil,
		)

		if err != nil {
			t.Fatalf("unexpected error: %v", err)
		}
		if result.SuggestedFilename != "2025-01-15_Invoice.pdf" {
			t.Errorf("expected suggested filename, got %s", result.SuggestedFilename)
		}
		if result.Confidence != 0.95 {
			t.Errorf("expected confidence 0.95, got %f", result.Confidence)
		}
	})
}

func TestPDFSplit(t *testing.T) {
	t.Run("returns async job", func(t *testing.T) {
		server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			json.NewEncoder(w).Encode(pdfSplitResponse{
				StatusURL: "https://api.example.com/status/job123",
			})
		}))
		defer server.Close()

		client := NewClient("rt_test123", WithBaseURL(server.URL))

		job, err := client.PDFSplitReader(
			context.Background(),
			&mockReader{data: []byte("fake pdf content")},
			"test.pdf",
			nil,
		)

		if err != nil {
			t.Fatalf("unexpected error: %v", err)
		}
		if job == nil {
			t.Error("expected job")
		}
	})
}

func TestAsyncJob(t *testing.T) {
	t.Run("polls until completed", func(t *testing.T) {
		callCount := 0
		mockResult := PdfSplitResult{
			OriginalFilename: "multi.pdf",
			Documents: []SplitDocument{
				{
					Index:       0,
					Filename:    "doc1.pdf",
					Pages:       "1-5",
					DownloadURL: "https://...",
					Size:        1000,
				},
			},
			TotalPages: 10,
		}

		server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			callCount++
			var status JobStatusResponse
			if callCount < 3 {
				status = JobStatusResponse{
					JobID:    "job123",
					Status:   JobStatusProcessing,
					Progress: callCount * 33,
				}
			} else {
				status = JobStatusResponse{
					JobID:    "job123",
					Status:   JobStatusCompleted,
					Progress: 100,
					Result:   &mockResult,
				}
			}
			json.NewEncoder(w).Encode(status)
		}))
		defer server.Close()

		client := NewClient("rt_test123", WithBaseURL(server.URL))

		job := &AsyncJob{
			client:       client,
			statusURL:    server.URL + "/status/job123",
			pollInterval: 1, // 1ms for fast testing
			maxAttempts:  10,
		}

		progressUpdates := []int{}
		result, err := job.Wait(context.Background(), func(s *JobStatusResponse) {
			progressUpdates = append(progressUpdates, s.Progress)
		})

		if err != nil {
			t.Fatalf("unexpected error: %v", err)
		}
		if result.OriginalFilename != "multi.pdf" {
			t.Errorf("expected original filename multi.pdf, got %s", result.OriginalFilename)
		}
		if len(result.Documents) != 1 {
			t.Errorf("expected 1 document, got %d", len(result.Documents))
		}

		// Check progress was tracked
		found33 := false
		found66 := false
		for _, p := range progressUpdates {
			if p == 33 {
				found33 = true
			}
			if p == 66 {
				found66 = true
			}
		}
		if !found33 || !found66 {
			t.Errorf("expected progress updates 33 and 66, got %v", progressUpdates)
		}
	})
}

// mockReader is a simple io.Reader for testing
type mockReader struct {
	data []byte
	pos  int
}

func (r *mockReader) Read(p []byte) (n int, err error) {
	if r.pos >= len(r.data) {
		return 0, io.EOF
	}
	n = copy(p, r.data[r.pos:])
	r.pos += n
	return n, nil
}

// testLogger captures log output for testing
type testLogger struct {
	buf bytes.Buffer
}

func (l *testLogger) Printf(format string, v ...any) {
	l.buf.WriteString(fmt.Sprintf(format, v...))
	l.buf.WriteString("\n")
}

func (l *testLogger) output() string {
	return l.buf.String()
}

func (l *testLogger) contains(s string) bool {
	return strings.Contains(l.buf.String(), s)
}

func TestMaskAPIKey(t *testing.T) {
	tests := []struct {
		name     string
		input    string
		expected string
	}{
		{"standard key", "rt_abc123xyz456", "rt_...z456"},
		{"short key", "abc", "***"},
		{"exactly 7 chars", "1234567", "***"},
		{"8 chars", "12345678", "123...5678"},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			result := maskAPIKey(tt.input)
			if result != tt.expected {
				t.Errorf("maskAPIKey(%q) = %q, want %q", tt.input, result, tt.expected)
			}
		})
	}
}

func TestFormatBytes(t *testing.T) {
	tests := []struct {
		name     string
		input    int64
		expected string
	}{
		{"bytes", 500, "500 B"},
		{"kilobytes", 1536, "1.5 KB"},
		{"megabytes", 1572864, "1.5 MB"},
		{"gigabytes", 1610612736, "1.5 GB"},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			result := formatBytes(tt.input)
			if result != tt.expected {
				t.Errorf("formatBytes(%d) = %q, want %q", tt.input, result, tt.expected)
			}
		})
	}
}

func TestExtractPath(t *testing.T) {
	tests := []struct {
		name     string
		input    string
		expected string
	}{
		{"full URL", "https://api.example.com/v1/rename", "/v1/rename"},
		{"path only", "/v1/rename", "/v1/rename"},
		{"relative path", "v1/rename", "/v1/rename"},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			result := extractPath(tt.input)
			if result != tt.expected {
				t.Errorf("extractPath(%q) = %q, want %q", tt.input, result, tt.expected)
			}
		})
	}
}

func TestWithDebug(t *testing.T) {
	t.Run("enables debug logging", func(t *testing.T) {
		client := NewClient("rt_test123", WithDebug(true))
		if client.logger == nil {
			t.Error("expected logger to be set")
		}
	})

	t.Run("disables debug logging", func(t *testing.T) {
		client := NewClient("rt_test123", WithDebug(false))
		if client.logger != nil {
			t.Error("expected logger to be nil")
		}
	})
}

func TestWithLogger(t *testing.T) {
	t.Run("sets custom logger", func(t *testing.T) {
		logger := &testLogger{}
		client := NewClient("rt_test123", WithLogger(logger))
		if client.logger != logger {
			t.Error("expected custom logger to be set")
		}
	})
}

func TestDebugLogging(t *testing.T) {
	t.Run("logs HTTP requests", func(t *testing.T) {
		logger := &testLogger{}
		server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			json.NewEncoder(w).Encode(User{ID: "user123"})
		}))
		defer server.Close()

		client := NewClient("rt_test123", WithBaseURL(server.URL), WithLogger(logger))
		_, _ = client.GetUser(context.Background())

		if !logger.contains("[Renamed]") {
			t.Error("expected log to contain [Renamed] prefix")
		}
		if !logger.contains("GET") {
			t.Error("expected log to contain HTTP method")
		}
		if !logger.contains("/user") {
			t.Error("expected log to contain path")
		}
	})

	t.Run("logs file uploads", func(t *testing.T) {
		logger := &testLogger{}
		server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			json.NewEncoder(w).Encode(RenameResult{SuggestedFilename: "test.pdf"})
		}))
		defer server.Close()

		client := NewClient("rt_test123", WithBaseURL(server.URL), WithLogger(logger))
		_, _ = client.RenameReader(
			context.Background(),
			&mockReader{data: []byte("fake pdf content")},
			"document.pdf",
			nil,
		)

		if !logger.contains("Upload:") {
			t.Error("expected log to contain 'Upload:'")
		}
		if !logger.contains("document.pdf") {
			t.Error("expected log to contain filename")
		}
	})

	t.Run("logs job polling", func(t *testing.T) {
		logger := &testLogger{}
		server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			json.NewEncoder(w).Encode(JobStatusResponse{
				JobID:    "job123",
				Status:   JobStatusCompleted,
				Progress: 100,
				Result:   &PdfSplitResult{},
			})
		}))
		defer server.Close()

		client := NewClient("rt_test123", WithBaseURL(server.URL), WithLogger(logger))
		job := &AsyncJob{
			client:       client,
			statusURL:    server.URL + "/status/job123",
			pollInterval: 1,
			maxAttempts:  10,
		}

		_, _ = job.Wait(context.Background(), nil)

		if !logger.contains("Job") {
			t.Error("expected log to contain 'Job'")
		}
		if !logger.contains("job123") {
			t.Error("expected log to contain job ID")
		}
	})

	t.Run("does not log when logger is nil", func(t *testing.T) {
		server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			json.NewEncoder(w).Encode(User{ID: "user123"})
		}))
		defer server.Close()

		// No logger set - should not panic
		client := NewClient("rt_test123", WithBaseURL(server.URL))
		_, err := client.GetUser(context.Background())

		if err != nil {
			t.Errorf("unexpected error: %v", err)
		}
	})
}
