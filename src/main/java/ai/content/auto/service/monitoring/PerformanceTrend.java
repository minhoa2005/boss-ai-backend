package ai.content.auto.service.monitoring;

import lombok.Builder;
import lombok.Data;

/**
 * Performance trend information
 */
@Data
@Builder
public class PerformanceTrend {

    private String metric;
    private SystemMonitoringService.TrendDirection trend;
    private double change; // percentage change
    private String description;

    // Trend analysis
    private double slope; // Rate of change
    private double confidence; // 0.0 to 1.0
    private String timeframe; // Period analyzed

    // Predictions
    private double predictedValue;
    private String prediction; // Future trend prediction
}