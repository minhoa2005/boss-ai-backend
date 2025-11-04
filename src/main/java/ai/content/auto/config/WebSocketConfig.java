package ai.content.auto.config;

import ai.content.auto.websocket.JobStatusWebSocketHandler;
import ai.content.auto.websocket.WebSocketAuthInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket configuration for real-time updates
 * Configures WebSocket endpoints and authentication
 */
@Slf4j
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final JobStatusWebSocketHandler jobStatusWebSocketHandler;
    private final WebSocketAuthInterceptor webSocketAuthInterceptor;

    @Override
    public void registerWebSocketHandlers(@NonNull WebSocketHandlerRegistry registry) {
        // Register WebSocket handler for job status updates
        registry.addHandler(jobStatusWebSocketHandler, "/ws/job-status")
                .addInterceptors(webSocketAuthInterceptor)
                .setAllowedOrigins("*") // Configure based on your CORS settings
                .withSockJS(); // Enable SockJS fallback for browsers that don't support WebSocket

        // Register WebSocket handler without SockJS for native WebSocket clients
        registry.addHandler(jobStatusWebSocketHandler, "/ws/job-status-native")
                .addInterceptors(webSocketAuthInterceptor)
                .setAllowedOrigins("*");

        log.info("WebSocket handlers registered successfully");
    }
}