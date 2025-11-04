package ai.content.auto.example;

import ai.content.auto.service.WebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Example demonstrating WebSocket integration with other services
 * This shows how to integrate WebSocket notifications with content generation
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketIntegrationExample {

    private final WebSocketService webSocketService;

    /**
     * Example: Integrate WebSocket notifications with content generation process
     */
    public void demonstrateContentGenerationIntegration() {
        Long userId = 1L;
        String jobId = "job-123";

        // 1. Send job started notification
        webSocketService.sendJobStatusUpdate(jobId, "STARTED", Map.of(
                "message", "Content generation started",
                "progress", 0));

        // 2. Send progress updates
        webSocketService.sendJobStatusUpdate(jobId, "PROCESSING", Map.of(
                "message", "Analyzing content requirements",
                "progress", 25));

        webSocketService.sendJobStatusUpdate(jobId, "PROCESSING", Map.of(
                "message", "Generating content with AI",
                "progress", 50));

        webSocketService.sendJobStatusUpdate(jobId, "PROCESSING", Map.of(
                "message", "Optimizing content quality",
                "progress", 75));

        // 3. Send completion notification
        webSocketService.sendJobCompletionNotification(userId, jobId, true, Map.of(
                "contentId", "content-456",
                "title", "Generated Content Title",
                "wordCount", 500,
                "qualityScore", 8.5));

        log.info("WebSocket integration example completed for job: {}", jobId);
    }

    /**
     * Example: Send system maintenance notification
     */
    public void demonstrateMaintenanceNotification() {
        webSocketService.sendMaintenanceNotification(
                "System maintenance scheduled in 30 minutes. Please save your work.", 30);

        log.info("Maintenance notification sent to all connected users");
    }

    /**
     * Example: Send user-specific channel message
     */
    public void demonstrateUserChannelMessage() {
        Long userId = 1L;
        String channel = "notifications";

        webSocketService.sendToUserChannel(userId, channel, "QUOTA_WARNING", Map.of(
                "message", "You have used 80% of your monthly quota",
                "remainingCredits", 200,
                "resetDate", "2024-02-01"));

        log.info("User-specific notification sent to user: {} on channel: {}", userId, channel);
    }

    /**
     * Example: Broadcast to all users on a channel
     */
    public void demonstrateChannelBroadcast() {
        String channel = "announcements";

        webSocketService.broadcastToChannel(channel, "NEW_FEATURE", Map.of(
                "title", "New AI Model Available",
                "description", "We've added GPT-4 Turbo for faster content generation",
                "availableFrom", "2024-01-15"));

        log.info("Feature announcement broadcasted to channel: {}", channel);
    }

    /**
     * Example: Send system notification with different levels
     */
    public void demonstrateSystemNotifications() {
        // Info notification
        webSocketService.broadcastSystemNotification(
                "New features have been deployed successfully",
                WebSocketService.NotificationLevel.INFO);

        // Warning notification
        webSocketService.broadcastSystemNotification(
                "High server load detected. Some operations may be slower",
                WebSocketService.NotificationLevel.WARNING);

        // Error notification
        webSocketService.broadcastSystemNotification(
                "AI service temporarily unavailable. Please try again in a few minutes",
                WebSocketService.NotificationLevel.ERROR);

        // Success notification
        webSocketService.broadcastSystemNotification(
                "All systems are operating normally",
                WebSocketService.NotificationLevel.SUCCESS);

        log.info("System notifications sent with different levels");
    }

    /**
     * Example: Check connection status and get statistics
     */
    public void demonstrateConnectionMonitoring() {
        Long userId = 1L;

        // Check if user is connected
        boolean isConnected = webSocketService.isUserConnected(userId);
        log.info("User {} connection status: {}", userId, isConnected ? "Connected" : "Disconnected");

        // Get connection statistics
        WebSocketService.ConnectionStats stats = webSocketService.getConnectionStats();
        log.info("WebSocket Statistics - Active Connections: {}, Job Subscriptions: {}, Channel Subscriptions: {}",
                stats.getActiveConnections(),
                stats.getActiveJobSubscriptions(),
                stats.getActiveChannelSubscriptions());
    }
}