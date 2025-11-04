package ai.content.auto.service.monitoring;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * Comprehensive database performance metrics
 */
@Data
@Builder
public class DatabaseMetrics {

    private Instant timestamp;

    // Connection pool metrics
    private ConnectionPoolMetrics connectionPoolMetrics;

    // Query performance metrics
    private QueryPerformanceMetrics queryPerformanceMetrics;

    // Database size metrics
    private DatabaseSizeMetrics databaseSizeMetrics;

    // Index usage metrics
    private IndexUsageMetrics indexUsageMetrics;

    // Collection metadata
    private long metricsCollectionTime;

    // Derived metrics for easy access
    public double getConnectionPoolUsagePercent() {
        return connectionPoolMetrics != null ? connectionPoolMetrics.getConnectionPoolUsagePercent() : 0.0;
    }

    public double getAverageQueryTime() {
        return queryPerformanceMetrics != null ? queryPerformanceMetrics.getAverageQueryTime() : 0.0;
    }
}

@Data
@Builder
class ConnectionPoolMetrics {
    private int activeConnections;
    private int maxConnections;
    private int idleConnections;
    private double connectionPoolUsagePercent;
    private double averageConnectionWaitTime;
    private int connectionLeaks;
}

@Data
@Builder
class QueryPerformanceMetrics {
    private double averageQueryTime;
    private int slowQueryCount;
    private long totalQueries;
    private double queriesPerSecond;
    private double cacheHitRatio;
}

@Data
@Builder
class DatabaseSizeMetrics {
    private long totalSizeBytes;
    private long dataSize;
    private long indexSize;
    private double growthRatePerDay; // MB per day
    private String largestTable;
    private long largestTableSize;
}

@Data
@Builder
class IndexUsageMetrics {
    private int totalIndexes;
    private int usedIndexes;
    private int unusedIndexCount;
    private double indexHitRatio;
    private String mostUsedIndex;
    private String leastUsedIndex;
}