# WebSocket Implementation for Real-time Updates

This document describes the WebSocket implementation for real-time job status updates and system notifications.

## Overview

The WebSocket implementation provides:
- **User-specific channels** for job status updates
- **Connection management** with automatic reconnection support
- **Message broadcasting** for job completion notifications
- **WebSocket authentication and authorization** using JWT tokens
- **Real-time system notifications** and maintenance alerts

## Architecture

### Components

1. **WebSocketConfig** - Configuration for WebSocket endpoints
2. **WebSocketAuthInterceptor** - JWT-based authentication for WebSocket connections
3. **JobStatusWebSocketHandler** - Main WebSocket handler for real-time communication
4. **WebSocketService** - High-level service interface for WebSocket operations
5. **WebSocketController** - REST endpoints for WebSocket management

### Endpoints

- **WebSocket Endpoints:**
  - `/ws/job-status` - Main WebSocket endpoint with SockJS fallback
  - `/ws/job-status-native` - Native WebSocket endpoint (no SockJS)

- **REST Management Endpoints:**
  - `GET /api/v1/websocket/stats` - Get connection statistics (Admin only)
  - `GET /api/v1/websocket/user/{userId}/status` - Check user connection status
  - `POST /api/v1/websocket/broadcast/system` - Broadcast system notification (Admin only)
  - `POST /api/v1/websocket/broadcast/maintenance` - Send maintenance notification (Admin only)

## Authentication

WebSocket connections are authenticated using JWT tokens. The token can be provided in two ways:

1. **Query Parameter:** `?token=your_jwt_token`
2. **Authorization Header:** `Authorization: Bearer your_jwt_token`

The `WebSocketAuthInterceptor` validates the token and extracts user information before allowing the connection.

## Message Types

### Client to Server Messages

```json
{
  "type": "subscribe_job",
  "jobId": "job-123"
}
```

```json
{
  "type": "unsubscribe_job",
  "jobId": "job-123"
}
```

```json
{
  "type": "subscribe_channel",
  "channel": "notifications"
}
```

```json
{
  "type": "ping"
}
```

### Server to Client Messages

#### Connection Established
```json
{
  "type": "connection_established",
  "userId": 1,
  "username": "user@example.com",
  "timestamp": 1640995200000,
  "message": "WebSocket connection established successfully"
}
```

#### Job Status Update
```json
{
  "type": "job_status_update",
  "jobId": "job-123",
  "timestamp": 1640995200000,
  "data": {
    "status": "processing",
    "progress": 50,
    "message": "Generating content..."
  }
}
```

#### Job Completion
```json
{
  "type": "job_completed",
  "jobId": "job-123",
  "timestamp": 1640995200000,
  "result": {
    "success": true,
    "contentId": "content-456",
    "title": "Generated Content"
  }
}
```

#### System Notification
```json
{
  "type": "system_notification",
  "message": "System maintenance in 30 minutes",
  "level": "warning",
  "timestamp": 1640995200000
}
```

## Usage Examples

### Frontend JavaScript Client

```javascript
// Connect to WebSocket with JWT token
const token = localStorage.getItem('accessToken');
const ws = new WebSocket(`ws://localhost:8080/ws/job-status?token=${token}`);

ws.onopen = function(event) {
    console.log('WebSocket connected');
    
    // Subscribe to job updates
    ws.send(JSON.stringify({
        type: 'subscribe_job',
        jobId: 'job-123'
    }));
};

ws.onmessage = function(event) {
    const message = JSON.parse(event.data);
    
    switch(message.type) {
        case 'job_status_update':
            updateJobProgress(message.jobId, message.data);
            break;
        case 'job_completed':
            handleJobCompletion(message.jobId, message.result);
            break;
        case 'system_notification':
            showSystemNotification(message.message, message.level);
            break;
    }
};

ws.onclose = function(event) {
    console.log('WebSocket disconnected');
    // Implement reconnection logic
};
```

### Backend Service Integration

```java
@Service
@RequiredArgsConstructor
public class ContentGenerationService {
    
    private final WebSocketService webSocketService;
    
