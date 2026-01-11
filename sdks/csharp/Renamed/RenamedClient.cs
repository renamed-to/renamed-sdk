using System.Diagnostics;
using System.Net;
using System.Net.Http.Headers;
using System.Text;
using System.Text.Json;
using System.Text.Json.Serialization;
using Renamed.Sdk.Exceptions;
using Renamed.Sdk.Logging;
using Renamed.Sdk.Models;

namespace Renamed.Sdk;

/// <summary>
/// Client for interacting with the renamed.to API.
/// Provides methods for AI-powered file renaming, PDF splitting, and data extraction.
/// </summary>
public sealed class RenamedClient : IDisposable
{
    private readonly HttpClient _httpClient;
    private readonly bool _ownsHttpClient;
    private readonly string _apiKey;
    private readonly string _baseUrl;
    private readonly int _maxRetries;
    private readonly IRenamedLogger? _logger;
    private bool _disposed;

    private static readonly JsonSerializerOptions JsonOptions = new()
    {
        PropertyNamingPolicy = JsonNamingPolicy.CamelCase,
        DefaultIgnoreCondition = JsonIgnoreCondition.WhenWritingNull,
        Converters = { new JsonStringEnumConverter(JsonNamingPolicy.CamelCase) }
    };

    private static readonly Dictionary<string, string> MimeTypes = new(StringComparer.OrdinalIgnoreCase)
    {
        [".pdf"] = "application/pdf",
        [".jpg"] = "image/jpeg",
        [".jpeg"] = "image/jpeg",
        [".png"] = "image/png",
        [".tiff"] = "image/tiff",
        [".tif"] = "image/tiff"
    };

    /// <summary>
    /// Creates a new RenamedClient instance.
    /// </summary>
    /// <param name="options">Client configuration options.</param>
    /// <exception cref="AuthenticationException">Thrown when API key is missing.</exception>
    public RenamedClient(RenamedClientOptions options)
    {
        ArgumentNullException.ThrowIfNull(options);

        if (string.IsNullOrWhiteSpace(options.ApiKey))
        {
            throw new AuthenticationException("API key is required");
        }

        _apiKey = options.ApiKey;
        _baseUrl = options.BaseUrl.TrimEnd('/');
        _maxRetries = options.MaxRetries;

        // Initialize logger based on Debug flag
        if (options.Debug)
        {
            _logger = options.Logger ?? ConsoleLogger.Instance;
        }

        if (options.HttpClient is not null)
        {
            _httpClient = options.HttpClient;
            _ownsHttpClient = false;
        }
        else
        {
            _httpClient = new HttpClient { Timeout = options.Timeout };
            _ownsHttpClient = true;
        }
    }

    /// <summary>
    /// Creates a new RenamedClient with just an API key using default options.
    /// </summary>
    /// <param name="apiKey">API key for authentication.</param>
    public RenamedClient(string apiKey)
        : this(new RenamedClientOptions { ApiKey = apiKey })
    {
    }

    /// <summary>
    /// Gets the current user profile and credits.
    /// </summary>
    /// <param name="cancellationToken">Cancellation token.</param>
    /// <returns>User profile information.</returns>
    /// <example>
    /// <code>
    /// var user = await client.GetUserAsync();
    /// Console.WriteLine($"Credits remaining: {user.Credits}");
    /// </code>
    /// </example>
    public Task<User> GetUserAsync(CancellationToken cancellationToken = default)
    {
        return RequestAsync<User>("/user", HttpMethod.Get, cancellationToken: cancellationToken);
    }

