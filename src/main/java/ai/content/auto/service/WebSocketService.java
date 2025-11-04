package ai.content.auto.service;

import ai.content.auto.websocket.JobStatusWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * WebSocket service providing high-level interface for real-time communication
 * This service acts as a facade for WebSocket operations
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketService {

    private final JobStatusWebSocketHandler webSocketHandler;

    /**
     * Send job status update to user
     */
    public void sendJobStatusUpdate(String jobId, String status, Map<String, Object> data) {
        try {
            Map<String, Object> statusUpdate = Map.of(
                    "status", status,
                    "data", data != null ? data : Map.of());

            webSocketHandler.sendJobStatusUpdate(jobId, statusUpdate);
            log.debug("Sent job status update: {} - {}", jobId, status);

        } catch (Exception e) {
            log.error("Failed to send job status update for job: {}", jobId, e);
        }
    }

    /**
     * Send job completion notification
     */
    public void sendJobCompletionNotification(Long userId, String jobId, boolean success, Map<String, Object> result) {
        try {
            Map<String, Object> completionData = Map.of(
                    "success", success,
                    "result", result != null ? result : Map.of());

            webSocketHandler.sendJobCompletionNotification(userId, jobId, completionData);
            log.info("Sent job completion notification: {} - Success: {}", jobId, success);

        } catch (Exception e) {
            log.error("Failed to send job completion notification for job: {}", jobId, e);
        }
    }

    /**
     * Send job error notification
     */
    public void sendJobErrorNotification(Long userId, String jobId, String errorMessage,
            Map<String, Object> errorDetails) {
        try {
            Map<String, Object> errorData = Map.of(
                    "success", false,
                    "error", errorMessage,
                    "details", errorDetails != null ? errorDetails : Map.of());

            webSocketHandler.sendJobCompletionNotification(userId, jobId, errorData);
            log.warn("Sent job error notification: {} - Error: {}", jobId, errorMessage);

        } catch (Exception e) {
            log.error("Failed to send job error notification for job: {}", jobId, e);
        }
    }

    /**
     * Send message to user on specific channel
     */
    public void sendToUserChannel(Long userId, String channel, String messageType, Map<String, Object> data) {
        try {
            Map<String, Object> messageData = Map.of(
                    "messageType", messageType,
                    "data", data != null ? data : Map.of());

            webSocketHandler.sendToUserChannel(userId, channel, messageData);
            log.debug("Sent message to user {} on channel {}: {}", userId, channel, messageType);

        } catch (Exception e) {
            log.error("Failed to send message to user {} on channel {}", userId, channel, e);
        }
    }

    /**
     * Broadcast message to all users on a channel
     */
    public void broadcastToChannel(String channel, String messageType, Map<String, Object> data) {
        try {
            Map<String, Object> messageData = Map.of(
                    "messageType", messageType,
                    "data", data != null ? data : Map.of());

            webSocketHandler.broadcastToChannel(channel, messageData);
            log.debug("Broadcasted message to channel {}: {}", channel, messageType);

        } catch (Exception e) {
            log.error("Failed to broadcast message to channel {}", channel, e);
        }
    }

    /**
     * Send system notification to all connected users
     */
    public void broadcastSystemNotification(String message, NotificationLevel level) {
        try {
            webSocketHandler.broadcastSystemNotification(message, level.name().toLowerCase());
            log.info("Broadcasted system notification: {} ({})", message, level);

        } catch (Exception e) {
            log.error("Failed to broadcast system notification", e);
        }
    }

    /**
     * Send system maintenance notification
     */
    public void sendMaintenanceNotification(String message, int delayMinutes) {
        try {
            Map<String, Object> data = Map.of(
                    "delayMinutes", delayMinutes,
                    "scheduledTime", System.currentTimeMillis() + (delayMinutes * 60 * 1000L));

            broadcastToChannel("system", "maintenance_scheduled", data);
            log.info("Sent maintenance notification: {} (delay: {} minutes)", message, delayMinutes);

        } catch (Exception e) {
            log.error("Failed to send maintenance notification", e);
        }
    }

    /**
     * Check if user is connected
     */
    public boolean isUserConnected(Long userId) {
        return webSocketHandler.isUserConnected(userId);
    }

    /**
     * Get connection statistics
     */
    public ConnectionStats getConnectionStats() {
        return ConnectionStats.builder()
                .activeConnections(webSocketHandler.getActiveConnectionCount())
                .activeJobSubscriptions(webSocketHandler.getActiveSubscriptionCount())
                .activeChannelSubscriptions(webSocketHandler.getActiveChannelSubscriptionCount())
                .build();
    }

    /**
     * Notification levels
     */
    public enum NotificationLevel {
        INFO,
        WARNING,
        ERROR,
        SUCCESS
    }

    /**
     * Connection statistics
     */
    public static class ConnectionStats {
        private final int activeConnections;
        private final int activeJobSubscriptions;
        private final int activeChannelSubscriptions;

        private ConnectionStats(int activeConnections, int activeJobSubscriptions, int activeChannelSubscriptions) {
            this.activeConnections = activeConnections;
            this.activeJobSubscriptions = activeJobSubscriptions;
            this.activeChannelSubscriptions = activeChannelSubscriptions;
        }

        public static Builder builder() {
            return new Builder();
        }

        public int getActiveConnections() {
            return activeConnections;
        }

        public int getActiveJobSubscriptions() {
            return activeJobSubscriptions;
        }

        public int getActiveChannelSubscriptions() {
            return activeChannelSubscriptions;
        }

        public static class Builder {
            private int activeConnections;
            private int activeJobSubscriptions;
            private int activeChannelSubscriptions;

            public Builder activeConnections(int activeConnections) {
                this.activeConnections = activeConnections;
                return this;
            }

            public Builder activeJobSubscriptions(int activeJobSubscriptions) {
                this.activeJobSubscriptions = activeJobSubscriptions;
                return this;
            }

            public Builder activeChannelSubscriptions(int activeChannelSubscriptions) {
                this.activeChannelSubscriptions = activeChannelSubscriptions;
                return this;
            }

            public ConnectionStats build() {
                return new ConnectionStats(activeConnections, activeJobSubscriptions, activeChannelSubscriptions);
            }
        }
    }
}