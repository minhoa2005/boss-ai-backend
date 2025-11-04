package ai.content.auto.service.monitoring;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Cache optimization recommendations
 */
@Data
@Builder
public class CacheOptimizationRecommendations {
    private boolean shouldCleanupExpiredKeys;
    private boolean shouldOptimizeTtl;
    private boolean shouldIncreaseMemory;
    private boolean shouldAddMoreInstances;
    private List<String> recommendations;
}