    /// <summary>
    /// Renames a file using AI.
    /// </summary>
    /// <param name="fileStream">The file stream to rename.</param>
    /// <param name="fileName">The original filename.</param>
    /// <param name="options">Optional rename options.</param>
    /// <param name="cancellationToken">Cancellation token.</param>
    /// <returns>The rename result with suggested filename.</returns>
    /// <example>
    /// <code>
    /// using var stream = File.OpenRead("invoice.pdf");
    /// var result = await client.RenameAsync(stream, "invoice.pdf");
    /// Console.WriteLine(result.SuggestedFilename); // "2025-01-15_AcmeCorp_INV-12345.pdf"
    /// </code>
    /// </example>
    public Task<RenameResult> RenameAsync(
        Stream fileStream,
        string fileName,
        RenameOptions? options = null,
        CancellationToken cancellationToken = default)
    {
        var additionalFields = new Dictionary<string, string>();

        if (!string.IsNullOrEmpty(options?.Template))
        {
            additionalFields["template"] = options.Template;
        }

        return UploadFileAsync<RenameResult>(
            "/rename",
            fileStream,
            fileName,
            additionalFields.Count > 0 ? additionalFields : null,
            cancellationToken);
    }

    /// <summary>
    /// Renames a file using AI.
    /// </summary>
    /// <param name="filePath">Path to the file to rename.</param>
    /// <param name="options">Optional rename options.</param>
    /// <param name="cancellationToken">Cancellation token.</param>
    /// <returns>The rename result with suggested filename.</returns>
    /// <example>
    /// <code>
    /// var result = await client.RenameAsync("path/to/invoice.pdf");
    /// Console.WriteLine(result.SuggestedFilename);
    /// </code>
    /// </example>
    public async Task<RenameResult> RenameAsync(
        string filePath,
        RenameOptions? options = null,
        CancellationToken cancellationToken = default)
    {
        await using var stream = File.OpenRead(filePath);
        return await RenameAsync(stream, Path.GetFileName(filePath), options, cancellationToken)
            .ConfigureAwait(false);
    }

    /// <summary>
    /// Renames a file using AI.
    /// </summary>
    /// <param name="fileBytes">The file bytes to rename.</param>
    /// <param name="fileName">The original filename.</param>
    /// <param name="options">Optional rename options.</param>
    /// <param name="cancellationToken">Cancellation token.</param>
    /// <returns>The rename result with suggested filename.</returns>
    public Task<RenameResult> RenameAsync(
        byte[] fileBytes,
        string fileName,
        RenameOptions? options = null,
        CancellationToken cancellationToken = default)
    {
        using var stream = new MemoryStream(fileBytes);
        return RenameAsync(stream, fileName, options, cancellationToken);
    }

    /// <summary>
    /// Splits a PDF into multiple documents.
    /// Returns an AsyncJob that can be polled for completion.
    /// </summary>
    /// <param name="fileStream">The PDF file stream to split.</param>
    /// <param name="fileName">The original filename.</param>
    /// <param name="options">Optional PDF split options.</param>
    /// <param name="cancellationToken">Cancellation token.</param>
    /// <returns>An AsyncJob that can be used to poll for completion.</returns>
    /// <example>
    /// <code>
    /// using var stream = File.OpenRead("multi-page.pdf");
    /// var job = await client.PdfSplitAsync(stream, "multi-page.pdf", new PdfSplitOptions { Mode = PdfSplitMode.Auto });
    /// var result = await job.WaitAsync(status => Console.WriteLine($"Progress: {status.Progress}%"));
    /// foreach (var doc in result.Documents)
    /// {
    ///     Console.WriteLine($"{doc.Filename}: {doc.DownloadUrl}");
    /// }
    /// </code>
    /// </example>
    public async Task<AsyncJob<PdfSplitResult>> PdfSplitAsync(
        Stream fileStream,
        string fileName,
        PdfSplitOptions? options = null,
        CancellationToken cancellationToken = default)
    {
        var additionalFields = new Dictionary<string, string>();

        if (options?.Mode is not null)
        {
            additionalFields["mode"] = options.Mode.Value.ToString().ToLowerInvariant();
        }

        if (options?.PagesPerSplit is not null)
        {
            additionalFields["pagesPerSplit"] = options.PagesPerSplit.Value.ToString();
        }

        var response = await UploadFileAsync<PdfSplitJobResponse>(
            "/pdf-split",
            fileStream,
            fileName,
            additionalFields.Count > 0 ? additionalFields : null,
            cancellationToken).ConfigureAwait(false);

        return new AsyncJob<PdfSplitResult>(this, response.StatusUrl, _logger);
    }

