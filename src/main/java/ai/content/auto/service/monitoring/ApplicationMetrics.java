package ai.content.auto.service.monitoring;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

/**
 * Application-specific performance metrics
 */
@Data
@Builder
public class ApplicationMetrics {

    private Instant timestamp;

    // Component metrics
    private Map<String, Object> redisMetrics;
    private Map<String, Object> databaseMetrics;
    private Map<String, Object> apiMetrics;

    // Application-specific metrics
    private long totalContentGenerated;
    private long activeUsers;
    private double averageResponseTime;
    private double errorRate;

    // Performance indicators
    private double throughput; // requests per second
    private double availability; // percentage uptime
    private double reliability; // success rate
}