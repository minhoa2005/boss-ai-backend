package ai.content.auto.service.ai;

import lombok.Builder;
import lombok.Data;

/**
 * Criteria for AI provider selection optimization
 */
@Data
@Builder
public class ProviderSelectionCriteria {

    private String contentType;
    private String language;
    private String tone;

    // Quality constraints
    private Double minQualityScore;
    private Double maxQualityScore;

    // Cost constraints
    private Double maxCostPerToken;
    private Double maxTotalCost;

    // Performance constraints
    private Long maxResponseTimeMs;
    private Double minSuccessRate;

    // Optimization preferences
    private Boolean prioritizeCost;
    private Boolean prioritizeQuality;
    private Boolean prioritizeSpeed;
    private Boolean prioritizeReliability;

    // Request characteristics
    private Integer expectedTokenCount;
    private String urgencyLevel; // LOW, MEDIUM, HIGH
    private Boolean allowFallback;

    @Override
    public int hashCode() {
        // Simple hash for caching purposes
        return toString().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        return toString().equals(obj.toString());
    }

    @Override
    public String toString() {
        return String.format("ProviderSelectionCriteria{contentType='%s', language='%s', " +
                "prioritizeCost=%s, prioritizeQuality=%s, prioritizeSpeed=%s}",
                contentType, language, prioritizeCost, prioritizeQuality, prioritizeSpeed);
    }
}