    /// <summary>
    /// Splits a PDF into multiple documents.
    /// </summary>
    /// <param name="filePath">Path to the PDF file to split.</param>
    /// <param name="options">Optional PDF split options.</param>
    /// <param name="cancellationToken">Cancellation token.</param>
    /// <returns>An AsyncJob that can be used to poll for completion.</returns>
    public async Task<AsyncJob<PdfSplitResult>> PdfSplitAsync(
        string filePath,
        PdfSplitOptions? options = null,
        CancellationToken cancellationToken = default)
    {
        await using var stream = File.OpenRead(filePath);
        return await PdfSplitAsync(stream, Path.GetFileName(filePath), options, cancellationToken)
            .ConfigureAwait(false);
    }

    /// <summary>
    /// Splits a PDF into multiple documents.
    /// </summary>
    /// <param name="fileBytes">The PDF file bytes to split.</param>
    /// <param name="fileName">The original filename.</param>
    /// <param name="options">Optional PDF split options.</param>
    /// <param name="cancellationToken">Cancellation token.</param>
    /// <returns>An AsyncJob that can be used to poll for completion.</returns>
    public Task<AsyncJob<PdfSplitResult>> PdfSplitAsync(
        byte[] fileBytes,
        string fileName,
        PdfSplitOptions? options = null,
        CancellationToken cancellationToken = default)
    {
        using var stream = new MemoryStream(fileBytes);
        return PdfSplitAsync(stream, fileName, options, cancellationToken);
    }

    /// <summary>
    /// Extracts structured data from a document.
    /// </summary>
    /// <param name="fileStream">The document file stream.</param>
    /// <param name="fileName">The original filename.</param>
    /// <param name="options">Optional extract options.</param>
    /// <param name="cancellationToken">Cancellation token.</param>
    /// <returns>The extraction result with structured data.</returns>
    /// <example>
    /// <code>
    /// using var stream = File.OpenRead("invoice.pdf");
    /// var result = await client.ExtractAsync(stream, "invoice.pdf", new ExtractOptions
    /// {
    ///     Prompt = "Extract invoice number, date, and total amount"
    /// });
    /// Console.WriteLine(JsonSerializer.Serialize(result.Data));
    /// </code>
    /// </example>
    public Task<ExtractResult> ExtractAsync(
        Stream fileStream,
        string fileName,
        ExtractOptions? options = null,
        CancellationToken cancellationToken = default)
    {
        var additionalFields = new Dictionary<string, string>();

        if (options?.Schema is not null)
        {
            additionalFields["schema"] = JsonSerializer.Serialize(options.Schema, JsonOptions);
        }

        if (!string.IsNullOrEmpty(options?.Prompt))
        {
            additionalFields["prompt"] = options.Prompt;
        }

        return UploadFileAsync<ExtractResult>(
            "/extract",
            fileStream,
            fileName,
            additionalFields.Count > 0 ? additionalFields : null,
            cancellationToken);
    }

    /// <summary>
    /// Extracts structured data from a document.
    /// </summary>
    /// <param name="filePath">Path to the document file.</param>
    /// <param name="options">Optional extract options.</param>
    /// <param name="cancellationToken">Cancellation token.</param>
    /// <returns>The extraction result with structured data.</returns>
    public async Task<ExtractResult> ExtractAsync(
        string filePath,
        ExtractOptions? options = null,
        CancellationToken cancellationToken = default)
    {
        await using var stream = File.OpenRead(filePath);
        return await ExtractAsync(stream, Path.GetFileName(filePath), options, cancellationToken)
            .ConfigureAwait(false);
    }

