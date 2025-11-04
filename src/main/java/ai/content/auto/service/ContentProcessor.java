package ai.content.auto.service;

import ai.content.auto.dtos.ContentGenerateRequest;
import ai.content.auto.dtos.ContentGenerateResponse;
import ai.content.auto.dtos.GenerationJobDto;
import ai.content.auto.entity.GenerationJob;
import ai.content.auto.entity.GenerationJob.JobStatus;
import ai.content.auto.entity.User;
import ai.content.auto.exception.BusinessException;
import ai.content.auto.mapper.GenerationJobMapper;
import ai.content.auto.repository.GenerationJobRepository;
import ai.content.auto.repository.UserRepository;
import ai.content.auto.service.ai.AIProviderManager;
import ai.content.auto.websocket.JobStatusWebSocketHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service for processing queued content generation jobs asynchronously
 * Handles job processing with configurable concurrency, retry logic, and
 * timeout handling
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ContentProcessor {

    private final GenerationJobRepository jobRepository;
    private final UserRepository userRepository;
    private final GenerationJobMapper jobMapper;
    private final AIProviderManager aiProviderManager;
    private final JobStatusWebSocketHandler webSocketHandler;
    private final ObjectMapper objectMapper;

    @Value("${app.processor.max-concurrent-jobs:10}")
    private int maxConcurrentJobs;

    @Value("${app.processor.job-timeout-seconds:120}")
    private int jobTimeoutSeconds;

    @Value("${app.processor.max-retry-attempts:3}")
    private int maxRetryAttempts;

    @Value("${app.processor.retry-base-delay-ms:1000}")
    private long retryBaseDelayMs;

    @Value("${app.processor.processing-batch-size:20}")
    private int processingBatchSize;

    // Semaphore to control concurrent job processing
    private final Semaphore processingSlots = new Semaphore(maxConcurrentJobs);

    // Metrics tracking
    private final AtomicInteger activeJobs = new AtomicInteger(0);
    private final AtomicInteger processedJobs = new AtomicInteger(0);
    private final AtomicInteger failedJobs = new AtomicInteger(0);

    /**
     * Process a single job asynchronously
     */
    @Async("taskExecutor")
    public CompletableFuture<Void> processJob(String jobId) {
        return CompletableFuture.runAsync(() -> {
            try {
                // Acquire processing slot
                if (!processingSlots.tryAcquire(1, TimeUnit.SECONDS)) {
                    log.warn("No processing slots available for job: {}", jobId);
                    return;
                }

                try {
                    activeJobs.incrementAndGet();
                    processJobInternal(jobId);
                } finally {
                    processingSlots.release();
                    activeJobs.decrementAndGet();
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Job processing interrupted for job: {}", jobId);
            } catch (Exception e) {
                log.error("Unexpected error processing job: {}", jobId, e);
            }
        });
    }

    /**
     * Scheduled method to process queued jobs
     * Runs every 5 seconds to check for new jobs
     */
    @Scheduled(fixedDelay = 5000) // 5 seconds
    public void processQueuedJobs() {
        try {
            // Check if we have available processing slots
            int availableSlots = processingSlots.availablePermits();
            if (availableSlots <= 0) {
                log.debug("No available processing slots, skipping queue check");
                return;
            }

            // Get next jobs to process
            Pageable pageable = PageRequest.of(0, Math.min(availableSlots, processingBatchSize));
            List<GenerationJob> queuedJobs = jobRepository.findNextJobsToProcess(JobStatus.QUEUED, pageable);

            if (!queuedJobs.isEmpty()) {
                log.info("Found {} queued jobs to process", queuedJobs.size());

                for (GenerationJob job : queuedJobs) {
                    // Check if job has expired
                    if (job.isExpired()) {
                        markJobExpired(job);
                        continue;
                    }

                    // Start processing job asynchronously
                    processJob(job.getJobId());
                }
            }

        } catch (Exception e) {
            log.error("Error in scheduled job processing", e);
        }
    }

    /**
     * Scheduled method to handle job timeouts
     * Runs every minute to check for timed out jobs
     */
    @Scheduled(fixedDelay = 60000) // 1 minute
    public void handleJobTimeouts() {
        try {
            Instant timeoutThreshold = Instant.now().minus(jobTimeoutSeconds, ChronoUnit.SECONDS);
            List<GenerationJob> timedOutJobs = jobRepository.findTimedOutJobs(JobStatus.PROCESSING, timeoutThreshold);

            for (GenerationJob job : timedOutJobs) {
                log.warn("Job timed out: {} (started at: {})", job.getJobId(), job.getStartedAt());

                if (job.canRetry()) {
                    scheduleJobRetry(job, "Job timed out after " + jobTimeoutSeconds + " seconds");
                } else {
                    markJobFailed(job, "Job timed out and exceeded maximum retry attempts");
                }
            }

        } catch (Exception e) {
            log.error("Error handling job timeouts", e);
        }
    }

    /**
     * Get processing statistics
     */
    public Map<String, Object> getProcessingStatistics() {
        return Map.of(
                "active_jobs", activeJobs.get(),
                "processed_jobs", processedJobs.get(),
                "failed_jobs", failedJobs.get(),
                "available_slots", processingSlots.availablePermits(),
                "max_concurrent_jobs", maxConcurrentJobs,
                "job_timeout_seconds", jobTimeoutSeconds,
                "max_retry_attempts", maxRetryAttempts);
    }

    private void processJobInternal(String jobId) {
        GenerationJob job = null;

        try {
            // Load job from database
            job = jobRepository.findByJobId(jobId)
                    .orElseThrow(() -> new BusinessException("Job not found: " + jobId));

            // Check if job is still in queued state
            if (job.getStatus() != JobStatus.QUEUED) {
                log.warn("Job {} is not in queued state: {}", jobId, job.getStatus());
                return;
            }

            log.info("Starting processing for job: {} (user: {})", jobId, job.getUserId());

            // Mark job as processing
            markJobProcessing(job);

            // Send WebSocket update
            sendJobStatusUpdate(job, "processing", "Job processing started", 10);

            // Load user
            Long userId = job.getUserId();
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new BusinessException("User not found: " + userId));

            // Parse request parameters
            ContentGenerateRequest request = parseRequestParams(job.getRequestParams());

            // Send progress update
            sendJobStatusUpdate(job, "processing", "Generating content with AI provider", 30);

            // Generate content using AI provider manager
            ContentGenerateResponse response = aiProviderManager.generateContent(request, user);

            // Send progress update
            sendJobStatusUpdate(job, "processing", "Content generated, finalizing", 80);

            // Mark job as completed
            markJobCompleted(job, response, request);

            // Send completion notification
            sendJobCompletionNotification(job, response);

            processedJobs.incrementAndGet();
            log.info("Job completed successfully: {} (processing time: {}ms)",
                    jobId, job.getProcessingDuration());

        } catch (Exception e) {
            log.error("Error processing job: {}", jobId, e);

            if (job != null) {
                if (job.canRetry()) {
                    scheduleJobRetry(job, e.getMessage());
                } else {
                    markJobFailed(job, e.getMessage());
                }
            }

            failedJobs.incrementAndGet();
        }
    }

    @Transactional
    private void markJobProcessing(GenerationJob job) {
        job.setStatus(JobStatus.PROCESSING);
        job.setStartedAt(Instant.now());
        job.setUpdatedAt(Instant.now());
        jobRepository.save(job);
    }

    @Transactional
    private void markJobCompleted(GenerationJob job, ContentGenerateResponse response, ContentGenerateRequest request) {
        Instant now = Instant.now();

        job.setStatus(JobStatus.COMPLETED);
        job.setCompletedAt(now);
        job.setUpdatedAt(now);
        job.setResultContent(response.getGeneratedContent());
        // Set AI provider info from the job metadata or default values
        job.setAiProvider("OpenAI"); // This would be set by the AI provider manager
        job.setAiModel("gpt-3.5-turbo"); // This would be set by the AI provider manager
        job.setTokensUsed(response.getTokensUsed());
        job.setGenerationCost(response.getGenerationCost());

        // Calculate processing time
        if (job.getStartedAt() != null) {
            job.setProcessingTimeMs(now.toEpochMilli() - job.getStartedAt().toEpochMilli());
        }

        jobRepository.save(job);
    }

    @Transactional
    private void markJobFailed(GenerationJob job, String errorMessage) {
        Instant now = Instant.now();

        job.setStatus(JobStatus.FAILED);
        job.setCompletedAt(now);
        job.setUpdatedAt(now);
        job.setErrorMessage(errorMessage);

        // Calculate processing time if started
        if (job.getStartedAt() != null) {
            job.setProcessingTimeMs(now.toEpochMilli() - job.getStartedAt().toEpochMilli());
        }

        jobRepository.save(job);

        // Send failure notification
        sendJobFailureNotification(job, errorMessage);
    }

    @Transactional
    private void markJobExpired(GenerationJob job) {
        job.setStatus(JobStatus.EXPIRED);
        job.setCompletedAt(Instant.now());
        job.setUpdatedAt(Instant.now());
        job.setErrorMessage("Job expired before processing");
        jobRepository.save(job);

        log.info("Job expired: {}", job.getJobId());
    }

    @Transactional
    private void scheduleJobRetry(GenerationJob job, String errorMessage) {
        job.setRetryCount(job.getRetryCount() + 1);
        job.setStatus(JobStatus.QUEUED);
        job.setUpdatedAt(Instant.now());
        job.setErrorMessage(errorMessage);

        // Calculate exponential backoff delay
        long delayMs = calculateRetryDelay(job.getRetryCount());
        job.setNextRetryAt(Instant.now().plusMillis(delayMs));

        jobRepository.save(job);

        log.info("Job scheduled for retry: {} (attempt {} of {}, delay: {}ms)",
                job.getJobId(), job.getRetryCount(), job.getMaxRetries(), delayMs);

        // Send retry notification
        sendJobRetryNotification(job, delayMs);
    }

    private long calculateRetryDelay(int retryCount) {
        // Exponential backoff: base_delay * 2^(retry_count - 1)
        // With jitter to avoid thundering herd
        long baseDelay = retryBaseDelayMs * (1L << (retryCount - 1));
        long maxDelay = 300000; // Max 5 minutes
        long delay = Math.min(baseDelay, maxDelay);

        // Add jitter (±25%)
        double jitter = 0.75 + (Math.random() * 0.5); // 0.75 to 1.25
        return (long) (delay * jitter);
    }

    private ContentGenerateRequest parseRequestParams(Map<String, Object> requestParams) {
        try {
            String json = objectMapper.writeValueAsString(requestParams);
            return objectMapper.readValue(json, ContentGenerateRequest.class);
        } catch (Exception e) {
            throw new BusinessException("Failed to parse request parameters: " + e.getMessage());
        }
    }

    private void sendJobStatusUpdate(GenerationJob job, String status, String message, int progress) {
        try {
            Map<String, Object> statusUpdate = Map.of(
                    "status", status,
                    "message", message,
                    "progress", progress,
                    "timestamp", Instant.now().toEpochMilli());

            webSocketHandler.sendJobStatusUpdate(job.getJobId(), statusUpdate);

        } catch (Exception e) {
            log.error("Failed to send job status update for job: {}", job.getJobId(), e);
        }
    }

    private void sendJobCompletionNotification(GenerationJob job, ContentGenerateResponse response) {
        try {
            Map<String, Object> result = Map.of(
                    "jobId", job.getJobId(),
                    "status", "completed",
                    "content", response.getGeneratedContent(),
                    "title", response.getTitle() != null ? response.getTitle() : "",
                    "aiProvider", job.getAiProvider() != null ? job.getAiProvider() : "OpenAI",
                    "aiModel", job.getAiModel() != null ? job.getAiModel() : "gpt-3.5-turbo",
                    "tokensUsed", response.getTokensUsed() != null ? response.getTokensUsed() : 0,
                    "generationCost",
                    response.getGenerationCost() != null ? response.getGenerationCost() : BigDecimal.ZERO,
                    "processingTimeMs", job.getProcessingTimeMs() != null ? job.getProcessingTimeMs() : 0,
                    "completedAt", job.getCompletedAt().toEpochMilli());

            webSocketHandler.sendJobCompletionNotification(job.getUserId(), job.getJobId(), result);

        } catch (Exception e) {
            log.error("Failed to send job completion notification for job: {}", job.getJobId(), e);
        }
    }

    private void sendJobFailureNotification(GenerationJob job, String errorMessage) {
        try {
            Map<String, Object> result = Map.of(
                    "jobId", job.getJobId(),
                    "status", "failed",
                    "errorMessage", errorMessage,
                    "retryCount", job.getRetryCount(),
                    "maxRetries", job.getMaxRetries(),
                    "canRetry", job.canRetry(),
                    "failedAt", job.getCompletedAt().toEpochMilli());

            webSocketHandler.sendJobCompletionNotification(job.getUserId(), job.getJobId(), result);

        } catch (Exception e) {
            log.error("Failed to send job failure notification for job: {}", job.getJobId(), e);
        }
    }

    private void sendJobRetryNotification(GenerationJob job, long delayMs) {
        try {
            Map<String, Object> statusUpdate = Map.of(
                    "status", "retry_scheduled",
                    "message", "Job will be retried in " + (delayMs / 1000) + " seconds",
                    "retryCount", job.getRetryCount(),
                    "maxRetries", job.getMaxRetries(),
                    "nextRetryAt", job.getNextRetryAt().toEpochMilli(),
                    "timestamp", Instant.now().toEpochMilli());

            webSocketHandler.sendJobStatusUpdate(job.getJobId(), statusUpdate);

        } catch (Exception e) {
            log.error("Failed to send job retry notification for job: {}", job.getJobId(), e);
        }
    }
}