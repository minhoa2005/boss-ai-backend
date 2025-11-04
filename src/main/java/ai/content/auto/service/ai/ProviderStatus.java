package ai.content.auto.service.ai;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Represents the current status of an AI provider
 */
@Data
@Builder
public class ProviderStatus {

    /**
     * Provider name
     */
    private String name;

    /**
     * Whether the provider is currently available
     */
    private boolean isAvailable;

    /**
     * Detailed health status
     */
    private ProviderHealthStatus healthStatus;

    /**
     * Provider capabilities
     */
    private ProviderCapabilities capabilities;

    /**
     * Cost per token
     */
    private BigDecimal costPerToken;

    /**
     * Average response time in milliseconds
     */
    private long averageResponseTime;

    /**
     * Success rate (0.0 to 1.0)
     */
    private double successRate;

    /**
     * Quality score (0.0 to 10.0)
     */
    private double qualityScore;

    /**
     * Current load (0.0 to 1.0)
     */
    private double currentLoad;

    /**
     * Whether the circuit breaker is open
     */
    private boolean circuitBreakerOpen;

    /**
     * Number of consecutive failures
     */
    private int consecutiveFailures;
}