    /// <summary>
    /// Extracts structured data from a document.
    /// </summary>
    /// <param name="fileBytes">The document file bytes.</param>
    /// <param name="fileName">The original filename.</param>
    /// <param name="options">Optional extract options.</param>
    /// <param name="cancellationToken">Cancellation token.</param>
    /// <returns>The extraction result with structured data.</returns>
    public Task<ExtractResult> ExtractAsync(
        byte[] fileBytes,
        string fileName,
        ExtractOptions? options = null,
        CancellationToken cancellationToken = default)
    {
        using var stream = new MemoryStream(fileBytes);
        return ExtractAsync(stream, fileName, options, cancellationToken);
    }

    /// <summary>
    /// Downloads a file from a URL (e.g., split document).
    /// </summary>
    /// <param name="url">The URL to download from.</param>
    /// <param name="cancellationToken">Cancellation token.</param>
    /// <returns>The file contents as a byte array.</returns>
    /// <example>
    /// <code>
    /// var result = await job.WaitAsync();
    /// foreach (var doc in result.Documents)
    /// {
    ///     var bytes = await client.DownloadFileAsync(doc.DownloadUrl);
    ///     await File.WriteAllBytesAsync(doc.Filename, bytes);
    /// }
    /// </code>
    /// </example>
    public async Task<byte[]> DownloadFileAsync(string url, CancellationToken cancellationToken = default)
    {
        var stopwatch = Stopwatch.StartNew();

        using var request = new HttpRequestMessage(HttpMethod.Get, url);
        request.Headers.Authorization = new AuthenticationHeaderValue("Bearer", _apiKey);

        using var response = await _httpClient.SendAsync(request, cancellationToken).ConfigureAwait(false);
        stopwatch.Stop();

        var path = GetPathFromUrl(url);
        LogRequest(HttpMethod.Get, path, (int)response.StatusCode, stopwatch.ElapsedMilliseconds);

        if (!response.IsSuccessStatusCode)
        {
            throw CreateExceptionFromResponse(response.StatusCode, response.ReasonPhrase);
        }

        return await response.Content.ReadAsByteArrayAsync(cancellationToken).ConfigureAwait(false);
    }

    /// <summary>
    /// Makes an authenticated request to the API.
    /// This method is internal and used by AsyncJob for status polling.
    /// </summary>
    internal async Task<TResponse> RequestAsync<TResponse>(
        string path,
        HttpMethod method,
        object? body = null,
        CancellationToken cancellationToken = default)
    {
        var url = BuildUrl(path);
        Exception? lastException = null;
        var attempts = 0;

        while (attempts <= _maxRetries)
        {
            var stopwatch = Stopwatch.StartNew();

            try
            {
                using var request = new HttpRequestMessage(method, url);
                request.Headers.Authorization = new AuthenticationHeaderValue("Bearer", _apiKey);

                if (body is not null)
                {
                    var json = JsonSerializer.Serialize(body, JsonOptions);
                    request.Content = new StringContent(json, Encoding.UTF8, "application/json");
                }

                using var response = await _httpClient.SendAsync(request, cancellationToken).ConfigureAwait(false);
                stopwatch.Stop();

                var requestPath = GetPathFromUrl(path);
                LogRequest(method, requestPath, (int)response.StatusCode, stopwatch.ElapsedMilliseconds);

                var responseText = await response.Content.ReadAsStringAsync(cancellationToken).ConfigureAwait(false);

                if (!response.IsSuccessStatusCode)
                {
                    throw CreateExceptionFromResponse(response.StatusCode, response.ReasonPhrase, responseText);
                }

                if (string.IsNullOrEmpty(responseText))
                {
                    return default!;
                }

                return JsonSerializer.Deserialize<TResponse>(responseText, JsonOptions)!;
            }
            catch (TaskCanceledException ex) when (!cancellationToken.IsCancellationRequested)
            {
                throw new RenamedTimeoutException("Request timed out", ex);
            }
            catch (HttpRequestException ex)
            {
                throw new NetworkException("Network request failed", ex);
            }
            catch (RenamedExceptionBase ex) when (ex.StatusCode >= 400 && ex.StatusCode < 500)
            {
                // Don't retry client errors
                throw;
            }
            catch (Exception ex)
            {
                lastException = ex;
                attempts++;

                if (attempts <= _maxRetries)
                {
                    var delayMs = (int)(Math.Pow(2, attempts) * 100);
                    LogRetry(attempts, _maxRetries, delayMs);
                    await Task.Delay(delayMs, cancellationToken).ConfigureAwait(false);
                }
            }
        }

        throw lastException ?? new NetworkException();
    }

