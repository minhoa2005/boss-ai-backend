package ai.content.auto.service;

import ai.content.auto.dtos.*;
import ai.content.auto.entity.GenerationJob;
import ai.content.auto.entity.User;
import ai.content.auto.entity.GenerationJob.JobStatus;
import ai.content.auto.entity.GenerationJob.JobPriority;
import ai.content.auto.exception.BusinessException;
import ai.content.auto.mapper.GenerationJobMapper;
import ai.content.auto.repository.GenerationJobRepository;
import ai.content.auto.service.ai.AIProviderManager;
import ai.content.auto.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Service for managing content generation queue operations
 * Handles job queuing, status tracking, and queue statistics
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QueueManagementService {

    private final GenerationJobRepository jobRepository;
    private final GenerationJobMapper jobMapper;
    private final SecurityUtil securityUtil;
    private final RedisTemplate<String, Object> redisTemplate;
    private final WebSocketService webSocketService;
    private final AIProviderManager aiProviderManager;

    @Value("${app.queue.max-concurrent-jobs:10000}")
    private int maxConcurrentJobs;

    @Value("${app.queue.max-user-concurrent-jobs:10}")
    private int maxUserConcurrentJobs;

    @Value("${app.queue.default-job-expiration-hours:24}")
    private int defaultJobExpirationHours;

    @Value("${app.queue.cleanup-retention-days:7}")
    private int cleanupRetentionDays;

    private static final String QUEUE_STATS_KEY = "queue:stats";
    private static final String USER_JOB_COUNT_KEY = "queue:user:";

    /**
     * Queue a new content generation job
     */
    public QueueJobResponse queueJob(QueueJobRequest request) {
        try {
            // Get current user
            Long userId = securityUtil.getCurrentUserId();

            // Validate queue capacity
            validateQueueCapacity(userId);
            try {
                generateMetadata(request);
            } catch (Exception e) {
                log.warn("Failed to generate metadata, continuing without it: {}", e.getMessage());
            }
            // Create job entity
            GenerationJob job = createJobEntity(request, userId);

            // Save job to database
            GenerationJob savedJob = saveJobInTransaction(job);

            // Update Redis counters
            updateRedisCounters(userId, savedJob.getPriority());

            // Calculate queue position and estimated time
            Integer queuePosition = calculateQueuePosition(savedJob);
            Integer estimatedTime = calculateEstimatedProcessingTime(queuePosition);

            // Send job queued notification via WebSocket
            log.info("Sending WebSocket notification for job: {} to user: {}", savedJob.getJobId(), userId);
            webSocketService.sendJobStatusUpdate(savedJob.getJobId(), "QUEUED", Map.of(
                    "message", "Job queued successfully",
                    "progress", 0,
                    "queuePosition", queuePosition,
                    "estimatedTime", estimatedTime));

            log.info("Job queued successfully: {} for user: {}", savedJob.getJobId(), userId);

            return QueueJobResponse.builder()
                    .jobId(savedJob.getJobId())
                    .status(savedJob.getStatus())
                    .priority(savedJob.getPriority())
                    .queuePosition(queuePosition)
                    .estimatedProcessingTimeMinutes(estimatedTime)
                    .createdAt(savedJob.getCreatedAt())
                    .expiresAt(savedJob.getExpiresAt())
                    .message("Job queued successfully")
                    .websocketChannel("job-updates-" + savedJob.getJobId())
                    .build();

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to queue job for user: {}", securityUtil.getCurrentUserId(), e);
            throw new BusinessException("Failed to queue content generation job");
        }
    }

    /**
     * Get job status by job ID
     */
    public GenerationJobDto getJobStatus(String jobId) {
        try {
            Long userId = securityUtil.getCurrentUserId();

            GenerationJob job = findJobByIdAndUser(jobId, userId);

            return jobMapper.toDto(job);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to get job status: {} for user: {}", jobId, securityUtil.getCurrentUserId(), e);
            throw new BusinessException("Failed to retrieve job status");
        }
    }

    /**
     * Get user's job history with pagination
     */
    public Page<GenerationJobDto> getUserJobs(Long userId, Pageable pageable) {
        try {
            // Validate user access
            if (!userId.equals(securityUtil.getCurrentUserId())) {
                throw new BusinessException("Access denied to user jobs");
            }

            Page<GenerationJob> jobs = jobRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);

            return jobs.map(jobMapper::toDto);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to get user jobs for user: {}", userId, e);
            throw new BusinessException("Failed to retrieve user jobs");
        }
    }

    /**
     * Cancel a queued job
     */
    @Transactional
    public void cancelJob(String jobId) {
        try {
            Long userId = securityUtil.getCurrentUserId();

            GenerationJob job = findJobByIdAndUser(jobId, userId);

            // Only allow cancellation of queued jobs
            if (job.getStatus() != JobStatus.QUEUED) {
                throw new BusinessException("Job cannot be cancelled in current status: " + job.getStatus());
            }

            // Update job status
            job.setStatus(JobStatus.CANCELLED);
            job.setCompletedAt(Instant.now());
            job.setUpdatedAt(Instant.now());

            jobRepository.save(job);

            // Update Redis counters
            decrementRedisCounters(userId, job.getPriority());

            // Send job cancelled notification via WebSocket
            webSocketService.sendJobErrorNotification(userId, jobId,
                    "Job cancelled by user", Map.of(
                            "reason", "user_cancelled",
                            "cancelledAt", Instant.now().toString()));

            log.info("Job cancelled: {} by user: {}", jobId, userId);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to cancel job: {} for user: {}", jobId, securityUtil.getCurrentUserId(), e);
            throw new BusinessException("Failed to cancel job");
        }
    }

    /**
     * Get next jobs to process from queue
     */
    public List<GenerationJobDto> getNextJobsToProcess(int limit) {
        try {
            Pageable pageable = PageRequest.of(0, limit);
            List<GenerationJob> jobs = jobRepository.findNextJobsToProcess(JobStatus.QUEUED, pageable);

            return jobs.stream()
                    .map(jobMapper::toDto)
                    .toList();

        } catch (Exception e) {
            log.error("Failed to get next jobs to process", e);
            throw new BusinessException("Failed to retrieve jobs from queue");
        }
    }

    /**
     * Get queue statistics for monitoring
     */
    public QueueStatisticsDto getQueueStatistics() {
        try {
            // Get cached statistics from Redis
            QueueStatisticsDto cachedStats = getCachedStatistics();
            if (cachedStats != null) {
                return cachedStats;
            }

            // Calculate fresh statistics
            QueueStatisticsDto stats = calculateQueueStatistics();

            // Cache statistics for 30 seconds
            cacheStatistics(stats);

            return stats;

        } catch (Exception e) {
            log.error("Failed to get queue statistics", e);
            throw new BusinessException("Failed to retrieve queue statistics");
        }
    }

    /**
     * Clean up expired and old completed jobs
     */
    @Transactional
    public void cleanupJobs() {
        try {
            Instant now = Instant.now();
            Instant cutoffTime = now.minus(cleanupRetentionDays, ChronoUnit.DAYS);

            // Mark expired jobs
            List<GenerationJob> expiredJobs = jobRepository.findExpiredJobs(now);
            for (GenerationJob job : expiredJobs) {
                job.setStatus(JobStatus.EXPIRED);
                job.setCompletedAt(now);
                job.setUpdatedAt(now);
            }
            jobRepository.saveAll(expiredJobs);

            // Delete old completed jobs
            List<JobStatus> cleanupStatuses = Arrays.asList(
                    JobStatus.COMPLETED, JobStatus.FAILED, JobStatus.CANCELLED, JobStatus.EXPIRED);
            int deletedCount = jobRepository.deleteJobsOlderThan(cutoffTime, cleanupStatuses);

            log.info("Cleanup completed: {} expired jobs marked, {} old jobs deleted",
                    expiredJobs.size(), deletedCount);

        } catch (Exception e) {
            log.error("Failed to cleanup jobs", e);
        }
    }

    // Private helper methods

    private void validateQueueCapacity(Long userId) {
        // Check global queue capacity
        long totalActiveJobs = jobRepository.countByStatus(JobStatus.QUEUED) +
                jobRepository.countByStatus(JobStatus.PROCESSING);

        if (totalActiveJobs >= maxConcurrentJobs) {
            throw new BusinessException("Queue is at maximum capacity. Please try again later.");
        }

        // Check user concurrent job limit
        long userActiveJobs = jobRepository.countActiveJobsByUser(userId);
        if (userActiveJobs >= maxUserConcurrentJobs) {
            throw new BusinessException("You have reached the maximum number of concurrent jobs (" +
                    maxUserConcurrentJobs + ")");
        }
    }

    private GenerationJob createJobEntity(QueueJobRequest request, Long userId) {
        String jobId = UUID.randomUUID().toString();
        Instant now = Instant.now();
        Instant expiresAt = now.plus(
                request.getExpirationHours() != null ? request.getExpirationHours() : defaultJobExpirationHours,
                ChronoUnit.HOURS);

        String contentType = request.getContentType();
        if (contentType == null && request.getRequestParams() != null) {
            Object ct = request.getRequestParams().get("contentType");
            if (ct != null) {
                contentType = ct.toString();
            }
        }
        return GenerationJob.builder()
                .jobId(jobId)
                .userId(userId)
                .requestParams(request.getRequestParams())
                .status(JobStatus.QUEUED)
                .priority(request.getPriority())
                .contentType(contentType)
                .maxRetries(request.getMaxRetries())
                .expiresAt(expiresAt)
                .metadata(request.getMetadata())
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    @Transactional
    private GenerationJob saveJobInTransaction(GenerationJob job) {
        return jobRepository.save(job);
    }

    private GenerationJob findJobByIdAndUser(String jobId, Long userId) {
        GenerationJob job = jobRepository.findByJobId(jobId)
                .orElseThrow(() -> new BusinessException("Job not found: " + jobId));

        if (!job.getUserId().equals(userId)) {
            throw new BusinessException("Access denied to job: " + jobId);
        }

        return job;
    }

    private Integer calculateQueuePosition(GenerationJob job) {
        // Count jobs with higher or equal priority that were created before this job
        long position = jobRepository.findNextJobsToProcess(JobStatus.QUEUED, Pageable.unpaged())
                .stream()
                .takeWhile(j -> !j.getJobId().equals(job.getJobId()))
                .count() + 1;

        return (int) position;
    }

    private Integer calculateEstimatedProcessingTime(Integer queuePosition) {
        // Estimate based on average processing time and queue position
        // This is a simplified calculation - in production, you'd use more
        // sophisticated algorithms
        int avgProcessingMinutes = 2; // Average 2 minutes per job
        int concurrentProcessors = 5; // Assume 5 concurrent processors

        return (queuePosition / concurrentProcessors) * avgProcessingMinutes;
    }

    private void updateRedisCounters(Long userId, JobPriority priority) {
        try {
            // Update user job count
            String userKey = USER_JOB_COUNT_KEY + userId;
            redisTemplate.opsForValue().increment(userKey);
            redisTemplate.expire(userKey, 1, TimeUnit.HOURS);

            // Update priority counters
            String priorityKey = QUEUE_STATS_KEY + ":priority:" + priority.name();
            redisTemplate.opsForValue().increment(priorityKey);

        } catch (Exception e) {
            log.warn("Failed to update Redis counters", e);
        }
    }

    private void decrementRedisCounters(Long userId, JobPriority priority) {
        try {
            // Decrement user job count
            String userKey = USER_JOB_COUNT_KEY + userId;
            redisTemplate.opsForValue().decrement(userKey);

            // Decrement priority counters
            String priorityKey = QUEUE_STATS_KEY + ":priority:" + priority.name();
            redisTemplate.opsForValue().decrement(priorityKey);

        } catch (Exception e) {
            log.warn("Failed to decrement Redis counters", e);
        }
    }

    private QueueStatisticsDto getCachedStatistics() {
        try {
            return (QueueStatisticsDto) redisTemplate.opsForValue().get(QUEUE_STATS_KEY);
        } catch (Exception e) {
            log.warn("Failed to get cached statistics", e);
            return null;
        }
    }

    private QueueStatisticsDto calculateQueueStatistics() {
        Instant oneHourAgo = Instant.now().minus(1, ChronoUnit.HOURS);

        // Get basic counts
        long queuedJobs = jobRepository.countByStatus(JobStatus.QUEUED);
        long processingJobs = jobRepository.countByStatus(JobStatus.PROCESSING);

        // Get processing statistics
        Object[] stats = jobRepository.getProcessingStatistics(oneHourAgo);
        Long completedLastHour = (Long) stats[2];
        Long failedLastHour = (Long) stats[3];
        Double avgProcessingTime = (Double) stats[4];

        // Get queue depth by priority
        Map<String, Long> queueDepthByPriority = new HashMap<>();
        for (JobPriority priority : JobPriority.values()) {
            long count = jobRepository.countByStatusAndPriority(JobStatus.QUEUED, priority);
            queueDepthByPriority.put(priority.name(), count);
        }

        // Calculate capacity utilization
        double capacityUtilization = (double) processingJobs / maxConcurrentJobs;

        // Estimate wait time
        int estimatedWaitTime = calculateEstimatedProcessingTime((int) queuedJobs);

        // Determine health status
        String healthStatus = determineHealthStatus(capacityUtilization, failedLastHour, completedLastHour);

        return QueueStatisticsDto.builder()
                .queuedJobs(queuedJobs)
                .processingJobs(processingJobs)
                .completedJobsLastHour(completedLastHour)
                .failedJobsLastHour(failedLastHour)
                .averageProcessingTimeMs(avgProcessingTime)
                .queueDepthByPriority(queueDepthByPriority)
                .capacityUtilization(capacityUtilization)
                .estimatedWaitTimeMinutes(estimatedWaitTime)
                .healthStatus(healthStatus)
                .lastUpdated(Instant.now())
                .build();
    }

    private void cacheStatistics(QueueStatisticsDto stats) {
        try {
            redisTemplate.opsForValue().set(QUEUE_STATS_KEY, stats, 30, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Failed to cache statistics", e);
        }
    }

    private String determineHealthStatus(double capacityUtilization, Long failedLastHour, Long completedLastHour) {
        if (capacityUtilization > 0.9) {
            return "OVERLOADED";
        } else if (failedLastHour > 0 && completedLastHour > 0 &&
                (double) failedLastHour / (failedLastHour + completedLastHour) > 0.1) {
            return "DEGRADED";
        } else if (capacityUtilization > 0.7) {
            return "BUSY";
        } else {
            return "HEALTHY";
        }
    }

    private void generateMetadata(QueueJobRequest request) {
        GenerateMetadataRequest metadataRequest = new GenerateMetadataRequest();
        request.getRequestParams().forEach((key, value) -> {
            if (key.equals("title")) {
                metadataRequest.setTitle(value.toString());
            } else if (key.equals("content")) {
                metadataRequest.setContent(value.toString());
            } else if (key.equals("industry")) {
                metadataRequest.setIndustry(value.toString());
            } else if (key.equals("communicationGoal")) {
                metadataRequest.setCommunicationGoal(value.toString());
            } else if (key.equals("businessProfile")) {
                metadataRequest.setBusinessProfile(value.toString());
            }
        });
        try {
            User user = securityUtil.getCurrentUser();
            GenerateMetadataResponse metadataResponse = aiProviderManager.generateMetadata(metadataRequest, user);
            Map<String, Object> params = request.getRequestParams() != null
                    ? new HashMap<>(request.getRequestParams())
                    : new HashMap<>();

            if (metadataResponse.getContentType() != null)
                params.put("contentType", metadataResponse.getContentType());
            if (metadataResponse.getTone() != null)
                params.put("tone", metadataResponse.getTone());
            if (metadataResponse.getTargetAudience() != null)
                params.put("targetAudience", metadataResponse.getTargetAudience());
            request.setRequestParams(params);
        } catch (Exception e) {
            log.error("Metadata generation failed: {}", e.getMessage());
            throw e;
        }
    }
}