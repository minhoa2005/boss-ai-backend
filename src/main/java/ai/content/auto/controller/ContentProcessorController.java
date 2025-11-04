package ai.content.auto.controller;

import ai.content.auto.dtos.BaseResponse;
import ai.content.auto.service.ContentProcessor;
import ai.content.auto.websocket.JobStatusWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller for content processor management and monitoring
 */
@RestController
@RequestMapping("/api/v1/processor")
@RequiredArgsConstructor
@Slf4j
public class ContentProcessorController {

    private final ContentProcessor contentProcessor;
    private final JobStatusWebSocketHandler webSocketHandler;

    /**
     * Get processing statistics for monitoring
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BaseResponse<Map<String, Object>>> getProcessingStatistics() {
        log.info("Getting processing statistics");

        Map<String, Object> statistics = contentProcessor.getProcessingStatistics();

        BaseResponse<Map<String, Object>> response = new BaseResponse<Map<String, Object>>()
                .setErrorMessage("Processing statistics retrieved successfully")
                .setData(statistics);

        return ResponseEntity.ok(response);
    }

    /**
     * Get WebSocket connection statistics
     */
    @GetMapping("/websocket/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BaseResponse<Map<String, Object>>> getWebSocketStatistics() {
        log.info("Getting WebSocket statistics");

        Map<String, Object> statistics = Map.of(
                "active_connections", webSocketHandler.getActiveConnectionCount(),
                "active_subscriptions", webSocketHandler.getActiveSubscriptionCount());

        BaseResponse<Map<String, Object>> response = new BaseResponse<Map<String, Object>>()
                .setErrorMessage("WebSocket statistics retrieved successfully")
                .setData(statistics);

        return ResponseEntity.ok(response);
    }

    /**
     * Process a specific job manually (admin only)
     */
    @PostMapping("/jobs/{jobId}/process")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BaseResponse<String>> processJob(@PathVariable String jobId) {
        log.info("Manual job processing requested for job: {}", jobId);

        try {
            contentProcessor.processJob(jobId);

            BaseResponse<String> response = new BaseResponse<String>()
                    .setErrorMessage("Job processing initiated successfully")
                    .setData("Job " + jobId + " has been queued for processing");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to initiate job processing for job: {}", jobId, e);

            BaseResponse<String> response = new BaseResponse<String>()
                    .setErrorMessage("Failed to initiate job processing: " + e.getMessage());

            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Send system notification to all connected WebSocket clients (admin only)
     */
    @PostMapping("/websocket/broadcast")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BaseResponse<String>> broadcastSystemNotification(
            @RequestBody Map<String, String> request) {

        String message = request.get("message");
        String level = request.getOrDefault("level", "info");

        if (message == null || message.trim().isEmpty()) {
            BaseResponse<String> response = new BaseResponse<String>()
                    .setErrorMessage("Message is required");
            return ResponseEntity.badRequest().body(response);
        }

        log.info("Broadcasting system notification: {} (level: {})", message, level);

        webSocketHandler.broadcastSystemNotification(message, level);

        BaseResponse<String> response = new BaseResponse<String>()
                .setErrorMessage("System notification broadcasted successfully")
                .setData("Notification sent to " + webSocketHandler.getActiveConnectionCount() + " connected clients");

        return ResponseEntity.ok(response);
    }

    /**
     * Health check endpoint for the processor
     */
    @GetMapping("/health")
    public ResponseEntity<BaseResponse<Map<String, Object>>> getProcessorHealth() {
        Map<String, Object> statistics = contentProcessor.getProcessingStatistics();

        // Determine health status
        int activeJobs = (Integer) statistics.get("active_jobs");
        int availableSlots = (Integer) statistics.get("available_slots");
        int maxConcurrentJobs = (Integer) statistics.get("max_concurrent_jobs");

        String healthStatus;
        if (availableSlots == 0) {
            healthStatus = "OVERLOADED";
        } else if (activeJobs > maxConcurrentJobs * 0.8) {
            healthStatus = "BUSY";
        } else {
            healthStatus = "HEALTHY";
        }

        Map<String, Object> health = Map.of(
                "status", healthStatus,
                "active_jobs", activeJobs,
                "available_slots", availableSlots,
                "capacity_utilization", (double) activeJobs / maxConcurrentJobs,
                "websocket_connections", webSocketHandler.getActiveConnectionCount(),
                "timestamp", System.currentTimeMillis());

        BaseResponse<Map<String, Object>> response = new BaseResponse<Map<String, Object>>()
                .setErrorMessage("Processor health retrieved successfully")
                .setData(health);

        return ResponseEntity.ok(response);
    }
}