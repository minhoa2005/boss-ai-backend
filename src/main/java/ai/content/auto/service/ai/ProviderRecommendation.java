package ai.content.auto.service.ai;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * AI provider recommendation with alternatives and reasoning
 */
@Data
@Builder
public class ProviderRecommendation {

    private AIProvider primaryProvider;
    private double primaryScore;
    private Map<String, Double> scoreBreakdown;

    private List<ProviderAlternative> alternatives;

    private String optimizationReason;
    private double confidence; // 0.0 to 1.0

    private String riskAssessment;
    private List<String> warnings;

    @Data
    @Builder
    public static class ProviderAlternative {
        private AIProvider provider;
        private double score;
        private String reason;
        private Map<String, Double> scoreBreakdown;
    }
}