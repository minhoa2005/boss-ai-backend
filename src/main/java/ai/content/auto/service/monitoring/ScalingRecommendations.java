package ai.content.auto.service.monitoring;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

/**
 * Scaling recommendations
 */
@Data
@Builder
public class ScalingRecommendations {
    private Instant timestamp;
    private List<ScalingRecommendation> recommendations;
    private double totalEstimatedCost;
    private String implementationPriority; // LOW, MEDIUM, HIGH
}