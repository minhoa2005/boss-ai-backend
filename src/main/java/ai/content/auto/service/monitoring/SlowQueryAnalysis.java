package ai.content.auto.service.monitoring;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

/**
 * Slow query analysis and optimization recommendations
 */
@Data
@Builder
public class SlowQueryAnalysis {

    private Instant timestamp;
    private List<SlowQuery> slowQueries;
    private List<QueryOptimizationRecommendation> recommendations;

    // Summary statistics
    private int totalSlowQueries;
    private double averageSlowQueryTime;
    private String worstPerformingQuery;
    private double worstQueryTime;
}

@Data
@Builder
class SlowQuery {
    private String query;
    private double executionTime;
    private int executionCount;
    private double averageTime;
    private String recommendation;
    private String tableInvolved;
    private boolean hasIndex;
}

@Data
@Builder
class QueryOptimizationRecommendation {
    private String query;
    private double currentExecutionTime;
    private String recommendation;
    private String estimatedImprovement;
    private String priority; // LOW, MEDIUM, HIGH
    private String implementationSteps;
}