    private async Task<TResponse> UploadFileAsync<TResponse>(
        string path,
        Stream fileStream,
        string fileName,
        IDictionary<string, string>? additionalFields = null,
        CancellationToken cancellationToken = default)
    {
        var url = BuildUrl(path);
        var stopwatch = Stopwatch.StartNew();

        // Get file size for logging
        var fileSize = GetStreamLength(fileStream);
        LogUpload(fileName, fileSize);

        using var content = new MultipartFormDataContent();
        using var streamContent = new StreamContent(fileStream);

        var mimeType = GetMimeType(fileName);
        streamContent.Headers.ContentType = new MediaTypeHeaderValue(mimeType);
        content.Add(streamContent, "file", fileName);

        if (additionalFields is not null)
        {
            foreach (var (key, value) in additionalFields)
            {
                content.Add(new StringContent(value), key);
            }
        }

        using var request = new HttpRequestMessage(HttpMethod.Post, url) { Content = content };
        request.Headers.Authorization = new AuthenticationHeaderValue("Bearer", _apiKey);

        try
        {
            using var response = await _httpClient.SendAsync(request, cancellationToken).ConfigureAwait(false);
            stopwatch.Stop();

            LogRequest(HttpMethod.Post, path, (int)response.StatusCode, stopwatch.ElapsedMilliseconds);

            var responseText = await response.Content.ReadAsStringAsync(cancellationToken).ConfigureAwait(false);

            if (!response.IsSuccessStatusCode)
            {
                throw CreateExceptionFromResponse(response.StatusCode, response.ReasonPhrase, responseText);
            }

            if (string.IsNullOrEmpty(responseText))
            {
                return default!;
            }

            return JsonSerializer.Deserialize<TResponse>(responseText, JsonOptions)!;
        }
        catch (TaskCanceledException ex) when (!cancellationToken.IsCancellationRequested)
        {
            throw new RenamedTimeoutException("Request timed out", ex);
        }
        catch (HttpRequestException ex)
        {
            throw new NetworkException("Network request failed", ex);
        }
    }

    private string BuildUrl(string path)
    {
        if (path.StartsWith("http://", StringComparison.OrdinalIgnoreCase) ||
            path.StartsWith("https://", StringComparison.OrdinalIgnoreCase))
        {
            return path;
        }

        return path.StartsWith('/')
            ? $"{_baseUrl}{path}"
            : $"{_baseUrl}/{path}";
    }

    private static string GetMimeType(string fileName)
    {
        var extension = Path.GetExtension(fileName);
        return MimeTypes.TryGetValue(extension, out var mimeType)
            ? mimeType
            : "application/octet-stream";
    }

