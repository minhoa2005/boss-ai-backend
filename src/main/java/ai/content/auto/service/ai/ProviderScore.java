package ai.content.auto.service.ai;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * Provider scoring information
 */
@Data
@Builder
public class ProviderScore {

    private AIProvider provider;
    private double totalScore;
    private Map<String, Double> scoreBreakdown;

    // Individual score components
    private double costScore;
    private double qualityScore;
    private double availabilityScore;
    private double responseTimeScore;
    private double loadScore;

    // Metadata
    private String scoringReason;
    private long calculatedAt;
}