    public void generateContent(ContentGenerateRequest request) {
        String jobId = UUID.randomUUID().toString();
        Long userId = getCurrentUserId();
        
        // Send job started notification
        webSocketService.sendJobStatusUpdate(jobId, "STARTED", Map.of(
            "message", "Content generation started",
            "progress", 0
        ));
        
        try {
            // Process content generation...
            
            // Send progress updates
            webSocketService.sendJobStatusUpdate(jobId, "PROCESSING", Map.of(
                "message", "Generating content with AI",
                "progress", 50
            ));
            
            // Complete generation
            String contentId = processGeneration(request);
            
            // Send completion notification
            webSocketService.sendJobCompletionNotification(userId, jobId, true, Map.of(
                "contentId", contentId,
                "title", "Generated Content"
            ));
            
        } catch (Exception e) {
            // Send error notification
            webSocketService.sendJobErrorNotification(userId, jobId, 
                "Generation failed: " + e.getMessage(), Map.of());
        }
    }
}
```

## Features

### Connection Management
- **Automatic heartbeat** every 30 seconds
- **Connection cleanup** on disconnect
- **Session management** with user mapping
- **Reconnection support** with connection state preservation

### Security
- **JWT authentication** for all connections
- **User authorization** based on roles
- **Session validation** and cleanup
- **CORS configuration** for cross-origin requests

### Scalability
- **Concurrent connection support** for thousands of users
- **Efficient message broadcasting** with targeted delivery
- **Memory management** with automatic cleanup
- **Performance monitoring** and statistics

### Error Handling
- **Graceful connection failures** with proper cleanup
- **Message delivery guarantees** with retry logic
- **Transport error recovery** with automatic reconnection
- **Comprehensive logging** for debugging

## Configuration

### Application Properties

```yaml
# WebSocket configuration is handled automatically
# CORS settings apply to WebSocket endpoints
app:
  cors:
    allowed-origins: "http://localhost:3000,http://localhost:5173"
    allowed-methods: "GET,POST,PUT,DELETE,OPTIONS"
    allow-credentials: true
```

### Security Configuration

WebSocket endpoints are configured in `SecurityConfig.java`:

```java
// WebSocket endpoints (authentication handled by WebSocketAuthInterceptor)
.requestMatchers("/ws/**").permitAll()
```

## Monitoring and Management

### Connection Statistics

Get real-time statistics about WebSocket connections:

```bash
curl -H "Authorization: Bearer $JWT_TOKEN" \
     http://localhost:8080/api/v1/websocket/stats
```

Response:
```json
{
  "errorMessage": "WebSocket statistics retrieved successfully",
  "data": {
    "activeConnections": 25,
    "activeJobSubscriptions": 15,
    "activeChannelSubscriptions": 40
  }
}
```

### System Notifications

Send system-wide notifications:

```bash
curl -X POST \
     -H "Authorization: Bearer $ADMIN_JWT_TOKEN" \
     -H "Content-Type: application/json" \
     -d '{"message": "System maintenance in 30 minutes", "level": "warning"}' \
     http://localhost:8080/api/v1/websocket/broadcast/system
```

## Testing

The implementation includes comprehensive unit tests in `JobStatusWebSocketHandlerTest.java` covering:

- Connection establishment and authentication
- Message handling and routing
- Job subscription management
- System notification broadcasting
- Connection cleanup and error handling

## Future Enhancements

1. **Message Persistence** - Store messages for offline users
2. **Message Acknowledgment** - Ensure message delivery
3. **Rate Limiting** - Prevent message spam
4. **Message Filtering** - Advanced subscription management
5. **Clustering Support** - Multi-instance WebSocket support
6. **Metrics Integration** - Prometheus metrics for monitoring

## Troubleshooting

### Common Issues

1. **Connection Refused**
   - Check JWT token validity
   - Verify CORS configuration
   - Ensure WebSocket endpoint is accessible

2. **Authentication Failed**
   - Validate JWT token format
   - Check token expiration
   - Verify user exists and is active

3. **Messages Not Received**
   - Check job subscription status
   - Verify WebSocket connection is open
   - Check server logs for errors

### Debug Logging

Enable debug logging for WebSocket components:

```yaml
logging:
  level:
    ai.content.auto.websocket: DEBUG
    ai.content.auto.service.WebSocketService: DEBUG
```

This will provide detailed information about connection management, message routing, and error handling.