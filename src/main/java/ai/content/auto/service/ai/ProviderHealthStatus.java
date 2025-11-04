package ai.content.auto.service.ai;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

/**
 * Represents the health status of an AI provider
 */
@Data
@Builder
public class ProviderHealthStatus {

    /**
     * Overall health status
     */
    private HealthLevel healthLevel;

    /**
     * Whether the provider is currently available
     */
    private boolean isAvailable;

    /**
     * Last successful request timestamp
     */
    private Instant lastSuccessfulRequest;

    /**
     * Last failed request timestamp
     */
    private Instant lastFailedRequest;

    /**
     * Number of consecutive failures
     */
    private int consecutiveFailures;

    /**
     * Current response time in milliseconds
     */
    private long currentResponseTime;

    /**
     * Error rate in the last hour (0.0 to 1.0)
     */
    private double errorRate;

    /**
     * Additional health metrics
     */
    private Map<String, Object> additionalMetrics;

    /**
     * Health check message
     */
    private String message;

    /**
     * Last health check timestamp
     */
    private Instant lastHealthCheck;

    public enum HealthLevel {
        HEALTHY, // Provider is working normally
        DEGRADED, // Provider is working but with issues
        UNHEALTHY, // Provider has significant issues
        DOWN // Provider is completely unavailable
    }
}