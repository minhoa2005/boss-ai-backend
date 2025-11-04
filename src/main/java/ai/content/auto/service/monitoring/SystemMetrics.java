package ai.content.auto.service.monitoring;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * System performance metrics
 */
@Data
@Builder
public class SystemMetrics {

    private Instant timestamp;

    // Memory metrics
    private long heapMemoryUsed;
    private long heapMemoryMax;
    private double heapMemoryUsagePercent;
    private long nonHeapMemoryUsed;
    private long nonHeapMemoryMax;
    private double nonHeapMemoryUsagePercent;

    // CPU metrics
    private double cpuUsagePercent;
    private int availableProcessors;

    // System metrics
    private long systemUptime;

    // Derived metrics
    private double memoryPressure; // 0.0 to 1.0
    private double systemLoad; // 0.0 to 1.0
}