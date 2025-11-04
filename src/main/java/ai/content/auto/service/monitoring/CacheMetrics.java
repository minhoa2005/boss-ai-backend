package ai.content.auto.service.monitoring;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Comprehensive cache performance metrics
 */
@Data
@Builder
public class CacheMetrics {

    private Instant timestamp;

    // Component metrics
    private RedisConnectionMetrics connectionMetrics;
    private CachePerformanceMetrics performanceMetrics;
    private CacheMemoryMetrics memoryMetrics;
    private CacheKeyStatistics keyStatistics;

    // Collection metadata
    private long metricsCollectionTime;
}

@Data
@Builder
class RedisConnectionMetrics {
    private boolean isConnected;
    private long averageResponseTime; // milliseconds
    private int connectionPoolSize;
    private int activeConnections;
    private String errorMessage;
}

@Data
@Builder
class CachePerformanceMetrics {
    private long totalOperations;
    private long cacheHits;
    private long cacheMisses;
    private double hitRatio; // 0.0 to 1.0
    private double averageGetTime; // milliseconds
    private double averageSetTime; // milliseconds
}

@Data
@Builder
class CacheMemoryMetrics {
    private long usedMemory; // bytes
    private long maxMemory; // bytes
    private double memoryUsagePercent;
    private long evictedKeys;
    private long expiredKeys;
}

@Data
@Builder
class CacheKeyStatistics {
    private int totalKeys;
    private int keysWithTtl;
    private int keysWithoutTtl;
    private int averageKeySize; // bytes
    private String largestKey;
    private int largestKeySize; // bytes
}
