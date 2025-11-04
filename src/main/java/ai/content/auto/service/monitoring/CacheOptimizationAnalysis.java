package ai.content.auto.service.monitoring;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

/**
 * Cache optimization analysis results
 */
@Data
@Builder
public class CacheOptimizationAnalysis {
    private Instant timestamp;
    private CacheMetrics currentMetrics;
    private double hitRatio;
    private Map<String, Long> hotKeys;
    private CacheOptimizationRecommendations recommendations;
    private double overallScore; // 0-100
}