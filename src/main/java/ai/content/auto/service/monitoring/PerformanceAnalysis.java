package ai.content.auto.service.monitoring;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

/**
 * Performance analysis over time
 */
@Data
@Builder
public class PerformanceAnalysis {

    private int analysisWindow; // hours
    private Instant timestamp;

    // Current state
    private Map<String, Object> currentMetrics;

    // Trends analysis
    private Map<String, PerformanceTrend> trends;

    // Recommendations
    private Map<String, String> recommendations;

    // Overall assessment
    private double overallScore; // 0-100
    private String performanceGrade; // A, B, C, D, F

    // Predictions
    private Map<String, Object> predictions;
    private String riskLevel; // LOW, MEDIUM, HIGH
}