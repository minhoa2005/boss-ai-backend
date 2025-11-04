package ai.content.auto.websocket;

import ai.content.auto.entity.User;
import ai.content.auto.service.JwtService;
import ai.content.auto.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.net.URI;
import java.util.Map;
import java.util.Optional;

/**
 * WebSocket authentication interceptor
 * Validates JWT tokens and sets user information in session attributes
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    private final JwtService jwtService;
    private final UserService userService;

    @Override
    public boolean beforeHandshake(
            @NonNull ServerHttpRequest request,
            @NonNull ServerHttpResponse response,
            @NonNull WebSocketHandler wsHandler,
            @NonNull Map<String, Object> attributes) throws Exception {

        try {
            // Extract JWT token from query parameters or headers
            String token = extractTokenFromRequest(request);

            if (!StringUtils.hasText(token)) {
                log.warn("WebSocket handshake rejected - no token provided");
                return false;
            }

            // Validate JWT token
            if (!jwtService.validateToken(token)) {
                log.warn("WebSocket handshake rejected - invalid token");
                return false;
            }

            // Extract username from token
            String username = jwtService.getUsernameFromToken(token);
            if (!StringUtils.hasText(username)) {
                log.warn("WebSocket handshake rejected - no username in token");
                return false;
            }

            // Get user from database
            Optional<User> userOptional = userService.findByUsername(username);
            if (userOptional.isEmpty()) {
                log.warn("WebSocket handshake rejected - user not found: {}", username);
                return false;
            }

            User user = userOptional.get();

            // Check if user is active
            if (!Boolean.TRUE.equals(user.getIsActive())) {
                log.warn("WebSocket handshake rejected - user inactive: {}", username);
                return false;
            }

            // Store user information in session attributes
            attributes.put("userId", user.getId());
            attributes.put("username", user.getUsername());
            attributes.put("email", user.getEmail());

            log.info("WebSocket handshake successful for user: {} (ID: {})", username, user.getId());
            return true;

        } catch (Exception e) {
            log.error("Error during WebSocket handshake authentication", e);
            return false;
        }
    }

    @Override
    public void afterHandshake(
            @NonNull ServerHttpRequest request,
            @NonNull ServerHttpResponse response,
            @NonNull WebSocketHandler wsHandler,
            @Nullable Exception exception) {

        if (exception != null) {
            log.error("WebSocket handshake completed with error", exception);
        } else {
            log.debug("WebSocket handshake completed successfully");
        }
    }

    /**
     * Extract JWT token from request
     * Supports both query parameter (?token=...) and Authorization header
     */
    private String extractTokenFromRequest(ServerHttpRequest request) {
        // Try to get token from query parameters first
        URI uri = request.getURI();
        String query = uri.getQuery();

        if (StringUtils.hasText(query)) {
            String token = extractTokenFromQuery(query);
            if (StringUtils.hasText(token)) {
                return token;
            }
        }

        // Try to get token from Authorization header
        String authHeader = request.getHeaders().getFirst("Authorization");
        if (StringUtils.hasText(authHeader) && authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7); // Remove "Bearer " prefix
        }

        return null;
    }

    /**
     * Extract token from query string
     */
    private String extractTokenFromQuery(String query) {
        String[] params = query.split("&");
        for (String param : params) {
            if (param.startsWith("token=")) {
                return param.substring(6); // Remove "token=" prefix
            }
        }
        return null;
    }
}