    private static RenamedExceptionBase CreateExceptionFromResponse(
        HttpStatusCode statusCode,
        string? reasonPhrase,
        string? responseText = null)
    {
        var message = reasonPhrase ?? "Request failed";

        if (!string.IsNullOrEmpty(responseText))
        {
            try
            {
                var errorResponse = JsonSerializer.Deserialize<ErrorResponse>(responseText, JsonOptions);
                if (!string.IsNullOrEmpty(errorResponse?.Error))
                {
                    message = errorResponse.Error;
                }
            }
            catch
            {
                // Ignore JSON parse errors
            }
        }

        return statusCode switch
        {
            HttpStatusCode.Unauthorized => new AuthenticationException(message),
            HttpStatusCode.PaymentRequired => new InsufficientCreditsException(message),
            HttpStatusCode.BadRequest or HttpStatusCode.UnprocessableEntity => new ValidationException(message),
            HttpStatusCode.TooManyRequests => new RateLimitException(message),
            _ => new RenamedExceptionBase(message, "API_ERROR", (int)statusCode)
        };
    }

    /// <summary>
    /// Logs a debug message if logging is enabled.
    /// Internal for use by AsyncJob.
    /// </summary>
    internal void LogDebug(string message)
    {
        _logger?.Log(message);
    }

    private void LogRequest(HttpMethod method, string path, int statusCode, long elapsedMs)
    {
        _logger?.Log($"[Renamed] {method.Method} {path} -> {statusCode} ({elapsedMs}ms)");
    }

    private void LogRetry(int attempt, int maxRetries, int delayMs)
    {
        _logger?.Log($"[Renamed] Retry attempt {attempt}/{maxRetries}, waiting {delayMs}ms");
    }

    private void LogUpload(string fileName, long? fileSize)
    {
        if (_logger is null)
        {
            return;
        }

        var sizeStr = fileSize.HasValue ? FormatFileSize(fileSize.Value) : "unknown size";
        _logger.Log($"[Renamed] Upload: {fileName} ({sizeStr})");
    }

    private static string FormatFileSize(long bytes)
    {
        return bytes switch
        {
            < 1024 => $"{bytes} B",
            < 1024 * 1024 => $"{bytes / 1024.0:F1} KB",
            < 1024 * 1024 * 1024 => $"{bytes / (1024.0 * 1024.0):F1} MB",
            _ => $"{bytes / (1024.0 * 1024.0 * 1024.0):F1} GB"
        };
    }

    private static long? GetStreamLength(Stream stream)
    {
        try
        {
            if (stream.CanSeek)
            {
                return stream.Length;
            }
        }
        catch
        {
            // Some streams may throw even when CanSeek is true
        }

        return null;
    }

    private static string GetPathFromUrl(string urlOrPath)
    {
        // If it's already a path (starts with /), return as-is
        if (urlOrPath.StartsWith('/'))
        {
            return urlOrPath;
        }

        // Try to parse as URL and extract path
        if (Uri.TryCreate(urlOrPath, UriKind.Absolute, out var uri))
        {
            return uri.AbsolutePath;
        }

        return urlOrPath;
    }

    /// <summary>
    /// Masks the API key for safe logging (rt_...xxxx format).
    /// </summary>
    internal static string MaskApiKey(string apiKey)
    {
        if (string.IsNullOrEmpty(apiKey))
        {
            return "***";
        }

        // Show first 3 chars and last 4 chars: rt_...xxxx
        if (apiKey.Length <= 7)
        {
            return "***";
        }

        var prefix = apiKey[..3];
        var suffix = apiKey[^4..];
        return $"{prefix}...{suffix}";
    }

    /// <summary>
    /// Disposes the client and releases resources.
    /// </summary>
    public void Dispose()
    {
        if (_disposed)
        {
            return;
        }

        if (_ownsHttpClient)
        {
            _httpClient.Dispose();
        }

        _disposed = true;
    }

    private sealed class PdfSplitJobResponse
    {
        [JsonPropertyName("statusUrl")]
        public required string StatusUrl { get; init; }
    }

    private sealed class ErrorResponse
    {
        [JsonPropertyName("error")]
        public string? Error { get; init; }

        [JsonPropertyName("retryAfter")]
        public int? RetryAfter { get; init; }
    }
}
