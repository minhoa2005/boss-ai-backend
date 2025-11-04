package ai.content.auto.service.monitoring;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * Cache optimization result
 */
@Data
@Builder
public class CacheOptimizationResult {
    private Instant timestamp;
    private int optimizationsApplied;
    private CacheMetrics beforeMetrics;
    private CacheMetrics afterMetrics;
    private String estimatedImprovement;
    private boolean success;
    private String errorMessage;
}