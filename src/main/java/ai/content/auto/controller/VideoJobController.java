package ai.content.auto.controller;

import ai.content.auto.dto.request.CreateVideoJobRequest;
import ai.content.auto.dto.request.BatchVideoJobRequest;
import ai.content.auto.dto.response.VideoJobDto;
import ai.content.auto.dto.response.BatchVideoJobResponse;
import ai.content.auto.dto.response.BatchProgressDto;
import ai.content.auto.dtos.BaseResponse;
import ai.content.auto.service.VideoGenerationQueueService;
import ai.content.auto.service.VideoResourceMonitorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller for video generation queue management.
 * Handles video job creation, batch processing, and job status queries.
 */
@RestController
@RequestMapping("/api/v1/video/jobs")
@RequiredArgsConstructor
@Slf4j
public class VideoJobController {

        private final VideoGenerationQueueService videoQueueService;
        private final VideoResourceMonitorService resourceMonitor;

        /**
         * Create a single video generation job
         */
        @PostMapping
        public ResponseEntity<BaseResponse<VideoJobDto>> createVideoJob(
                        @Valid @RequestBody CreateVideoJobRequest request) {

                log.info("Creating video generation job with title: {}", request.getVideoTitle());

                VideoJobDto job = videoQueueService.createVideoJob(request);

                BaseResponse<VideoJobDto> response = new BaseResponse<VideoJobDto>()
                                .setErrorMessage("Video generation job created successfully")
                                .setData(job);

                return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }

        /**
         * Create a batch of video generation jobs
         */
        @PostMapping("/batch")
        public ResponseEntity<BaseResponse<BatchVideoJobResponse>> createBatchVideoJobs(
                        @Valid @RequestBody BatchVideoJobRequest request) {

                log.info("Creating batch of {} video jobs", request.getVideoJobs().size());

                BatchVideoJobResponse batchResponse = videoQueueService.createBatchVideoJobs(request);

                BaseResponse<BatchVideoJobResponse> response = new BaseResponse<BatchVideoJobResponse>()
                                .setErrorMessage("Batch video jobs created successfully")
                                .setData(batchResponse);

                return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }

        /**
         * Get video job by job ID
         */
        @GetMapping("/{jobId}")
        public ResponseEntity<BaseResponse<VideoJobDto>> getVideoJob(@PathVariable String jobId) {

                log.info("Getting video job: {}", jobId);

                VideoJobDto job = videoQueueService.getVideoJob(jobId);

                BaseResponse<VideoJobDto> response = new BaseResponse<VideoJobDto>()
                                .setErrorMessage("Video job retrieved successfully")
                                .setData(job);

                return ResponseEntity.ok(response);
        }

        /**
         * Get current user's video jobs
         */
        @GetMapping
        public ResponseEntity<BaseResponse<Page<VideoJobDto>>> getUserVideoJobs(
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size) {

                log.info("Getting user video jobs - page: {}, size: {}", page, size);

                Page<VideoJobDto> jobs = videoQueueService.getUserVideoJobs(page, size);

                BaseResponse<Page<VideoJobDto>> response = new BaseResponse<Page<VideoJobDto>>()
                                .setErrorMessage("User video jobs retrieved successfully")
                                .setData(jobs);

                return ResponseEntity.ok(response);
        }

        /**
         * Get jobs by batch ID
         */
        @GetMapping("/batch/{batchId}")
        public ResponseEntity<BaseResponse<List<VideoJobDto>>> getBatchJobs(@PathVariable String batchId) {

                log.info("Getting batch jobs: {}", batchId);

                List<VideoJobDto> jobs = videoQueueService.getBatchJobs(batchId);

                BaseResponse<List<VideoJobDto>> response = new BaseResponse<List<VideoJobDto>>()
                                .setErrorMessage("Batch jobs retrieved successfully")
                                .setData(jobs);

                return ResponseEntity.ok(response);
        }

        /**
         * Cancel a video job
         */
        @PostMapping("/{jobId}/cancel")
        public ResponseEntity<BaseResponse<VideoJobDto>> cancelVideoJob(@PathVariable String jobId) {

                log.info("Cancelling video job: {}", jobId);

                VideoJobDto job = videoQueueService.cancelVideoJob(jobId);

                BaseResponse<VideoJobDto> response = new BaseResponse<VideoJobDto>()
                                .setErrorMessage("Video job cancelled successfully")
                                .setData(job);

                return ResponseEntity.ok(response);
        }

        /**
         * Get scheduled jobs for current user
         */
        @GetMapping("/scheduled")
        public ResponseEntity<BaseResponse<Page<VideoJobDto>>> getScheduledJobs(
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size) {

                log.info("Getting scheduled video jobs - page: {}, size: {}", page, size);

                Page<VideoJobDto> jobs = videoQueueService.getScheduledJobs(page, size);

                BaseResponse<Page<VideoJobDto>> response = new BaseResponse<Page<VideoJobDto>>()
                                .setErrorMessage("Scheduled video jobs retrieved successfully")
                                .setData(jobs);

                return ResponseEntity.ok(response);
        }

        /**
         * Reschedule a video job
         */
        @PutMapping("/{jobId}/reschedule")
        public ResponseEntity<BaseResponse<VideoJobDto>> rescheduleVideoJob(
                        @PathVariable String jobId,
                        @RequestParam String scheduledAt) {

                log.info("Rescheduling video job: {} to {}", jobId, scheduledAt);

                VideoJobDto job = videoQueueService.rescheduleVideoJob(jobId, scheduledAt);

                BaseResponse<VideoJobDto> response = new BaseResponse<VideoJobDto>()
                                .setErrorMessage("Video job rescheduled successfully")
                                .setData(job);

                return ResponseEntity.ok(response);
        }

        /**
         * Get batch progress information
         */
        @GetMapping("/batch/{batchId}/progress")
        public ResponseEntity<BaseResponse<BatchProgressDto>> getBatchProgress(
                        @PathVariable String batchId) {

                log.info("Getting batch progress: {}", batchId);

                BatchProgressDto progress = videoQueueService.getBatchProgress(batchId);

                BaseResponse<BatchProgressDto> response = new BaseResponse<BatchProgressDto>()
                                .setErrorMessage("Batch progress retrieved successfully")
                                .setData(progress);

                return ResponseEntity.ok(response);
        }

        /**
         * Get video generation resource metrics
         * Provides information about system capacity, resource usage, and optimization
         * status
         */
        @GetMapping("/resources/metrics")
        public ResponseEntity<BaseResponse<Map<String, Object>>> getResourceMetrics() {

                log.info("Getting video generation resource metrics");

                Map<String, Object> metrics = resourceMonitor.getResourceMetrics();

                BaseResponse<Map<String, Object>> response = new BaseResponse<Map<String, Object>>()
                                .setErrorMessage("Resource metrics retrieved successfully")
                                .setData(metrics);

                return ResponseEntity.ok(response);
        }
}
