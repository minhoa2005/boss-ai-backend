package ai.content.auto.service;

import ai.content.auto.dtos.ContentGenerateRequest;
import ai.content.auto.dtos.ContentGenerateResponse;
import ai.content.auto.dtos.GenerationJobDto;
import ai.content.auto.entity.GenerationJob;
import ai.content.auto.entity.GenerationJob.JobStatus;

import ai.content.auto.mapper.GenerationJobMapper;
import ai.content.auto.repository.GenerationJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Service for processing content generation jobs from the queue
 * Handles asynchronous job processing with retry logic and error handling
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ContentGenerationQueueService {

    private final GenerationJobRepository jobRepository;
    private final GenerationJobMapper jobMapper;
    private final ContentService contentService;
    private final QueueManagementService queueManagementService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final WebSocketService webSocketService;

    @Value("${app.queue.processing-batch-size:10}")
    private int processingBatchSize;

    @Value("${app.queue.retry-delay-minutes:5}")
    private int retryDelayMinutes;

    @Value("${app.queue.max-processing-time-minutes:10}")
    private int maxProcessingTimeMinutes;

    private static final String PROCESSING_LOCK_KEY = "queue:processing:lock";
    private static final String JOB_PROCESSING_KEY = "queue:job:processing:";

    /**
     * Process jobs from the queue (scheduled every 5 seconds for testing)
     */
    @Scheduled(fixedDelay = 5000) // 5 seconds for faster testing
    public void processQueuedJobs() {
        try {
            // Acquire distributed lock to prevent multiple instances from processing
            if (!acquireProcessingLock()) {
                log.debug("Another instance is processing jobs, skipping");
                return;
            }

            log.info("Starting queue processing cycle");

            // Get next batch of jobs to process
            List<GenerationJobDto> jobs = queueManagementService.getNextJobsToProcess(processingBatchSize);

            if (jobs.isEmpty()) {
                log.info("No jobs in queue to process");
                return;
            }

            log.info("Processing {} jobs from queue", jobs.size());

            // Process jobs asynchronously
            for (GenerationJobDto job : jobs) {
                processJobAsync(job);
            }

        } catch (Exception e) {
            log.error("Error during queue processing cycle", e);
        } finally {
            releaseProcessingLock();
        }
    }

    /**
     * Process retry jobs (scheduled every 5 minutes)
     */
    @Scheduled(fixedDelay = 300000) // 5 minutes
    public void processRetryJobs() {
        try {
            log.debug("Processing retry jobs");

            Instant now = Instant.now();
            List<GenerationJob> retryJobs = jobRepository.findJobsReadyForRetry(
                    JobStatus.FAILED, now, PageRequest.of(0, processingBatchSize));

            if (retryJobs.isEmpty()) {
                log.debug("No retry jobs to process");
                return;
            }

            log.info("Processing {} retry jobs", retryJobs.size());

            for (GenerationJob job : retryJobs) {
                // Reset job to queued status for retry
                updateJobToQueued(job.getJobId());

                // Process the job
                GenerationJobDto jobDto = jobMapper.toDto(job);
                processJobAsync(jobDto);
            }

        } catch (Exception e) {
            log.error("Error during retry job processing", e);
        }
    }

    /**
     * Clean up stale processing jobs (scheduled every 10 minutes)
     */
    @Scheduled(fixedDelay = 600000) // 10 minutes
    public void cleanupStaleJobs() {
        try {
            log.debug("Cleaning up stale processing jobs");

            Instant cutoffTime = Instant.now().minus(maxProcessingTimeMinutes, ChronoUnit.MINUTES);

            // Find jobs that have been processing too long
            List<GenerationJob> staleJobs = jobRepository.findAll().stream()
                    .filter(job -> job.getStatus() == JobStatus.PROCESSING)
                    .filter(job -> job.getStartedAt() != null && job.getStartedAt().isBefore(cutoffTime))
                    .toList();

            if (staleJobs.isEmpty()) {
                log.debug("No stale jobs found");
                return;
            }

            log.warn("Found {} stale processing jobs, marking as failed", staleJobs.size());

            for (GenerationJob job : staleJobs) {
                updateJobToFailed(job.getJobId(), "Job processing timeout", null);

                // Remove processing lock
                removeJobProcessingLock(job.getJobId());
            }

        } catch (Exception e) {
            log.error("Error during stale job cleanup", e);
        }
    }

    /**
     * Process a single job asynchronously
     */
    @Async("queueTaskExecutor")
    public CompletableFuture<Void> processJobAsync(GenerationJobDto jobDto) {
        String jobId = jobDto.getJobId();
        Long userId = jobDto.getUserId();

        try {
            // Acquire job processing lock
            if (!acquireJobProcessingLock(jobId)) {
                log.debug("Job {} is already being processed", jobId);
                return CompletableFuture.completedFuture(null);
            }

            log.info("Starting processing job: {}", jobId);

            // Send job started notification via WebSocket
            webSocketService.sendJobStatusUpdate(jobId, "PROCESSING", Map.of(
                    "message", "Content generation started",
                    "progress", 10));

            // Update job status to processing
            if (!updateJobToProcessing(jobId)) {
                log.warn("Failed to update job {} to processing status", jobId);
                return CompletableFuture.completedFuture(null);
            }

            Instant startTime = Instant.now();

            try {
                // Convert job parameters to content generation request
                ContentGenerateRequest request = convertToContentRequest(jobDto);

                // Send progress update
                webSocketService.sendJobStatusUpdate(jobId, "PROCESSING", Map.of(
                        "message", "Generating content with AI...",
                        "progress", 30));

                // Generate content using async method with explicit user context
                // This avoids security context issues in scheduled tasks
                ContentGenerateResponse response = contentService.generateContentForUser(request, userId);

                // Send progress update
                webSocketService.sendJobStatusUpdate(jobId, "PROCESSING", Map.of(
                        "message", "Finalizing content...",
                        "progress", 80));

                // Calculate processing time
                long processingTime = Instant.now().toEpochMilli() - startTime.toEpochMilli();

                // Update job with successful result
                updateJobToCompleted(jobId, response, processingTime);

                // Send job completion notification via WebSocket
                webSocketService.sendJobCompletionNotification(userId, jobId, true, Map.of(
                        "contentId", response.getContentId(),
                        "title", response.getTitle(),
                        "generatedContent", response.getGeneratedContent(),
                        "wordCount", response.getWordCount(),
                        "processingTime", processingTime));

                log.info("Job {} completed successfully in {}ms", jobId, processingTime);

            } catch (Exception e) {
                log.error("Job {} failed during processing", jobId, e);

                // Send job error notification via WebSocket
                webSocketService.sendJobErrorNotification(userId, jobId,
                        "Content generation failed: " + e.getMessage(), Map.of(
                                "error", e.getMessage(),
                                "jobId", jobId));

                // Update job with failure
                Instant nextRetryAt = calculateNextRetryTime(jobDto.getRetryCount());
                updateJobToFailed(jobId, e.getMessage(), nextRetryAt);
            }

        } catch (Exception e) {
            log.error("Unexpected error processing job: {}", jobId, e);

            // Send error notification if we have user ID
            if (userId != null) {
                webSocketService.sendJobErrorNotification(userId, jobId,
                        "Unexpected error: " + e.getMessage(), Map.of(
                                "error", e.getMessage(),
                                "jobId", jobId));
            }
        } finally {
            // Always release the job processing lock
            removeJobProcessingLock(jobId);
        }

        return CompletableFuture.completedFuture(null);
    }

    // Private helper methods

    private boolean acquireProcessingLock() {
        try {
            Boolean acquired = redisTemplate.opsForValue()
                    .setIfAbsent(PROCESSING_LOCK_KEY, "locked", 60, TimeUnit.SECONDS);
            return Boolean.TRUE.equals(acquired);
        } catch (Exception e) {
            log.warn("Failed to acquire processing lock", e);
            return false;
        }
    }

    private void releaseProcessingLock() {
        try {
            redisTemplate.delete(PROCESSING_LOCK_KEY);
        } catch (Exception e) {
            log.warn("Failed to release processing lock", e);
        }
    }

    private boolean acquireJobProcessingLock(String jobId) {
        try {
            String lockKey = JOB_PROCESSING_KEY + jobId;
            Boolean acquired = redisTemplate.opsForValue()
                    .setIfAbsent(lockKey, "processing", maxProcessingTimeMinutes, TimeUnit.MINUTES);
            return Boolean.TRUE.equals(acquired);
        } catch (Exception e) {
            log.warn("Failed to acquire job processing lock for job: {}", jobId, e);
            return false;
        }
    }

    private void removeJobProcessingLock(String jobId) {
        try {
            String lockKey = JOB_PROCESSING_KEY + jobId;
            redisTemplate.delete(lockKey);
        } catch (Exception e) {
            log.warn("Failed to remove job processing lock for job: {}", jobId, e);
        }
    }

    private boolean updateJobToProcessing(String jobId) {
        try {
            Instant now = Instant.now();
            // Let Spring Data JPA handle the transaction for this simple update operation
            int updated = jobRepository.updateJobToProcessing(
                    jobId, JobStatus.PROCESSING, JobStatus.QUEUED, now);
            return updated > 0;
        } catch (Exception e) {
            log.error("Failed to update job {} to processing", jobId, e);
            return false;
        }
    }

    private void updateJobToQueued(String jobId) {
        try {
            Instant now = Instant.now();
            // Let Spring Data JPA handle the transaction for this simple update operation
            jobRepository.updateJobStatus(jobId, JobStatus.QUEUED, now);
        } catch (Exception e) {
            log.error("Failed to update job {} to queued", jobId, e);
        }
    }

    private void updateJobToCompleted(String jobId, ContentGenerateResponse response, long processingTime) {
        try {
            // Let Spring Data JPA handle the transaction for this simple update operation
            jobRepository.updateJobToCompleted(
                    jobId,
                    JobStatus.COMPLETED,
                    response.getGeneratedContent(),
                    processingTime,
                    response.getTokensUsed(),
                    response.getGenerationCost(),
                    response.getAiProvider(),
                    response.getAiModel(),
                    response.getOpenaiResponseId(),
                    Instant.now());
        } catch (Exception e) {
            log.error("Failed to update job {} to completed", jobId, e);
        }
    }

    private void updateJobToFailed(String jobId, String errorMessage, Instant nextRetryAt) {
        try {
            // Let Spring Data JPA handle the transaction for this simple update operation
            jobRepository.updateJobToFailed(
                    jobId,
                    JobStatus.FAILED,
                    errorMessage,
                    nextRetryAt,
                    Instant.now());
        } catch (Exception e) {
            log.error("Failed to update job {} to failed", jobId, e);
        }
    }

    private ContentGenerateRequest convertToContentRequest(GenerationJobDto jobDto) {
        Map<String, Object> params = jobDto.getRequestParams();

        // Convert job parameters back to ContentGenerateRequest
        // This is a simplified conversion - in production, you'd have more robust
        // parameter mapping
        ContentGenerateRequest request = new ContentGenerateRequest();

        if (params.containsKey("content")) {
            request.setContent((String) params.get("content"));
        }
        if (params.containsKey("industry")) {
            request.setIndustry((String) params.get("industry"));
        }
        if (params.containsKey("contentType")) {
            request.setContentType((String) params.get("contentType"));
        }
        if (params.containsKey("targetAudience")) {
            request.setTargetAudience((String) params.get("targetAudience"));
        }
        if (params.containsKey("tone")) {
            request.setTone((String) params.get("tone"));
        }
        if (params.containsKey("language")) {
            request.setLanguage((String) params.get("language"));
        }
        if (params.containsKey("title")) {
            request.setTitle((String) params.get("title"));
        }

        return request;
    }

    private Instant calculateNextRetryTime(Integer currentRetryCount) {
        // Exponential backoff: 5 minutes * 2^retryCount
        int delayMinutes = retryDelayMinutes * (int) Math.pow(2, currentRetryCount != null ? currentRetryCount : 0);
        return Instant.now().plus(delayMinutes, ChronoUnit.MINUTES);
    }
}