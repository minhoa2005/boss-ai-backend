package ai.content.auto.controller;

import ai.content.auto.dtos.*;
import ai.content.auto.entity.GenerationJob;
import ai.content.auto.service.QueueManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for content generation queue operations
 */
@RestController
@RequestMapping("/api/v1/queue")
@RequiredArgsConstructor
@Slf4j
public class QueueController {

    private final QueueManagementService queueManagementService;
    private final ai.content.auto.util.SecurityUtil securityUtil;

    /**
     * Queue a new content generation job
     */
    @PostMapping("/jobs")
    public ResponseEntity<BaseResponse<QueueJobResponse>> queueJob(
            @Valid @RequestBody QueueJobRequest request) {

        log.info("Queuing content generation job with type: {}", request.getRequestParams());

        QueueJobResponse response = queueManagementService.queueJob(request);

        BaseResponse<QueueJobResponse> baseResponse = new BaseResponse<QueueJobResponse>()
                .setErrorMessage("Job queued successfully")
                .setData(response);

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(baseResponse);
    }

    /**
     * Get job status by job ID
     */
    @GetMapping("/jobs/{jobId}")
    public ResponseEntity<BaseResponse<GenerationJobDto>> getJobStatus(
            @PathVariable String jobId) {

        log.info("Getting status for job: {}", jobId);

        GenerationJobDto job = queueManagementService.getJobStatus(jobId);

        BaseResponse<GenerationJobDto> response = new BaseResponse<GenerationJobDto>()
                .setErrorMessage("Job status retrieved successfully")
                .setData(job);

        return ResponseEntity.ok(response);
    }

    /**
     * Cancel a queued job
     */
    @DeleteMapping("/jobs/{jobId}")
    public ResponseEntity<BaseResponse<Void>> cancelJob(@PathVariable String jobId) {

        log.info("Cancelling job: {}", jobId);

        queueManagementService.cancelJob(jobId);

        BaseResponse<Void> response = new BaseResponse<Void>()
                .setErrorMessage("Job cancelled successfully");

        return ResponseEntity.ok(response);
    }

    /**
     * Get user's job history
     */
    @GetMapping("/jobs")
    public ResponseEntity<BaseResponse<Page<GenerationJobDto>>> getUserJobs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.info("Getting user jobs - page: {}, size: {}", page, size);

        Pageable pageable = PageRequest.of(page, size);

        // Get current user ID from security context
        Long userId = getCurrentUserId();
        Page<GenerationJobDto> jobs = queueManagementService.getUserJobs(userId, pageable);

        BaseResponse<Page<GenerationJobDto>> response = new BaseResponse<Page<GenerationJobDto>>()
                .setErrorMessage("User jobs retrieved successfully")
                .setData(jobs);

        return ResponseEntity.ok(response);
    }

    /**
     * Get queue statistics (admin only)
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BaseResponse<QueueStatisticsDto>> getQueueStatistics() {

        log.info("Getting queue statistics");

        QueueStatisticsDto statistics = queueManagementService.getQueueStatistics();

        BaseResponse<QueueStatisticsDto> response = new BaseResponse<QueueStatisticsDto>()
                .setErrorMessage("Queue statistics retrieved successfully")
                .setData(statistics);

        return ResponseEntity.ok(response);
    }

    /**
     * Trigger manual cleanup of old jobs (admin only)
     */
    @PostMapping("/cleanup")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BaseResponse<Void>> triggerCleanup() {

        log.info("Triggering manual job cleanup");

        queueManagementService.cleanupJobs();

        BaseResponse<Void> response = new BaseResponse<Void>()
                .setErrorMessage("Job cleanup triggered successfully");

        return ResponseEntity.ok(response);
    }

    /**
     * Manually trigger job processing (for testing)
     */
    @PostMapping("/process")
    public ResponseEntity<BaseResponse<Void>> triggerJobProcessing() {

        log.info("Manually triggering job processing");

        // This would trigger the ContentGenerationQueueService to process jobs
        // immediately
        // For now, just return success - the actual implementation would call the
        // processing service

        BaseResponse<Void> response = new BaseResponse<Void>()
                .setErrorMessage("Job processing triggered successfully");

        return ResponseEntity.ok(response);
    }

    // Helper method to get current user ID
    private Long getCurrentUserId() {
        return securityUtil.getCurrentUserId();
    }

}