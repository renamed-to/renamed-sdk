package renamed

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"io"
	"mime/multipart"
	"net/http"
	"net/textproto"
	"os"
	"path/filepath"
	"strings"
	"time"
)

const (
	defaultBaseURL     = "https://www.renamed.to/api/v1"
	defaultTimeout     = 30 * time.Second
	defaultMaxRetries  = 2
	defaultPollInterval = 2 * time.Second
	maxPollAttempts    = 150 // 5 minutes at 2s intervals
)

// ClientOption is a function that configures the Client.
type ClientOption func(*Client)

// WithBaseURL sets a custom base URL.
func WithBaseURL(url string) ClientOption {
	return func(c *Client) {
		c.baseURL = strings.TrimSuffix(url, "/")
	}
}

// WithTimeout sets the request timeout.
func WithTimeout(timeout time.Duration) ClientOption {
	return func(c *Client) {
		c.timeout = timeout
	}
}

// WithMaxRetries sets the maximum number of retries.
func WithMaxRetries(retries int) ClientOption {
	return func(c *Client) {
		c.maxRetries = retries
	}
}

// WithHTTPClient sets a custom HTTP client.
func WithHTTPClient(client *http.Client) ClientOption {
	return func(c *Client) {
		c.httpClient = client
	}
}

// Client is the renamed.to API client.
type Client struct {
	apiKey     string
	baseURL    string
	timeout    time.Duration
	maxRetries int
	httpClient *http.Client
}

// NewClient creates a new renamed.to API client.
//
// Example:
//
//	client := renamed.NewClient("rt_your_api_key")
//	result, err := client.Rename(ctx, "invoice.pdf", nil)
func NewClient(apiKey string, opts ...ClientOption) *Client {
	c := &Client{
		apiKey:     apiKey,
		baseURL:    defaultBaseURL,
		timeout:    defaultTimeout,
		maxRetries: defaultMaxRetries,
	}

	for _, opt := range opts {
		opt(c)
	}

	if c.httpClient == nil {
		c.httpClient = &http.Client{
			Timeout: c.timeout,
		}
	}

	return c
}

func (c *Client) buildURL(path string) string {
	if strings.HasPrefix(path, "http://") || strings.HasPrefix(path, "https://") {
		return path
	}
	if strings.HasPrefix(path, "/") {
		return c.baseURL + path
	}
	return c.baseURL + "/" + path
}

func getMimeType(filename string) string {
	ext := strings.ToLower(filepath.Ext(filename))
	if mt, ok := mimeTypes[ext]; ok {
		return mt
	}
	return "application/octet-stream"
}

func (c *Client) doRequest(ctx context.Context, req *http.Request) (*http.Response, error) {
	req.Header.Set("Authorization", "Bearer "+c.apiKey)

	var lastErr error
	for attempt := 0; attempt <= c.maxRetries; attempt++ {
		resp, err := c.httpClient.Do(req.WithContext(ctx))
		if err != nil {
			lastErr = NewNetworkError(err.Error())
			// Exponential backoff
			if attempt < c.maxRetries {
				time.Sleep(time.Duration(1<<attempt) * 100 * time.Millisecond)
			}
			continue
		}
		return resp, nil
	}
	return nil, lastErr
}

func (c *Client) request(ctx context.Context, method, path string, body io.Reader, contentType string) ([]byte, error) {
	url := c.buildURL(path)

	req, err := http.NewRequestWithContext(ctx, method, url, body)
	if err != nil {
		return nil, err
	}

	if contentType != "" {
		req.Header.Set("Content-Type", contentType)
	}

	resp, err := c.doRequest(ctx, req)
	if err != nil {
		return nil, err
	}
	defer resp.Body.Close()

	respBody, err := io.ReadAll(resp.Body)
	if err != nil {
		return nil, NewNetworkError(err.Error())
	}

	if resp.StatusCode >= 400 {
		var payload map[string]any
		_ = json.Unmarshal(respBody, &payload)
		return nil, ErrorFromHTTPStatus(resp.StatusCode, resp.Status, payload)
	}

	return respBody, nil
}

func (c *Client) uploadFile(ctx context.Context, path string, filename string, content []byte, fields map[string]string) ([]byte, error) {
	var buf bytes.Buffer
	writer := multipart.NewWriter(&buf)

	// Create part with correct Content-Type header
	h := make(textproto.MIMEHeader)
	h.Set("Content-Disposition", fmt.Sprintf(`form-data; name="file"; filename="%s"`, filename))
	h.Set("Content-Type", getMimeType(filename))

	part, err := writer.CreatePart(h)
	if err != nil {
		return nil, err
	}

	if _, err := part.Write(content); err != nil {
		return nil, err
	}

	// Add additional fields
	for key, value := range fields {
		if err := writer.WriteField(key, value); err != nil {
			return nil, err
		}
	}

	if err := writer.Close(); err != nil {
		return nil, err
	}

	return c.request(ctx, http.MethodPost, path, &buf, writer.FormDataContentType())
}

