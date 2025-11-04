package ai.content.auto.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for JobStatusWebSocketHandler
 */
@ExtendWith(MockitoExtension.class)
class JobStatusWebSocketHandlerTest {

    @Mock
    private WebSocketSession mockSession;

    private JobStatusWebSocketHandler webSocketHandler;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        webSocketHandler = new JobStatusWebSocketHandler(objectMapper);
    }

    @Test
    void testAfterConnectionEstablished_WithValidUserId() throws Exception {
        // Arrange
        Long userId = 1L;
        String username = "testuser";
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("userId", userId);
        attributes.put("username", username);

        when(mockSession.getAttributes()).thenReturn(attributes);
        when(mockSession.isOpen()).thenReturn(true);

        // Act
        webSocketHandler.afterConnectionEstablished(mockSession);

        // Assert
        verify(mockSession).sendMessage(any(TextMessage.class));
        assertTrue(webSocketHandler.isUserConnected(userId));
        assertEquals(1, webSocketHandler.getActiveConnectionCount());
    }

    @Test
    void testAfterConnectionEstablished_WithoutUserId() throws Exception {
        // Arrange
        Map<String, Object> attributes = new HashMap<>();
        when(mockSession.getAttributes()).thenReturn(attributes);

        // Act
        webSocketHandler.afterConnectionEstablished(mockSession);

        // Assert
        verify(mockSession).close(any(CloseStatus.class));
        assertEquals(0, webSocketHandler.getActiveConnectionCount());
    }

    @Test
    void testHandleMessage_SubscribeJob() throws Exception {
        // Arrange
        Long userId = 1L;
        String jobId = "job-123";
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("userId", userId);
        attributes.put("username", "testuser");

        when(mockSession.getAttributes()).thenReturn(attributes);
        when(mockSession.isOpen()).thenReturn(true);

        // Establish connection first
        webSocketHandler.afterConnectionEstablished(mockSession);

        // Create subscription message
        Map<String, Object> message = Map.of(
                "type", "subscribe_job",
                "jobId", jobId);
        String messageJson = objectMapper.writeValueAsString(message);
        TextMessage textMessage = new TextMessage(messageJson);

        // Act
        webSocketHandler.handleMessage(mockSession, textMessage);

        // Assert
        assertEquals(1, webSocketHandler.getActiveSubscriptionCount());
        verify(mockSession, atLeast(2)).sendMessage(any(TextMessage.class)); // Connection + subscription confirmation
    }

    @Test
    void testSendJobStatusUpdate() throws Exception {
        // Arrange
        Long userId = 1L;
        String jobId = "job-123";
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("userId", userId);
        attributes.put("username", "testuser");

        when(mockSession.getAttributes()).thenReturn(attributes);
        when(mockSession.isOpen()).thenReturn(true);

        // Establish connection and subscribe to job
        webSocketHandler.afterConnectionEstablished(mockSession);

        Map<String, Object> subscribeMessage = Map.of(
                "type", "subscribe_job",
                "jobId", jobId);
        String messageJson = objectMapper.writeValueAsString(subscribeMessage);
        webSocketHandler.handleMessage(mockSession, new TextMessage(messageJson));

        // Act
        Map<String, Object> statusUpdate = Map.of("status", "processing", "progress", 50);
        webSocketHandler.sendJobStatusUpdate(jobId, statusUpdate);

        // Assert
        verify(mockSession, atLeast(3)).sendMessage(any(TextMessage.class)); // Connection + subscription + status
                                                                             // update
    }

    @Test
    void testSendJobCompletionNotification() throws Exception {
        // Arrange
        Long userId = 1L;
        String jobId = "job-123";
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("userId", userId);
        attributes.put("username", "testuser");

        when(mockSession.getAttributes()).thenReturn(attributes);
        when(mockSession.isOpen()).thenReturn(true);

        // Establish connection
        webSocketHandler.afterConnectionEstablished(mockSession);

        // Act
        Map<String, Object> result = Map.of("success", true, "data", "completed");
        webSocketHandler.sendJobCompletionNotification(userId, jobId, result);

        // Assert
        verify(mockSession, atLeast(2)).sendMessage(any(TextMessage.class)); // Connection + completion
    }

    @Test
    void testBroadcastSystemNotification() throws Exception {
        // Arrange
        Long userId1 = 1L;
        Long userId2 = 2L;

        WebSocketSession mockSession2 = mock(WebSocketSession.class);

        Map<String, Object> attributes1 = new HashMap<>();
        attributes1.put("userId", userId1);
        attributes1.put("username", "user1");

        Map<String, Object> attributes2 = new HashMap<>();
        attributes2.put("userId", userId2);
        attributes2.put("username", "user2");

        when(mockSession.getAttributes()).thenReturn(attributes1);
        when(mockSession.isOpen()).thenReturn(true);
        when(mockSession2.getAttributes()).thenReturn(attributes2);
        when(mockSession2.isOpen()).thenReturn(true);

        // Establish connections
        webSocketHandler.afterConnectionEstablished(mockSession);
        webSocketHandler.afterConnectionEstablished(mockSession2);

        // Act
        webSocketHandler.broadcastSystemNotification("System maintenance", "warning");

        // Assert
        verify(mockSession, atLeast(2)).sendMessage(any(TextMessage.class)); // Connection + broadcast
        verify(mockSession2, atLeast(2)).sendMessage(any(TextMessage.class)); // Connection + broadcast
    }

    @Test
    void testAfterConnectionClosed() throws Exception {
        // Arrange
        Long userId = 1L;
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("userId", userId);
        attributes.put("username", "testuser");

        when(mockSession.getAttributes()).thenReturn(attributes);
        when(mockSession.isOpen()).thenReturn(true);

        // Establish connection
        webSocketHandler.afterConnectionEstablished(mockSession);
        assertTrue(webSocketHandler.isUserConnected(userId));

        // Act
        webSocketHandler.afterConnectionClosed(mockSession, CloseStatus.NORMAL);

        // Assert
        assertFalse(webSocketHandler.isUserConnected(userId));
        assertEquals(0, webSocketHandler.getActiveConnectionCount());
    }

    @Test
    void testGetConnectionStats() throws Exception {
        // Arrange
        Long userId = 1L;
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("userId", userId);
        attributes.put("username", "testuser");

        when(mockSession.getAttributes()).thenReturn(attributes);
        when(mockSession.isOpen()).thenReturn(true);

        // Act
        webSocketHandler.afterConnectionEstablished(mockSession);

        // Assert
        assertEquals(1, webSocketHandler.getActiveConnectionCount());
        assertEquals(0, webSocketHandler.getActiveSubscriptionCount());
        assertEquals(0, webSocketHandler.getActiveChannelSubscriptionCount());
    }
}