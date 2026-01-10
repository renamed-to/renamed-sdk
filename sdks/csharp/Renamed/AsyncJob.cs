using Renamed.Sdk.Exceptions;
using Renamed.Sdk.Models;

namespace Renamed.Sdk;

/// <summary>
/// Async job handle for long-running operations like PDF split.
/// Provides methods to poll for status and wait for completion.
/// </summary>
/// <typeparam name="TResult">The type of result expected when the job completes.</typeparam>
public sealed class AsyncJob<TResult> where TResult : class
{
    private readonly RenamedClient _client;
    private readonly string _statusUrl;
    private readonly TimeSpan _pollInterval;
    private readonly int _maxAttempts;

    internal AsyncJob(
        RenamedClient client,
        string statusUrl,
        TimeSpan? pollInterval = null,
        int? maxAttempts = null)
    {
        _client = client ?? throw new ArgumentNullException(nameof(client));
        _statusUrl = statusUrl ?? throw new ArgumentNullException(nameof(statusUrl));
        _pollInterval = pollInterval ?? TimeSpan.FromSeconds(2);
        _maxAttempts = maxAttempts ?? 150; // 5 minutes at 2s intervals
    }

    /// <summary>
    /// Gets the current job status.
    /// </summary>
    /// <param name="cancellationToken">Cancellation token.</param>
    /// <returns>The current job status response.</returns>
    public Task<JobStatusResponse> GetStatusAsync(CancellationToken cancellationToken = default)
    {
        return _client.RequestAsync<JobStatusResponse>(_statusUrl, HttpMethod.Get, cancellationToken: cancellationToken);
    }

    /// <summary>
    /// Waits for the job to complete, polling at regular intervals.
    /// </summary>
    /// <param name="onProgress">Optional callback invoked when status is updated.</param>
    /// <param name="cancellationToken">Cancellation token.</param>
    /// <returns>The job result when completed.</returns>
    /// <exception cref="JobException">Thrown when the job fails or times out.</exception>
    public async Task<TResult> WaitAsync(
        Action<JobStatusResponse>? onProgress = null,
        CancellationToken cancellationToken = default)
    {
        var attempts = 0;

        while (attempts < _maxAttempts)
        {
            cancellationToken.ThrowIfCancellationRequested();

            var status = await GetStatusAsync(cancellationToken).ConfigureAwait(false);

            onProgress?.Invoke(status);

            if (status.Status == JobStatus.Completed && status.Result is not null)
            {
                // The result is stored as PdfSplitResult in JobStatusResponse
                // We need to return it as TResult
                if (status.Result is TResult typedResult)
                {
                    return typedResult;
                }

                // This shouldn't happen if types are used correctly
                throw new JobException("Job completed but result type mismatch", status.JobId);
            }

            if (status.Status == JobStatus.Failed)
            {
                throw new JobException(status.Error ?? "Job failed", status.JobId);
            }

            attempts++;
            await Task.Delay(_pollInterval, cancellationToken).ConfigureAwait(false);
        }

        throw new JobException("Job polling timeout exceeded");
    }
}
