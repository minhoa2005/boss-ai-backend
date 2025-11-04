package ai.content.auto.service.ai;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Provider performance analysis data
 */
@Data
@Builder
public class ProviderPerformanceAnalysis {
    private String providerName;
    private int analysisHours;
    private Instant analysisStartTime;
    private Instant analysisEndTime;

    // Performance metrics
    private double averageSuccessRate;
    private long averageResponseTime;
    private double averageQualityScore;
    private long totalRequests;
    private long failedRequests;

    // Trend analysis
    private List<PerformanceTrend> performanceTrends;
    private String trendDirection; // IMPROVING, STABLE, DEGRADING

    // Risk assessment
    private String riskLevel; // LOW, MEDIUM, HIGH, CRITICAL
    private List<String> riskFactors;
    private List<String> recommendations;

    // Predictions
    private PredictionResult predictions;

    // Additional metrics
    private Map<String, Object> additionalMetrics;

    public String getRiskLevel() {
        return riskLevel;
    }
}

/**
 * Performance trend data point
 */
@Data
@Builder
class PerformanceTrend {
    private Instant timestamp;
    private double successRate;
    private long responseTime;
    private double qualityScore;
    private long requestCount;
    private String trendIndicator; // UP, DOWN, STABLE
}

/**
 * Performance prediction results
 */
@Data
@Builder
class PredictionResult {
    private double predictedSuccessRate;
    private long predictedResponseTime;
    private double predictedQualityScore;
    private String confidenceLevel; // HIGH, MEDIUM, LOW
    private List<String> predictionFactors;
}