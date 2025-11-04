package ai.content.auto.service.monitoring;

import lombok.Builder;
import lombok.Data;

/**
 * Individual scaling recommendation
 */
@Data
@Builder
public class ScalingRecommendation {
    private String resourceType; // CPU, MEMORY, DATABASE, etc.
    private String action; // SCALE_UP, SCALE_DOWN, MAINTAIN
    private double currentValue;
    private double predictedValue;
    private String recommendation;
    private String priority; // LOW, MEDIUM, HIGH
    private double estimatedCost;
}