// Rename renames a file using AI.
//
// The file can be a path string or io.Reader. If it's a reader, you must provide
// a filename using the options.
//
// Example:
//
//	result, err := client.Rename(ctx, "invoice.pdf", nil)
//	if err != nil {
//	    log.Fatal(err)
//	}
//	fmt.Println(result.SuggestedFilename)
func (c *Client) Rename(ctx context.Context, file string, opts *RenameOptions) (*RenameResult, error) {
	content, err := os.ReadFile(file)
	if err != nil {
		return nil, err
	}

	filename := filepath.Base(file)
	fields := make(map[string]string)

	if opts != nil && opts.Template != "" {
		fields["template"] = opts.Template
	}

	respBody, err := c.uploadFile(ctx, "/rename", filename, content, fields)
	if err != nil {
		return nil, err
	}

	var result RenameResult
	if err := json.Unmarshal(respBody, &result); err != nil {
		return nil, err
	}

	return &result, nil
}

// RenameReader renames a file from an io.Reader.
func (c *Client) RenameReader(ctx context.Context, reader io.Reader, filename string, opts *RenameOptions) (*RenameResult, error) {
	content, err := io.ReadAll(reader)
	if err != nil {
		return nil, err
	}

	fields := make(map[string]string)
	if opts != nil && opts.Template != "" {
		fields["template"] = opts.Template
	}

	respBody, err := c.uploadFile(ctx, "/rename", filename, content, fields)
	if err != nil {
		return nil, err
	}

	var result RenameResult
	if err := json.Unmarshal(respBody, &result); err != nil {
		return nil, err
	}

	return &result, nil
}

// AsyncJob represents an async job that can be polled for completion.
type AsyncJob struct {
	client       *Client
	statusURL    string
	pollInterval time.Duration
	maxAttempts  int
}

// Status returns the current job status.
func (j *AsyncJob) Status(ctx context.Context) (*JobStatusResponse, error) {
	respBody, err := j.client.request(ctx, http.MethodGet, j.statusURL, nil, "")
	if err != nil {
		return nil, err
	}

	var status JobStatusResponse
	if err := json.Unmarshal(respBody, &status); err != nil {
		return nil, err
	}

	return &status, nil
}

// ProgressCallback is called with status updates during Wait.
type ProgressCallback func(*JobStatusResponse)

// Wait waits for the job to complete, polling at regular intervals.
func (j *AsyncJob) Wait(ctx context.Context, onProgress ProgressCallback) (*PdfSplitResult, error) {
	for attempt := 0; attempt < j.maxAttempts; attempt++ {
		status, err := j.Status(ctx)
		if err != nil {
			return nil, err
		}

		if onProgress != nil {
			onProgress(status)
		}

		if status.Status == JobStatusCompleted && status.Result != nil {
			return status.Result, nil
		}

		if status.Status == JobStatusFailed {
			return nil, NewJobError(status.Error, status.JobID)
		}

		select {
		case <-ctx.Done():
			return nil, ctx.Err()
		case <-time.After(j.pollInterval):
		}
	}

	return nil, NewJobError("Job polling timeout exceeded", "")
}

// PDFSplit splits a PDF into multiple documents.
//
// Returns an AsyncJob that can be polled for completion.
//
// Example:
//
//	job, err := client.PDFSplit(ctx, "multi-page.pdf", &renamed.PdfSplitOptions{
//	    Mode: renamed.SplitModeAuto,
//	})
//	if err != nil {
//	    log.Fatal(err)
//	}
//
//	result, err := job.Wait(ctx, func(s *renamed.JobStatusResponse) {
//	    fmt.Printf("Progress: %d%%\n", s.Progress)
//	})
func (c *Client) PDFSplit(ctx context.Context, file string, opts *PdfSplitOptions) (*AsyncJob, error) {
	content, err := os.ReadFile(file)
	if err != nil {
		return nil, err
	}

	filename := filepath.Base(file)
	fields := make(map[string]string)

	if opts != nil {
		if opts.Mode != "" {
			fields["mode"] = string(opts.Mode)
		}
		if opts.PagesPerSplit > 0 {
			fields["pagesPerSplit"] = fmt.Sprintf("%d", opts.PagesPerSplit)
		}
	}

	respBody, err := c.uploadFile(ctx, "/pdf-split", filename, content, fields)
	if err != nil {
		return nil, err
	}

	var resp pdfSplitResponse
	if err := json.Unmarshal(respBody, &resp); err != nil {
		return nil, err
	}

	return &AsyncJob{
		client:       c,
		statusURL:    resp.StatusURL,
		pollInterval: defaultPollInterval,
		maxAttempts:  maxPollAttempts,
	}, nil
}

