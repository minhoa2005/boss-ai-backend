package ai.content.auto.service.monitoring;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Usage pattern analysis results
 */
@Data
@Builder
public class UsagePatternAnalysis {
    private int analysisWindow; // days
    private Instant timestamp;
    private Map<String, List<Double>> historicalMetrics;
    private Map<String, UsagePattern> patterns;
    private Map<String, ResourcePrediction> predictions;
    private double confidence; // 0.0 to 1.0
}