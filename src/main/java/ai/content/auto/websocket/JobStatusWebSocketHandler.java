package ai.content.auto.websocket;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * WebSocket handler for real-time job status updates
 * Provides user-specific channels, connection management, and message
 * broadcasting
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JobStatusWebSocketHandler implements WebSocketHandler {

    private final ObjectMapper objectMapper;

    // Store active WebSocket sessions by user ID
    private final Map<Long, WebSocketSession> userSessions = new ConcurrentHashMap<>();

    // Store job subscriptions by job ID -> user ID
    private final Map<String, Long> jobSubscriptions = new ConcurrentHashMap<>();

    // Store user-specific channels by user ID -> set of channel names
    private final Map<Long, ConcurrentHashMap<String, Boolean>> userChannels = new ConcurrentHashMap<>();

    // Connection heartbeat scheduler
    private final ScheduledExecutorService heartbeatScheduler = Executors.newScheduledThreadPool(2);

    // TypeReference for JSON parsing
    private static final TypeReference<Map<String, Object>> MAP_TYPE_REF = new TypeReference<Map<String, Object>>() {
    };

    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) throws Exception {
        try {
            // Extract user ID from session attributes (set by WebSocketAuthInterceptor)
            Long userId = extractUserIdFromSession(session);
            String username = (String) session.getAttributes().get("username");

            if (userId != null) {
                // Store session
                userSessions.put(userId, session);

                // Initialize user channels
                userChannels.put(userId, new ConcurrentHashMap<>());

                log.info("WebSocket connection established for user: {} ({})", username, userId);

                // Send connection confirmation
                sendMessage(session, Map.of(
                        "type", "connection_established",
                        "userId", userId,
                        "username", username,
                        "timestamp", System.currentTimeMillis(),
                        "message", "WebSocket connection established successfully"));

                // Start heartbeat for this connection
                startHeartbeat(session, userId);

            } else {
                log.warn("WebSocket connection rejected - no valid user ID in session attributes");
                session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Authentication required"));
            }

        } catch (Exception e) {
            log.error("Error establishing WebSocket connection", e);
            session.close(CloseStatus.SERVER_ERROR.withReason("Connection error"));
        }
    }

    @Override
    public void handleMessage(@NonNull WebSocketSession session, @NonNull WebSocketMessage<?> message)
            throws Exception {
        try {
            String payload = message.getPayload().toString();
            Map<String, Object> messageData = objectMapper.readValue(payload, MAP_TYPE_REF);

            String messageType = (String) messageData.get("type");
            Long userId = extractUserIdFromSession(session);

            if (userId == null) {
                log.warn("Received message from unauthenticated session");
                sendErrorMessage(session, "Authentication required");
                return;
            }

            log.debug("Received WebSocket message from user {}: {}", userId, messageType);

            switch (messageType) {
                case "subscribe_job":
                    handleJobSubscription(userId, (String) messageData.get("jobId"));
                    break;
                case "unsubscribe_job":
                    handleJobUnsubscription((String) messageData.get("jobId"));
                    break;
                case "subscribe_channel":
                    handleChannelSubscription(userId, (String) messageData.get("channel"));
                    break;
                case "unsubscribe_channel":
                    handleChannelUnsubscription(userId, (String) messageData.get("channel"));
                    break;
                case "ping":
                    handlePing(session, userId);
                    break;
                case "get_status":
                    handleStatusRequest(session, userId);
                    break;
                default:
                    log.warn("Unknown message type from user {}: {}", userId, messageType);
                    sendErrorMessage(session, "Unknown message type: " + messageType);
            }

        } catch (Exception e) {
            log.error("Error handling WebSocket message", e);
            sendErrorMessage(session, "Message processing error");
        }
    }

    @Override
    public void handleTransportError(@NonNull WebSocketSession session, @NonNull Throwable exception) throws Exception {
        Long userId = extractUserIdFromSession(session);
        String username = (String) session.getAttributes().get("username");

        log.error("WebSocket transport error for user: {} ({})", username, userId, exception);

        // Clean up resources
        cleanupUserSession(userId);

        // Close session if still open
        if (session.isOpen()) {
            try {
                session.close(CloseStatus.SERVER_ERROR);
            } catch (Exception e) {
                log.warn("Error closing WebSocket session after transport error", e);
            }
        }
    }

    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus closeStatus)
            throws Exception {
        Long userId = extractUserIdFromSession(session);
        String username = (String) session.getAttributes().get("username");

        if (userId != null) {
            // Clean up all user-related data
            cleanupUserSession(userId);

            log.info("WebSocket connection closed for user: {} ({}) with status: {}",
                    username, userId, closeStatus);
        }
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    /**
     * Send job status update to subscribed user
     */
    public void sendJobStatusUpdate(String jobId, Map<String, Object> statusUpdate) {
        Long userId = jobSubscriptions.get(jobId);

        if (userId != null) {
            WebSocketSession session = userSessions.get(userId);

            if (session != null && session.isOpen()) {
                try {
                    Map<String, Object> message = Map.of(
                            "type", "job_status_update",
                            "jobId", jobId,
                            "timestamp", System.currentTimeMillis(),
                            "data", statusUpdate);

                    sendMessage(session, message);
                    log.debug("Sent job status update for job: {} to user: {}", jobId, userId);

                } catch (Exception e) {
                    log.error("Failed to send job status update for job: {} to user: {}", jobId, userId, e);
                    // Clean up stale subscription on error
                    cleanupJobSubscription(jobId, userId);
                }
            } else {
                // Clean up stale subscription
                cleanupJobSubscription(jobId, userId);
            }
        }
    }

    /**
     * Send job completion notification to user
     */
    public void sendJobCompletionNotification(Long userId, String jobId, Map<String, Object> result) {
        WebSocketSession session = userSessions.get(userId);

        if (session != null && session.isOpen()) {
            try {
                Map<String, Object> message = Map.of(
                        "type", "job_completed",
                        "jobId", jobId,
                        "timestamp", System.currentTimeMillis(),
                        "result", result);

                sendMessage(session, message);
                log.info("Sent job completion notification for job: {} to user: {}", jobId, userId);

                // Clean up job subscription after completion
                jobSubscriptions.remove(jobId);

            } catch (Exception e) {
                log.error("Failed to send job completion notification for job: {} to user: {}", jobId, userId, e);
            }
        } else {
            log.warn("Cannot send job completion notification - user {} not connected", userId);
        }
    }

    /**
     * Broadcast system notification to all connected users
     */
    public void broadcastSystemNotification(String message, String level) {
        Map<String, Object> notification = Map.of(
                "type", "system_notification",
                "message", message,
                "level", level,
                "timestamp", System.currentTimeMillis());

        int successCount = 0;
        int failureCount = 0;

        for (Map.Entry<Long, WebSocketSession> entry : userSessions.entrySet()) {
            WebSocketSession session = entry.getValue();
            Long userId = entry.getKey();

            if (session.isOpen()) {
                try {
                    sendMessage(session, notification);
                    successCount++;
                } catch (Exception e) {
                    log.error("Failed to send system notification to user: {}", userId, e);
                    failureCount++;
                }
            } else {
                // Clean up closed session
                cleanupUserSession(userId);
                failureCount++;
            }
        }

        log.info("Broadcasted system notification: {} (level: {}) - Success: {}, Failures: {}",
                message, level, successCount, failureCount);
    }

    /**
     * Send message to specific user channel
     */
    public void sendToUserChannel(Long userId, String channel, Map<String, Object> messageData) {
        WebSocketSession session = userSessions.get(userId);
        ConcurrentHashMap<String, Boolean> channels = userChannels.get(userId);

        if (session != null && session.isOpen() && channels != null && channels.containsKey(channel)) {
            try {
                Map<String, Object> message = Map.of(
                        "type", "channel_message",
                        "channel", channel,
                        "timestamp", System.currentTimeMillis(),
                        "data", messageData);

                sendMessage(session, message);
                log.debug("Sent message to user {} on channel: {}", userId, channel);

            } catch (Exception e) {
                log.error("Failed to send message to user {} on channel: {}", userId, channel, e);
            }
        }
    }

    /**
     * Broadcast message to all users subscribed to a channel
     */
    public void broadcastToChannel(String channel, Map<String, Object> messageData) {
        Map<String, Object> message = Map.of(
                "type", "channel_broadcast",
                "channel", channel,
                "timestamp", System.currentTimeMillis(),
                "data", messageData);

        int successCount = 0;
        int failureCount = 0;

        for (Map.Entry<Long, ConcurrentHashMap<String, Boolean>> entry : userChannels.entrySet()) {
            Long userId = entry.getKey();
            ConcurrentHashMap<String, Boolean> channels = entry.getValue();

            if (channels.containsKey(channel)) {
                WebSocketSession session = userSessions.get(userId);
                if (session != null && session.isOpen()) {
                    try {
                        sendMessage(session, message);
                        successCount++;
                    } catch (Exception e) {
                        log.error("Failed to broadcast to user {} on channel: {}", userId, channel, e);
                        failureCount++;
                    }
                } else {
                    cleanupUserSession(userId);
                    failureCount++;
                }
            }
        }

        log.debug("Broadcasted to channel: {} - Success: {}, Failures: {}", channel, successCount, failureCount);
    }

    /**
     * Get count of active WebSocket connections
     */
    public int getActiveConnectionCount() {
        // Clean up stale sessions
        userSessions.entrySet().removeIf(entry -> !entry.getValue().isOpen());
        return userSessions.size();
    }

    /**
     * Get count of active job subscriptions
     */
    public int getActiveSubscriptionCount() {
        return jobSubscriptions.size();
    }

    /**
     * Get count of active channel subscriptions
     */
    public int getActiveChannelSubscriptionCount() {
        return userChannels.values().stream()
                .mapToInt(channels -> channels.size())
                .sum();
    }

    /**
     * Check if user is connected
     */
    public boolean isUserConnected(Long userId) {
        WebSocketSession session = userSessions.get(userId);
        return session != null && session.isOpen();
    }

    private void handleJobSubscription(Long userId, String jobId) {
        if (jobId != null && !jobId.trim().isEmpty()) {
            jobSubscriptions.put(jobId, userId);
            log.debug("User {} subscribed to job: {}", userId, jobId);

            // Send subscription confirmation
            WebSocketSession session = userSessions.get(userId);
            if (session != null && session.isOpen()) {
                try {
                    sendMessage(session, Map.of(
                            "type", "job_subscription_confirmed",
                            "jobId", jobId,
                            "timestamp", System.currentTimeMillis()));
                } catch (Exception e) {
                    log.error("Failed to send job subscription confirmation", e);
                }
            }
        } else {
            log.warn("Invalid job ID for subscription from user: {}", userId);
        }
    }

    private void handleJobUnsubscription(String jobId) {
        if (jobId != null && !jobId.trim().isEmpty()) {
            Long userId = jobSubscriptions.remove(jobId);
            log.debug("Unsubscribed from job: {} (user: {})", jobId, userId);

            // Send unsubscription confirmation if user is still connected
            if (userId != null) {
                WebSocketSession session = userSessions.get(userId);
                if (session != null && session.isOpen()) {
                    try {
                        sendMessage(session, Map.of(
                                "type", "job_unsubscription_confirmed",
                                "jobId", jobId,
                                "timestamp", System.currentTimeMillis()));
                    } catch (Exception e) {
                        log.error("Failed to send job unsubscription confirmation", e);
                    }
                }
            }
        }
    }

    private void handleChannelSubscription(Long userId, String channel) {
        if (channel != null && !channel.trim().isEmpty()) {
            ConcurrentHashMap<String, Boolean> channels = userChannels.computeIfAbsent(userId,
                    k -> new ConcurrentHashMap<>());
            channels.put(channel, true);

            log.debug("User {} subscribed to channel: {}", userId, channel);

            // Send subscription confirmation
            WebSocketSession session = userSessions.get(userId);
            if (session != null && session.isOpen()) {
                try {
                    sendMessage(session, Map.of(
                            "type", "channel_subscription_confirmed",
                            "channel", channel,
                            "timestamp", System.currentTimeMillis()));
                } catch (Exception e) {
                    log.error("Failed to send channel subscription confirmation", e);
                }
            }
        } else {
            log.warn("Invalid channel name for subscription from user: {}", userId);
        }
    }

    private void handleChannelUnsubscription(Long userId, String channel) {
        if (channel != null && !channel.trim().isEmpty()) {
            ConcurrentHashMap<String, Boolean> channels = userChannels.get(userId);
            if (channels != null) {
                channels.remove(channel);
                log.debug("User {} unsubscribed from channel: {}", userId, channel);

                // Send unsubscription confirmation
                WebSocketSession session = userSessions.get(userId);
                if (session != null && session.isOpen()) {
                    try {
                        sendMessage(session, Map.of(
                                "type", "channel_unsubscription_confirmed",
                                "channel", channel,
                                "timestamp", System.currentTimeMillis()));
                    } catch (Exception e) {
                        log.error("Failed to send channel unsubscription confirmation", e);
                    }
                }
            }
        }
    }

    private void handlePing(WebSocketSession session, Long userId) {
        try {
            sendMessage(session, Map.of(
                    "type", "pong",
                    "userId", userId,
                    "timestamp", System.currentTimeMillis()));
        } catch (Exception e) {
            log.error("Failed to send pong response to user: {}", userId, e);
        }
    }

    private void handleStatusRequest(WebSocketSession session, Long userId) {
        try {
            // Get user's job subscriptions
            long jobCount = jobSubscriptions.entrySet().stream()
                    .filter(entry -> entry.getValue().equals(userId))
                    .count();

            // Get user's channel subscriptions
            ConcurrentHashMap<String, Boolean> channels = userChannels.get(userId);
            int channelCount = channels != null ? channels.size() : 0;

            sendMessage(session, Map.of(
                    "type", "status_response",
                    "userId", userId,
                    "jobSubscriptions", jobCount,
                    "channelSubscriptions", channelCount,
                    "timestamp", System.currentTimeMillis()));

        } catch (Exception e) {
            log.error("Failed to send status response to user: {}", userId, e);
        }
    }

    private void sendMessage(WebSocketSession session, Map<String, Object> message) throws IOException {
        if (session.isOpen()) {
            String json = objectMapper.writeValueAsString(message);
            synchronized (session) {
                session.sendMessage(new TextMessage(json));
            }
        }
    }

    private void sendErrorMessage(WebSocketSession session, String errorMessage) {
        try {
            sendMessage(session, Map.of(
                    "type", "error",
                    "message", errorMessage,
                    "timestamp", System.currentTimeMillis()));
        } catch (Exception e) {
            log.error("Failed to send error message", e);
        }
    }

    private void startHeartbeat(WebSocketSession session, Long userId) {
        heartbeatScheduler.scheduleAtFixedRate(() -> {
            if (session.isOpen()) {
                try {
                    sendMessage(session, Map.of(
                            "type", "heartbeat",
                            "timestamp", System.currentTimeMillis()));
                } catch (Exception e) {
                    log.debug("Heartbeat failed for user: {}, cleaning up connection", userId);
                    cleanupUserSession(userId);
                }
            } else {
                cleanupUserSession(userId);
            }
        }, 30, 30, TimeUnit.SECONDS); // Send heartbeat every 30 seconds
    }

    private void cleanupUserSession(Long userId) {
        if (userId != null) {
            // Remove user session
            userSessions.remove(userId);

            // Remove user channels
            userChannels.remove(userId);

            // Remove job subscriptions for this user
            jobSubscriptions.entrySet().removeIf(entry -> entry.getValue().equals(userId));

            log.debug("Cleaned up session for user: {}", userId);
        }
    }

    private void cleanupJobSubscription(String jobId, Long userId) {
        jobSubscriptions.remove(jobId);
        log.debug("Cleaned up job subscription: {} for user: {}", jobId, userId);
    }

    private Long extractUserIdFromSession(WebSocketSession session) {
        try {
            // Get user ID from session attributes (set by WebSocketAuthInterceptor)
            Object userIdAttr = session.getAttributes().get("userId");
            if (userIdAttr instanceof Long) {
                return (Long) userIdAttr;
            }

            return null;

        } catch (Exception e) {
            log.error("Error extracting user ID from session", e);
            return null;
        }
    }
}