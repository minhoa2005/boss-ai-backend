package ai.content.auto.service.monitoring;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Capacity planning forecast
 */
@Data
@Builder
public class CapacityPlanningForecast {
    private int forecastWindow; // months
    private Instant timestamp;
    private Map<String, CapacityProjection> projections;
    private double totalEstimatedCost;
    private String riskAssessment; // LOW, MEDIUM, HIGH
    private List<String> recommendations;
}