package ai.content.auto.service.monitoring;

import lombok.Builder;
import lombok.Data;

/**
 * Resource prediction information
 */
@Data
@Builder
public class ResourcePrediction {
    private String resourceType;
    private double currentValue;
    private double predictedValue;
    private double confidence; // 0.0 to 1.0
    private String timeframe; // "7 days", "1 month", etc.
}