package ai.content.auto.service.monitoring;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Capacity projection for a resource
 */
@Data
@Builder
public class CapacityProjection {
    private String resourceType;
    private double currentCapacity;
    private List<Double> monthlyProjections;
    private double growthRate; // monthly growth rate
    private double capacityThreshold; // percentage threshold
    private String recommendedAction; // SCALE_UP, MONITOR, OPTIMIZE
}