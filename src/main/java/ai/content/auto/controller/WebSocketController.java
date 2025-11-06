package ai.content.auto.controller;

import ai.content.auto.dtos.BaseResponse;
import ai.content.auto.service.WebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * WebSocket management controller
 * Provides endpoints for WebSocket status and administration
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/websocket")
@RequiredArgsConstructor
public class WebSocketController {

    private final WebSocketService webSocketService;

    /**
     * Get WebSocket connection statistics
     */
    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BaseResponse<WebSocketService.ConnectionStats>> getConnectionStats() {
        log.info("Getting WebSocket connection statistics");

        WebSocketService.ConnectionStats stats = webSocketService.getConnectionStats();

        BaseResponse<WebSocketService.ConnectionStats> response = new BaseResponse<WebSocketService.ConnectionStats>()
                .setErrorMessage("WebSocket statistics retrieved successfully")
                .setData(stats);

        return ResponseEntity.ok(response);
    }

    /**
     * Check if specific user is connected
     */
    @GetMapping("/user/{userId}/status")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.id")
    public ResponseEntity<BaseResponse<Map<String, Object>>> getUserConnectionStatus(@PathVariable Long userId) {
        log.info("Checking connection status for user: {}", userId);

        boolean isConnected = webSocketService.isUserConnected(userId);

        Map<String, Object> status = Map.of(
                "userId", userId,
                "connected", isConnected,
                "timestamp", System.currentTimeMillis());

        BaseResponse<Map<String, Object>> response = new BaseResponse<Map<String, Object>>()
                .setErrorMessage("User connection status retrieved successfully")
                .setData(status);

        return ResponseEntity.ok(response);
    }

    /**
     * Send system notification to all connected users
     */
    @PostMapping("/broadcast/system")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BaseResponse<Void>> broadcastSystemNotification(
            @RequestBody SystemNotificationRequest request) {

        log.info("Broadcasting system notification: {} (level: {})", request.getMessage(), request.getLevel());

        WebSocketService.NotificationLevel level;
        try {
            level = WebSocketService.NotificationLevel.valueOf(request.getLevel().toUpperCase());
        } catch (IllegalArgumentException e) {
            level = WebSocketService.NotificationLevel.INFO;
        }

        webSocketService.broadcastSystemNotification(request.getMessage(), level);

        BaseResponse<Void> response = new BaseResponse<Void>()
                .setErrorMessage("System notification broadcasted successfully");

        return ResponseEntity.ok(response);
    }

    /**
     * Send maintenance notification
     */
    @PostMapping("/broadcast/maintenance")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BaseResponse<Void>> sendMaintenanceNotification(
            @RequestBody MaintenanceNotificationRequest request) {

        log.info("Sending maintenance notification: {} (delay: {} minutes)",
                request.getMessage(), request.getDelayMinutes());

        webSocketService.sendMaintenanceNotification(request.getMessage(), request.getDelayMinutes());

        BaseResponse<Void> response = new BaseResponse<Void>()
                .setErrorMessage("Maintenance notification sent successfully");

        return ResponseEntity.ok(response);
    }

    /**
     * Send message to specific user channel
     */
    @PostMapping("/user/{userId}/channel/{channel}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BaseResponse<Void>> sendToUserChannel(
            @PathVariable Long userId,
            @PathVariable String channel,
            @RequestBody ChannelMessageRequest request) {

        log.info("Sending message to user {} on channel {}: {}", userId, channel, request.getMessageType());

        webSocketService.sendToUserChannel(userId, channel, request.getMessageType(), request.getData());

        BaseResponse<Void> response = new BaseResponse<Void>()
                .setErrorMessage("Message sent to user channel successfully");

        return ResponseEntity.ok(response);
    }

    /**
     * Broadcast message to channel
     */
    @PostMapping("/broadcast/channel/{channel}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BaseResponse<Void>> broadcastToChannel(
            @PathVariable String channel,
            @RequestBody ChannelMessageRequest request) {

        log.info("Broadcasting message to channel {}: {}", channel, request.getMessageType());

        webSocketService.broadcastToChannel(channel, request.getMessageType(), request.getData());

        BaseResponse<Void> response = new BaseResponse<Void>()
                .setErrorMessage("Message broadcasted to channel successfully");

        return ResponseEntity.ok(response);
    }

    /**
     * Test job status update (for debugging)
     */
    @PostMapping("/test/job/{jobId}/status")
    public ResponseEntity<BaseResponse<Void>> testJobStatusUpdate(
            @PathVariable String jobId,
            @RequestBody TestJobStatusRequest request) {

        log.info("Sending test job status update for job: {} - status: {}", jobId, request.getStatus());

        webSocketService.sendJobStatusUpdate(jobId, request.getStatus(), request.getData());

        BaseResponse<Void> response = new BaseResponse<Void>()
                .setErrorMessage("Test job status update sent successfully");

        return ResponseEntity.ok(response);
    }

    /**
     * System notification request DTO
     */
    public static class SystemNotificationRequest {
        private String message;
        private String level = "INFO";

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getLevel() {
            return level;
        }

        public void setLevel(String level) {
            this.level = level;
        }
    }

    /**
     * Maintenance notification request DTO
     */
    public static class MaintenanceNotificationRequest {
        private String message;
        private int delayMinutes;

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public int getDelayMinutes() {
            return delayMinutes;
        }

        public void setDelayMinutes(int delayMinutes) {
            this.delayMinutes = delayMinutes;
        }
    }

    /**
     * Channel message request DTO
     */
    public static class ChannelMessageRequest {
        private String messageType;
        private Map<String, Object> data;

        public String getMessageType() {
            return messageType;
        }

        public void setMessageType(String messageType) {
            this.messageType = messageType;
        }

        public Map<String, Object> getData() {
            return data;
        }

        public void setData(Map<String, Object> data) {
            this.data = data;
        }
    }

    /**
     * Test job status request DTO
     */
    public static class TestJobStatusRequest {
        private String status;
        private Map<String, Object> data;

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public Map<String, Object> getData() {
            return data;
        }

        public void setData(Map<String, Object> data) {
            this.data = data;
        }
    }
}