package ai.content.auto.service.monitoring;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

/**
 * Database optimization recommendations
 */
@Data
@Builder
public class DatabaseOptimizationRecommendations {

    private Instant timestamp;
    private List<String> recommendations;

    // Priority and impact assessment
    private String priority; // LOW, MEDIUM, HIGH
    private String estimatedImpact; // LOW, MEDIUM, HIGH
    private String implementationComplexity; // LOW, MEDIUM, HIGH

    // Specific recommendation categories
    private List<String> indexRecommendations;
    private List<String> queryOptimizations;
    private List<String> configurationChanges;
    private List<String> maintenanceActions;
}
