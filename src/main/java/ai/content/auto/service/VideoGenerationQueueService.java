package ai.content.auto.service;

import ai.content.auto.config.VideoResourceConfig;
import ai.content.auto.dto.request.CreateVideoJobRequest;
import ai.content.auto.dto.request.BatchVideoJobRequest;
import ai.content.auto.dto.response.VideoJobDto;
import ai.content.auto.dto.response.BatchVideoJobResponse;
import ai.content.auto.dto.response.BatchProgressDto;
import ai.content.auto.entity.VideoGenerationJob;
import ai.content.auto.entity.VideoGenerationJob.JobStatus;
import ai.content.auto.entity.VideoGenerationJob.JobPriority;
import ai.content.auto.entity.VideoTemplate;
import ai.content.auto.entity.ContentGeneration;
import ai.content.auto.entity.User;
import ai.content.auto.exception.BusinessException;
import ai.content.auto.exception.NotFoundException;
import ai.content.auto.mapper.VideoJobMapper;
import ai.content.auto.repository.VideoGenerationJobRepository;
import ai.content.auto.repository.VideoTemplateRepository;
import ai.content.auto.repository.ContentGenerationRepository;
import ai.content.auto.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for managing video generation queue and batch processing.
 * Handles job creation, scheduling, and asynchronous processing.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VideoGenerationQueueService {

    private final VideoGenerationJobRepository jobRepository;
    private final VideoTemplateRepository templateRepository;
    private final ContentGenerationRepository contentRepository;
    private final VideoJobMapper jobMapper;
    private final SecurityUtil securityUtil;
    private final WebSocketService webSocketService;
    private final N8nService n8nService;
    private final VideoResourceConfig resourceConfig;
    private final VideoResourceMonitorService resourceMonitor;

    /**
     * Create a single video generation job
     */
    public VideoJobDto createVideoJob(CreateVideoJobRequest request) {
        try {
            User currentUser = securityUtil.getCurrentUser();
            log.info("Creating video generation job for user: {}", currentUser.getId());

            // Check if system can accept more jobs
            if (!resourceMonitor.canAcceptMoreJobs()) {
                throw new BusinessException("System is at capacity. Please try again later.");
            }

            // Check if user can submit more jobs
            if (!resourceMonitor.canUserSubmitMoreJobs(currentUser.getId())) {
                throw new BusinessException(
                        "You have reached the maximum number of concurrent video jobs (" +
                                resourceConfig.getProcessing().getMaxUserConcurrentJobs() + ")");
            }

            // Validate template
            VideoTemplate template = templateRepository.findById(request.getTemplateId())
                    .orElseThrow(() -> new NotFoundException("Video template not found: " + request.getTemplateId()));

            // Validate content if provided
            ContentGeneration content = null;
            if (request.getContentId() != null) {
                content = contentRepository.findById(request.getContentId())
                        .orElseThrow(() -> new NotFoundException("Content not found: " + request.getContentId()));
            }

            // Determine priority based on user subscription and request
            JobPriority priority = determinePriority(currentUser, request.getPriority());

            // Parse scheduled time
            Instant scheduledAt = parseScheduledTime(request.getScheduledAt());

            // Validate scheduled time
            if (scheduledAt != null && scheduledAt.isBefore(Instant.now())) {
                throw new BusinessException("Scheduled time must be in the future");
            }

            // Create job
            VideoGenerationJob job = VideoGenerationJob.builder()
                    .jobId(generateJobId())
                    .user(currentUser)
                    .template(template)
                    .content(content)
                    .status(JobStatus.QUEUED)
                    .priority(priority)
                    .videoTitle(request.getVideoTitle())
                    .videoDescription(request.getVideoDescription())
                    .videoScript(request.getVideoScript())
                    .duration(request.getDuration() != null ? request.getDuration() : template.getDefaultDuration())
                    .brandingConfig(request.getBrandingConfig())
                    .generationParams(request.getGenerationParams())
                    .scheduledAt(scheduledAt)
                    .scheduledBy(scheduledAt != null ? currentUser.getId() : null)
                    .batchId(request.getBatchId())
                    .batchPosition(request.getBatchPosition())
                    .build();

            VideoGenerationJob savedJob = saveJobInTransaction(job);

            log.info("Video generation job created: {} for user: {} with priority: {} {}",
                    savedJob.getJobId(), currentUser.getId(), priority,
                    scheduledAt != null ? "scheduled for " + scheduledAt : "");

            // Send WebSocket notification
            String message = scheduledAt != null
                    ? "Video generation scheduled for " + scheduledAt
                    : "Video generation job queued";
            sendJobStatusUpdate(savedJob, message);

            return jobMapper.toDto(savedJob);

        } catch (BusinessException | NotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error creating video job", e);
            throw new BusinessException("Failed to create video generation job");
        }
    }

    /**
     * Create a batch of video generation jobs
     */
    public BatchVideoJobResponse createBatchVideoJobs(BatchVideoJobRequest request) {
        try {
            User currentUser = securityUtil.getCurrentUser();
            log.info("Creating batch of {} video jobs for user: {}",
                    request.getVideoJobs().size(), currentUser.getId());

            String batchId = generateBatchId();
            JobPriority batchPriority = parsePriority(request.getPriority());

            List<VideoJobDto> createdJobs = new ArrayList<>();
            int queuedCount = 0;
            int failedCount = 0;

            for (int i = 0; i < request.getVideoJobs().size(); i++) {
                CreateVideoJobRequest jobRequest = request.getVideoJobs().get(i);

                // Set batch information
                jobRequest.setBatchId(batchId);
                jobRequest.setBatchPosition(i + 1);
                if (jobRequest.getPriority() == null) {
                    jobRequest.setPriority(batchPriority.name());
                }

                try {
                    VideoJobDto job = createVideoJob(jobRequest);
                    createdJobs.add(job);
                    queuedCount++;
                } catch (Exception e) {
                    log.error("Failed to create video job {} in batch {}", i + 1, batchId, e);
                    failedCount++;
                }
            }

            log.info("Batch {} created: {} queued, {} failed", batchId, queuedCount, failedCount);

            // Calculate initial batch progress
            BatchProgressDto initialProgress = BatchProgressDto.builder()
                    .batchId(batchId)
                    .batchName(request.getBatchName())
                    .totalJobs(request.getVideoJobs().size())
                    .queuedJobs(queuedCount)
                    .processingJobs(0)
                    .completedJobs(0)
                    .failedJobs(failedCount)
                    .cancelledJobs(0)
                    .progressPercentage(0)
                    .status("QUEUED")
                    .message(String.format("Batch created: %d jobs queued, %d failed", queuedCount, failedCount))
                    .isComplete(false)
                    .hasErrors(failedCount > 0)
                    .build();

            // Send batch creation notification
            sendBatchProgressUpdate(batchId, initialProgress);

            return BatchVideoJobResponse.builder()
                    .batchId(batchId)
                    .batchName(request.getBatchName())
                    .totalJobs(request.getVideoJobs().size())
                    .queuedJobs(queuedCount)
                    .failedJobs(failedCount)
                    .jobs(createdJobs)
                    .message(String.format("Batch created: %d jobs queued, %d failed", queuedCount, failedCount))
                    .progress(initialProgress)
                    .build();

        } catch (Exception e) {
            log.error("Unexpected error creating batch video jobs", e);
            throw new BusinessException("Failed to create batch video jobs");
        }
    }

    /**
     * Get video job by job ID
     */
    public VideoJobDto getVideoJob(String jobId) {
        VideoGenerationJob job = findJobByJobId(jobId);
        return jobMapper.toDto(job);
    }

    /**
     * Get user's video jobs
     */
    public Page<VideoJobDto> getUserVideoJobs(int page, int size) {
        User currentUser = securityUtil.getCurrentUser();
        Pageable pageable = PageRequest.of(page, size);

        Page<VideoGenerationJob> jobs = jobRepository.findByUser_IdOrderByCreatedAtDesc(
                currentUser.getId(), pageable);

        return jobs.map(jobMapper::toDto);
    }

    /**
     * Get jobs by batch ID
     */
    public List<VideoJobDto> getBatchJobs(String batchId) {
        List<VideoGenerationJob> jobs = jobRepository.findByBatchIdOrderByBatchPositionAsc(batchId);
        return jobs.stream().map(jobMapper::toDto).toList();
    }

    /**
     * Get batch progress information
     */
    public BatchProgressDto getBatchProgress(String batchId) {
        List<VideoGenerationJob> jobs = jobRepository.findByBatchIdOrderByBatchPositionAsc(batchId);

        if (jobs.isEmpty()) {
            throw new NotFoundException("Batch not found: " + batchId);
        }

        return calculateBatchProgress(batchId, jobs);
    }

    /**
     * Calculate batch progress from job list
     */
    private BatchProgressDto calculateBatchProgress(String batchId, List<VideoGenerationJob> jobs) {
        int totalJobs = jobs.size();
        int queuedJobs = 0;
        int processingJobs = 0;
        int completedJobs = 0;
        int failedJobs = 0;
        int cancelledJobs = 0;

        Instant earliestStarted = null;
        Instant latestCompleted = null;
        long totalProcessingTime = 0;
        int completedCount = 0;

        // Count jobs by status and calculate timing metrics
        for (VideoGenerationJob job : jobs) {
            switch (job.getStatus()) {
                case QUEUED -> queuedJobs++;
                case PROCESSING -> {
                    processingJobs++;
                    if (earliestStarted == null
                            || (job.getStartedAt() != null && job.getStartedAt().isBefore(earliestStarted))) {
                        earliestStarted = job.getStartedAt();
                    }
                }
                case COMPLETED -> {
                    completedJobs++;
                    if (job.getProcessingTimeMs() != null) {
                        totalProcessingTime += job.getProcessingTimeMs();
                        completedCount++;
                    }
                    if (latestCompleted == null
                            || (job.getCompletedAt() != null && job.getCompletedAt().isAfter(latestCompleted))) {
                        latestCompleted = job.getCompletedAt();
                    }
                }
                case FAILED -> failedJobs++;
                case CANCELLED -> cancelledJobs++;
            }
        }

        // Calculate progress percentage
        int finishedJobs = completedJobs + failedJobs + cancelledJobs;
        int progressPercentage = (int) ((finishedJobs * 100.0) / totalJobs);

        // Determine batch status
        String batchStatus = determineBatchStatus(queuedJobs, processingJobs, completedJobs, failedJobs, cancelledJobs,
                totalJobs);

        // Calculate average processing time and estimate remaining time
        Long averageProcessingTime = completedCount > 0 ? totalProcessingTime / completedCount : null;
        Long estimatedTimeRemaining = null;

        if (averageProcessingTime != null && (queuedJobs + processingJobs) > 0) {
            estimatedTimeRemaining = averageProcessingTime * (queuedJobs + processingJobs);
        }

        // Determine if batch is complete and has errors
        boolean isComplete = (queuedJobs + processingJobs) == 0;
        boolean hasErrors = failedJobs > 0;

        // Build progress message
        String message = buildBatchProgressMessage(batchStatus, completedJobs, failedJobs, totalJobs);

        // Get batch name from first job (if available)
        String batchName = jobs.get(0).getTemplate() != null ? jobs.get(0).getTemplate().getName() + " Batch"
                : "Video Batch";

        return BatchProgressDto.builder()
                .batchId(batchId)
                .batchName(batchName)
                .totalJobs(totalJobs)
                .queuedJobs(queuedJobs)
                .processingJobs(processingJobs)
                .completedJobs(completedJobs)
                .failedJobs(failedJobs)
                .cancelledJobs(cancelledJobs)
                .progressPercentage(progressPercentage)
                .status(batchStatus)
                .startedAt(earliestStarted)
                .completedAt(isComplete ? latestCompleted : null)
                .estimatedTimeRemainingMs(estimatedTimeRemaining)
                .averageProcessingTimeMs(averageProcessingTime)
                .message(message)
                .isComplete(isComplete)
                .hasErrors(hasErrors)
                .build();
    }

    /**
     * Determine overall batch status based on job statuses
     */
    private String determineBatchStatus(int queued, int processing, int completed, int failed, int cancelled,
            int total) {
        // All jobs completed successfully
        if (completed == total) {
            return "COMPLETED";
        }

        // All jobs failed
        if (failed == total) {
            return "FAILED";
        }

        // All jobs cancelled
        if (cancelled == total) {
            return "CANCELLED";
        }

        // Some jobs are still processing or queued
        if (processing > 0 || queued > 0) {
            return "PROCESSING";
        }

        // Mix of completed and failed (partial success)
        if (completed > 0 && failed > 0) {
            return "PARTIAL";
        }

        // Default to queued if all jobs are queued
        if (queued == total) {
            return "QUEUED";
        }

        return "UNKNOWN";
    }

    /**
     * Build human-readable progress message
     */
    private String buildBatchProgressMessage(String status, int completed, int failed, int total) {
        return switch (status) {
            case "COMPLETED" -> String.format("Batch completed successfully: %d/%d videos generated", completed, total);
            case "FAILED" -> String.format("Batch failed: %d/%d videos failed", failed, total);
            case "CANCELLED" -> "Batch cancelled by user";
            case "PROCESSING" -> String.format("Processing batch: %d/%d videos completed", completed, total);
            case "PARTIAL" ->
                String.format("Batch partially completed: %d succeeded, %d failed out of %d", completed, failed, total);
            case "QUEUED" -> String.format("Batch queued: %d videos waiting to process", total);
            default -> String.format("Batch status: %d/%d videos completed", completed, total);
        };
    }

    /**
     * Cancel a video job
     */
    public VideoJobDto cancelVideoJob(String jobId) {
        VideoGenerationJob job = findJobByJobId(jobId);

        // Can only cancel queued or processing jobs
        if (job.getStatus() != JobStatus.QUEUED && job.getStatus() != JobStatus.PROCESSING) {
            throw new BusinessException("Cannot cancel job in status: " + job.getStatus());
        }

        updateJobToCancelled(job.getJobId());

        VideoGenerationJob updatedJob = findJobByJobId(jobId);
        sendJobStatusUpdate(updatedJob, "Video generation cancelled");

        return jobMapper.toDto(updatedJob);
    }

    /**
     * Get scheduled jobs for current user
     */
    public Page<VideoJobDto> getScheduledJobs(int page, int size) {
        User currentUser = securityUtil.getCurrentUser();
        Pageable pageable = PageRequest.of(page, size);

        Instant now = Instant.now();
        Page<VideoGenerationJob> jobs = jobRepository.findScheduledJobsByUser(
                currentUser.getId(), now, JobStatus.QUEUED, pageable);

        return jobs.map(jobMapper::toDto);
    }

    /**
     * Reschedule a video job
     */
    public VideoJobDto rescheduleVideoJob(String jobId, String newScheduledAt) {
        VideoGenerationJob job = findJobByJobId(jobId);

        // Can only reschedule queued jobs
        if (job.getStatus() != JobStatus.QUEUED) {
            throw new BusinessException("Cannot reschedule job in status: " + job.getStatus());
        }

        // Verify user owns the job
        User currentUser = securityUtil.getCurrentUser();
        if (!job.getUser().getId().equals(currentUser.getId())) {
            throw new BusinessException("You can only reschedule your own jobs");
        }

        // Parse and validate new scheduled time
        Instant scheduledAt = parseScheduledTime(newScheduledAt);
        if (scheduledAt == null) {
            throw new BusinessException("Scheduled time is required");
        }

        if (scheduledAt.isBefore(Instant.now())) {
            throw new BusinessException("Scheduled time must be in the future");
        }

        updateJobScheduledTime(jobId, scheduledAt);

        VideoGenerationJob updatedJob = findJobByJobId(jobId);
        sendJobStatusUpdate(updatedJob, "Video job rescheduled to " + scheduledAt);

        return jobMapper.toDto(updatedJob);
    }

    /**
     * Process queued video jobs (scheduled every 10 seconds)
     * Uses adaptive batch sizing based on system load
     */
    @Scheduled(fixedDelay = 10000)
    public void processQueuedJobs() {
        try {
            // Check if system can accept more jobs
            if (!resourceMonitor.canAcceptMoreJobs()) {
                log.debug("System at capacity, skipping video queue processing");
                return;
            }

            // Get recommended batch size based on current load
            int batchSize = resourceMonitor.getRecommendedBatchSize();

            Instant now = Instant.now();
            List<VideoGenerationJob> jobs = jobRepository.findNextJobsToProcess(
                    JobStatus.QUEUED, now, PageRequest.of(0, batchSize));

            if (jobs.isEmpty()) {
                return;
            }

            log.info("Processing {} video generation jobs from queue (batch size: {})",
                    jobs.size(), batchSize);

            for (VideoGenerationJob job : jobs) {
                // Double-check capacity before processing each job
                if (!resourceMonitor.canAcceptMoreJobs()) {
                    log.info("System reached capacity, stopping batch processing");
                    break;
                }
                processJobAsync(job);
            }

        } catch (Exception e) {
            log.error("Error during video queue processing cycle", e);
        }
    }

    /**
     * Process scheduled jobs (scheduled every 30 seconds)
     * Checks for scheduled jobs that are ready to be queued
     */
    @Scheduled(fixedDelay = 30000)
    public void processScheduledJobs() {
        try {
            int batchSize = resourceConfig.getQueue().getProcessingBatchSize();
            Instant now = Instant.now();
            List<VideoGenerationJob> scheduledJobs = jobRepository.findScheduledJobsReadyToQueue(
                    JobStatus.QUEUED, now, PageRequest.of(0, batchSize));

            if (scheduledJobs.isEmpty()) {
                return;
            }

            log.info("Found {} scheduled video jobs ready to process", scheduledJobs.size());

            for (VideoGenerationJob job : scheduledJobs) {
                log.info("Starting scheduled video job: {} (scheduled for: {})",
                        job.getJobId(), job.getScheduledAt());
                sendJobStatusUpdate(job, "Scheduled job is now starting...");
            }

        } catch (Exception e) {
            log.error("Error during scheduled job processing", e);
        }
    }

    /**
     * Process retry jobs (scheduled every 10 minutes)
     */
    @Scheduled(fixedDelay = 600000)
    public void processRetryJobs() {
        try {
            int batchSize = resourceConfig.getQueue().getProcessingBatchSize();
            Instant now = Instant.now();
            List<VideoGenerationJob> retryJobs = jobRepository.findJobsReadyForRetry(
                    JobStatus.FAILED, now, PageRequest.of(0, batchSize));

            if (retryJobs.isEmpty()) {
                return;
            }

            log.info("Processing {} video retry jobs", retryJobs.size());

            for (VideoGenerationJob job : retryJobs) {
                // Check capacity before retrying
                if (!resourceMonitor.canAcceptMoreJobs()) {
                    log.info("System at capacity, deferring retry jobs");
                    break;
                }
                updateJobToQueued(job.getJobId());
                processJobAsync(job);
            }

        } catch (Exception e) {
            log.error("Error during video retry job processing", e);
        }
    }

    /**
     * Clean up stale processing jobs (scheduled every 15 minutes)
     */
    @Scheduled(fixedDelay = 900000)
    public void cleanupStaleJobs() {
        try {
            int maxProcessingMinutes = resourceConfig.getQueue().getMaxProcessingTimeMinutes();
            Instant cutoffTime = Instant.now().minus(maxProcessingMinutes, ChronoUnit.MINUTES);
            List<VideoGenerationJob> staleJobs = jobRepository.findStaleProcessingJobs(
                    JobStatus.PROCESSING, cutoffTime);

            if (staleJobs.isEmpty()) {
                return;
            }

            log.warn("Found {} stale video processing jobs, marking as failed", staleJobs.size());

            for (VideoGenerationJob job : staleJobs) {
                updateJobToFailed(job.getJobId(), "Job processing timeout", "TIMEOUT");
            }

        } catch (Exception e) {
            log.error("Error during stale job cleanup", e);
        }
    }

    /**
     * Clean up old completed jobs (scheduled daily)
     */
    @Scheduled(cron = "0 0 2 * * *") // 2 AM daily
    public void cleanupOldJobs() {
        try {
            int cleanupDays = resourceConfig.getQueue().getCleanupDays();
            Instant cutoffTime = Instant.now().minus(cleanupDays, ChronoUnit.DAYS);
            List<JobStatus> completedStatuses = List.of(JobStatus.COMPLETED, JobStatus.FAILED, JobStatus.CANCELLED);

            List<VideoGenerationJob> oldJobs = jobRepository.findOldCompletedJobs(
                    completedStatuses, cutoffTime, PageRequest.of(0, 1000));

            if (oldJobs.isEmpty()) {
                return;
            }

            log.info("Cleaning up {} old video jobs", oldJobs.size());
            deleteJobsInTransaction(oldJobs);

        } catch (Exception e) {
            log.error("Error during old job cleanup", e);
        }
    }

    /**
     * Process a video job asynchronously
     */
    @Async
    public void processJobAsync(VideoGenerationJob job) {
        String jobId = job.getJobId();

        try {
            log.info("Starting video generation for job: {}", jobId);

            // Update job to processing
            updateJobToProcessing(jobId);

            // Send progress update
            sendJobStatusUpdate(job, "Starting video generation...");

            // Call N8N service to generate video
            Map<String, Object> videoResult = n8nService.generateVideo(
                    job.getVideoScript(),
                    job.getTemplate().getName(),
                    job.getBrandingConfig(),
                    job.getGenerationParams());

            // Extract video information from result
            String videoUrl = (String) videoResult.get("videoUrl");
            String thumbnailUrl = (String) videoResult.get("thumbnailUrl");
            Long videoSize = videoResult.get("videoSize") != null ? ((Number) videoResult.get("videoSize")).longValue()
                    : null;
            String videoFormat = (String) videoResult.get("videoFormat");

            // Update job to completed
            updateJobToCompleted(jobId, videoUrl, thumbnailUrl, videoSize, videoFormat);

            VideoGenerationJob completedJob = findJobByJobId(jobId);
            sendJobStatusUpdate(completedJob, "Video generation completed successfully");

            log.info("Video generation completed for job: {}", jobId);

        } catch (Exception e) {
            log.error("Video generation failed for job: {}", jobId, e);
            updateJobToFailed(jobId, e.getMessage(), "GENERATION_ERROR");

            VideoGenerationJob failedJob = findJobByJobId(jobId);
            sendJobStatusUpdate(failedJob, "Video generation failed: " + e.getMessage());
        }
    }

    // Transaction methods

    @Transactional
    private VideoGenerationJob saveJobInTransaction(VideoGenerationJob job) {
        return jobRepository.save(job);
    }

    @Transactional
    private void updateJobToProcessing(String jobId) {
        VideoGenerationJob job = findJobByJobId(jobId);
        job.setStatus(JobStatus.PROCESSING);
        job.setStartedAt(Instant.now());
        jobRepository.save(job);
    }

    @Transactional
    private void updateJobToCompleted(String jobId, String videoUrl, String thumbnailUrl,
            Long videoSize, String videoFormat) {
        VideoGenerationJob job = findJobByJobId(jobId);
        job.setStatus(JobStatus.COMPLETED);
        job.setCompletedAt(Instant.now());
        job.setVideoUrl(videoUrl);
        job.setThumbnailUrl(thumbnailUrl);
        job.setVideoSizeBytes(videoSize);
        job.setVideoFormat(videoFormat);

        if (job.getStartedAt() != null) {
            job.setProcessingTimeMs(
                    ChronoUnit.MILLIS.between(job.getStartedAt(), job.getCompletedAt()));
        }

        VideoGenerationJob savedJob = jobRepository.save(job);

        // Send batch progress update if job is part of a batch
        if (savedJob.getBatchId() != null) {
            sendBatchProgressUpdateForJob(savedJob);
        }
    }

    @Transactional
    private void updateJobToFailed(String jobId, String errorMessage, String errorCode) {
        VideoGenerationJob job = findJobByJobId(jobId);
        job.setStatus(JobStatus.FAILED);
        job.setCompletedAt(Instant.now());
        job.setErrorMessage(errorMessage);
        job.setErrorCode(errorCode);
        job.setRetryCount(job.getRetryCount() + 1);

        // Schedule retry if not exceeded max retries
        if (job.getRetryCount() < job.getMaxRetries()) {
            int retryDelayMinutes = resourceConfig.getQueue().getRetryDelayMinutes();
            job.setNextRetryAt(Instant.now().plus(retryDelayMinutes, ChronoUnit.MINUTES));
        }

        VideoGenerationJob savedJob = jobRepository.save(job);

        // Send batch progress update if job is part of a batch
        if (savedJob.getBatchId() != null) {
            sendBatchProgressUpdateForJob(savedJob);
        }
    }

    @Transactional
    private void updateJobToCancelled(String jobId) {
        VideoGenerationJob job = findJobByJobId(jobId);
        job.setStatus(JobStatus.CANCELLED);
        job.setCompletedAt(Instant.now());
        VideoGenerationJob savedJob = jobRepository.save(job);

        // Send batch progress update if job is part of a batch
        if (savedJob.getBatchId() != null) {
            sendBatchProgressUpdateForJob(savedJob);
        }
    }

    @Transactional
    private void updateJobToQueued(String jobId) {
        VideoGenerationJob job = findJobByJobId(jobId);
        job.setStatus(JobStatus.QUEUED);
        job.setStartedAt(null);
        job.setCompletedAt(null);
        job.setErrorMessage(null);
        job.setErrorCode(null);
        jobRepository.save(job);
    }

    @Transactional
    private void deleteJobsInTransaction(List<VideoGenerationJob> jobs) {
        jobRepository.deleteAll(jobs);
    }

    @Transactional
    private void updateJobScheduledTime(String jobId, Instant scheduledAt) {
        VideoGenerationJob job = findJobByJobId(jobId);
        job.setScheduledAt(scheduledAt);
        job.setScheduledBy(securityUtil.getCurrentUser().getId());
        jobRepository.save(job);
    }

    // Helper methods

    private VideoGenerationJob findJobByJobId(String jobId) {
        return jobRepository.findByJobId(jobId)
                .orElseThrow(() -> new NotFoundException("Video job not found: " + jobId));
    }

    private String generateJobId() {
        return "video_" + UUID.randomUUID().toString();
    }

    private String generateBatchId() {
        return "batch_" + UUID.randomUUID().toString();
    }

    /**
     * Determine job priority based on user subscription and request
     * 
     * Priority rules:
     * - URGENT: Reserved for system/admin operations
     * - HIGH: Available for premium users (future implementation)
     * - STANDARD: Default for all users
     * - LOW: For batch/background operations
     */
    private JobPriority determinePriority(User user, String requestedPriority) {
        // Parse requested priority
        JobPriority requested = parsePriority(requestedPriority);

        // TODO: Implement user subscription/role checking
        // For now, allow all priorities but log warnings for restricted ones
        // In production, this would check user.getSubscriptionPlan() or user roles

        if (requested == JobPriority.URGENT) {
            log.warn("User {} requested URGENT priority - this should be restricted to admins", user.getId());
            // For now, allow it but in production this would be downgraded
        }

        if (requested == JobPriority.HIGH) {
            log.info("User {} requested HIGH priority - verify premium subscription", user.getId());
            // For now, allow it but in production this would check subscription
        }

        // Return requested priority
        // In production, this would enforce subscription-based restrictions
        return requested;
    }

    private JobPriority parsePriority(String priority) {
        if (priority == null) {
            return JobPriority.STANDARD;
        }

        try {
            return JobPriority.valueOf(priority.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid priority: {}, using STANDARD", priority);
            return JobPriority.STANDARD;
        }
    }

    /**
     * Parse scheduled time from ISO-8601 string
     */
    private Instant parseScheduledTime(String scheduledAt) {
        if (scheduledAt == null || scheduledAt.trim().isEmpty()) {
            return null;
        }

        try {
            return Instant.parse(scheduledAt);
        } catch (Exception e) {
            log.error("Failed to parse scheduled time: {}", scheduledAt, e);
            throw new BusinessException(
                    "Invalid scheduled time format. Use ISO-8601 format (e.g., 2024-01-15T10:30:00Z)");
        }
    }

    private void sendJobStatusUpdate(VideoGenerationJob job, String message) {
        try {
            Map<String, Object> statusData = Map.of(
                    "jobId", job.getJobId(),
                    "status", job.getStatus().name(),
                    "message", message,
                    "progress", jobMapper.toDto(job).getProgressPercentage());

            webSocketService.sendJobStatusUpdate(job.getJobId(), job.getStatus().name(), statusData);
        } catch (Exception e) {
            log.error("Failed to send WebSocket update for job: {}", job.getJobId(), e);
        }
    }

    /**
     * Send batch progress update for a specific job's batch
     */
    private void sendBatchProgressUpdateForJob(VideoGenerationJob job) {
        if (job.getBatchId() == null) {
            return;
        }

        try {
            List<VideoGenerationJob> batchJobs = jobRepository.findByBatchIdOrderByBatchPositionAsc(job.getBatchId());
            BatchProgressDto progress = calculateBatchProgress(job.getBatchId(), batchJobs);
            sendBatchProgressUpdate(job.getBatchId(), progress);
        } catch (Exception e) {
            log.error("Failed to send batch progress update for batch: {}", job.getBatchId(), e);
        }
    }

    /**
     * Send batch progress update via WebSocket
     */
    private void sendBatchProgressUpdate(String batchId, BatchProgressDto progress) {
        try {
            Map<String, Object> progressData = Map.of(
                    "batchId", batchId,
                    "status", progress.getStatus(),
                    "progressPercentage", progress.getProgressPercentage(),
                    "completedJobs", progress.getCompletedJobs(),
                    "failedJobs", progress.getFailedJobs(),
                    "totalJobs", progress.getTotalJobs(),
                    "message", progress.getMessage(),
                    "isComplete", progress.getIsComplete(),
                    "hasErrors", progress.getHasErrors());

            // Send to batch-specific channel
            webSocketService.sendJobStatusUpdate("batch_" + batchId, progress.getStatus(), progressData);

            log.debug("Sent batch progress update for batch {}: {}% complete ({}/{})",
                    batchId, progress.getProgressPercentage(), progress.getCompletedJobs(), progress.getTotalJobs());
        } catch (Exception e) {
            log.error("Failed to send batch progress WebSocket update for batch: {}", batchId, e);
        }
    }
}