// PDFSplitReader splits a PDF from an io.Reader.
func (c *Client) PDFSplitReader(ctx context.Context, reader io.Reader, filename string, opts *PdfSplitOptions) (*AsyncJob, error) {
	content, err := io.ReadAll(reader)
	if err != nil {
		return nil, err
	}

	fields := make(map[string]string)
	if opts != nil {
		if opts.Mode != "" {
			fields["mode"] = string(opts.Mode)
		}
		if opts.PagesPerSplit > 0 {
			fields["pagesPerSplit"] = fmt.Sprintf("%d", opts.PagesPerSplit)
		}
	}

	respBody, err := c.uploadFile(ctx, "/pdf-split", filename, content, fields)
	if err != nil {
		return nil, err
	}

	var resp pdfSplitResponse
	if err := json.Unmarshal(respBody, &resp); err != nil {
		return nil, err
	}

	return &AsyncJob{
		client:       c,
		statusURL:    resp.StatusURL,
		pollInterval: defaultPollInterval,
		maxAttempts:  maxPollAttempts,
	}, nil
}

// Extract extracts structured data from a document.
//
// Example:
//
//	result, err := client.Extract(ctx, "invoice.pdf", &renamed.ExtractOptions{
//	    Prompt: "Extract invoice number, date, and total",
//	})
func (c *Client) Extract(ctx context.Context, file string, opts *ExtractOptions) (*ExtractResult, error) {
	content, err := os.ReadFile(file)
	if err != nil {
		return nil, err
	}

	filename := filepath.Base(file)
	fields := make(map[string]string)

	if opts != nil {
		if opts.Prompt != "" {
			fields["prompt"] = opts.Prompt
		}
		if opts.Schema != nil {
			schemaJSON, err := json.Marshal(opts.Schema)
			if err != nil {
				return nil, err
			}
			fields["schema"] = string(schemaJSON)
		}
	}

	respBody, err := c.uploadFile(ctx, "/extract", filename, content, fields)
	if err != nil {
		return nil, err
	}

	var result ExtractResult
	if err := json.Unmarshal(respBody, &result); err != nil {
		return nil, err
	}

	return &result, nil
}

// ExtractReader extracts data from an io.Reader.
func (c *Client) ExtractReader(ctx context.Context, reader io.Reader, filename string, opts *ExtractOptions) (*ExtractResult, error) {
	content, err := io.ReadAll(reader)
	if err != nil {
		return nil, err
	}

	fields := make(map[string]string)
	if opts != nil {
		if opts.Prompt != "" {
			fields["prompt"] = opts.Prompt
		}
		if opts.Schema != nil {
			schemaJSON, err := json.Marshal(opts.Schema)
			if err != nil {
				return nil, err
			}
			fields["schema"] = string(schemaJSON)
		}
	}

	respBody, err := c.uploadFile(ctx, "/extract", filename, content, fields)
	if err != nil {
		return nil, err
	}

	var result ExtractResult
	if err := json.Unmarshal(respBody, &result); err != nil {
		return nil, err
	}

	return &result, nil
}

// GetUser returns the current user profile and credits.
//
// Example:
//
//	user, err := client.GetUser(ctx)
//	if err != nil {
//	    log.Fatal(err)
//	}
//	fmt.Printf("Credits remaining: %d\n", user.Credits)
func (c *Client) GetUser(ctx context.Context) (*User, error) {
	respBody, err := c.request(ctx, http.MethodGet, "/user", nil, "")
	if err != nil {
		return nil, err
	}

	var user User
	if err := json.Unmarshal(respBody, &user); err != nil {
		return nil, err
	}

	return &user, nil
}

// DownloadFile downloads a file from a URL (e.g., split document).
//
// Example:
//
//	result, _ := job.Wait(ctx, nil)
//	for _, doc := range result.Documents {
//	    content, err := client.DownloadFile(ctx, doc.DownloadURL)
//	    if err != nil {
//	        log.Fatal(err)
//	    }
//	    os.WriteFile(doc.Filename, content, 0644)
//	}
func (c *Client) DownloadFile(ctx context.Context, url string) ([]byte, error) {
	req, err := http.NewRequestWithContext(ctx, http.MethodGet, url, nil)
	if err != nil {
		return nil, err
	}

	req.Header.Set("Authorization", "Bearer "+c.apiKey)

	resp, err := c.httpClient.Do(req)
	if err != nil {
		return nil, NewNetworkError(err.Error())
	}
	defer resp.Body.Close()

	if resp.StatusCode >= 400 {
		return nil, ErrorFromHTTPStatus(resp.StatusCode, resp.Status, nil)
	}

	return io.ReadAll(resp.Body)
}
