package ai.content.auto.service.monitoring;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Service for database performance monitoring and optimization
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DatabaseMonitoringService {

    private final DataSource dataSource;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String DB_METRICS_PREFIX = "db:metrics:";
    private static final int METRICS_TTL_MINUTES = 60;

    /**
     * Collect comprehensive database metrics
     */
    public DatabaseMetrics collectDatabaseMetrics() {
        try {
            log.debug("Collecting database performance metrics");

            long startTime = System.currentTimeMillis();

            // Connection pool metrics
            ConnectionPoolMetrics poolMetrics = getConnectionPoolMetrics();

            // Query performance metrics
            QueryPerformanceMetrics queryMetrics = getQueryPerformanceMetrics();

            // Database size and growth metrics
            DatabaseSizeMetrics sizeMetrics = getDatabaseSizeMetrics();

            // Index usage metrics
            IndexUsageMetrics indexMetrics = getIndexUsageMetrics();

            long collectionTime = System.currentTimeMillis() - startTime;

            DatabaseMetrics metrics = DatabaseMetrics.builder()
                    .timestamp(Instant.now())
                    .connectionPoolMetrics(poolMetrics)
                    .queryPerformanceMetrics(queryMetrics)
                    .databaseSizeMetrics(sizeMetrics)
                    .indexUsageMetrics(indexMetrics)
                    .metricsCollectionTime(collectionTime)
                    .build();

            // Store metrics
            storeDatabaseMetrics(metrics);

            log.debug("Database metrics collected in {}ms", collectionTime);
            return metrics;

        } catch (Exception e) {
            log.error("Failed to collect database metrics", e);
            throw new RuntimeException("Database metrics collection failed", e);
        }
    }

    /**
     * Analyze slow queries and provide optimization recommendations
     */
    public SlowQueryAnalysis analyzeSlowQueries() {
        try {
            log.debug("Analyzing slow queries");

            List<SlowQuery> slowQueries = getSlowQueries();
            List<QueryOptimizationRecommendation> recommendations = generateOptimizationRecommendations(slowQueries);

            SlowQueryAnalysis analysis = SlowQueryAnalysis.builder()
                    .timestamp(Instant.now())
                    .slowQueries(slowQueries)
                    .recommendations(recommendations)
                    .totalSlowQueries(slowQueries.size())
                    .averageSlowQueryTime(calculateAverageSlowQueryTime(slowQueries))
                    .build();

            log.info("Slow query analysis completed - {} slow queries found", slowQueries.size());
            return analysis;

        } catch (Exception e) {
            log.error("Failed to analyze slow queries", e);
            throw new RuntimeException("Slow query analysis failed", e);
        }
    }

    /**
     * Get database optimization recommendations
     */
    public DatabaseOptimizationRecommendations getOptimizationRecommendations() {
        try {
            log.debug("Generating database optimization recommendations");

            DatabaseMetrics currentMetrics = collectDatabaseMetrics();
            List<String> recommendations = new ArrayList<>();

            // Connection pool recommendations
            if (currentMetrics.getConnectionPoolUsagePercent() > 80) {
                recommendations.add("Consider increasing database connection pool size");
            }

            // Query performance recommendations
            if (currentMetrics.getAverageQueryTime() > 500) {
                recommendations.add("Optimize slow queries - average query time is " +
                        currentMetrics.getAverageQueryTime() + "ms");
            }

            // Index recommendations
            if (currentMetrics.getIndexUsageMetrics().getUnusedIndexCount() > 5) {
                recommendations.add("Remove unused indexes to improve write performance");
            }

            // Size recommendations
            if (currentMetrics.getDatabaseSizeMetrics().getGrowthRatePerDay() > 1000) { // 1GB per day
                recommendations.add("Database is growing rapidly - consider data archiving strategy");
            }

            DatabaseOptimizationRecommendations optimizationRecs = DatabaseOptimizationRecommendations.builder()
                    .timestamp(Instant.now())
                    .recommendations(recommendations)
                    .priority(determinePriority(recommendations))
                    .estimatedImpact(estimateImpact(recommendations))
                    .implementationComplexity(estimateComplexity(recommendations))
                    .build();

            log.info("Generated {} database optimization recommendations", recommendations.size());
            return optimizationRecs;

        } catch (Exception e) {
            log.error("Failed to generate optimization recommendations", e);
            throw new RuntimeException("Optimization recommendations generation failed", e);
        }
    }

    /**
     * Monitor database backup and recovery status
     */
    public BackupMonitoringStatus getBackupStatus() {
        try {
            log.debug("Checking database backup status");

            // In real implementation, would check actual backup systems
            BackupMonitoringStatus status = BackupMonitoringStatus.builder()
                    .timestamp(Instant.now())
                    .lastBackupTime(Instant.now().minusSeconds(3600)) // 1 hour ago
                    .backupStatus("SUCCESS")
                    .backupSize(1024 * 1024 * 500) // 500MB
                    .backupLocation("/backups/daily/")
                    .retentionDays(30)
                    .nextScheduledBackup(Instant.now().plusSeconds(86400)) // 24 hours
                    .isHealthy(true)
                    .build();

            log.debug("Backup status checked - Last backup: {}", status.getLastBackupTime());
            return status;

        } catch (Exception e) {
            log.error("Failed to check backup status", e);
            throw new RuntimeException("Backup status check failed", e);
        }
    }

    private ConnectionPoolMetrics getConnectionPoolMetrics() {
        try (Connection connection = dataSource.getConnection()) {
            // In real implementation, would get actual connection pool metrics
            // This is a simplified version

            return ConnectionPoolMetrics.builder()
                    .activeConnections(5)
                    .maxConnections(20)
                    .idleConnections(15)
                    .connectionPoolUsagePercent(25.0)
                    .averageConnectionWaitTime(10.0)
                    .connectionLeaks(0)
                    .build();

        } catch (Exception e) {
            log.error("Failed to get connection pool metrics", e);
            return ConnectionPoolMetrics.builder()
                    .activeConnections(0)
                    .maxConnections(0)
                    .connectionPoolUsagePercent(0.0)
                    .build();
        }
    }

    private QueryPerformanceMetrics getQueryPerformanceMetrics() {
        try (Connection connection = dataSource.getConnection()) {
            // Sample query to measure performance
            long startTime = System.currentTimeMillis();

            try (PreparedStatement stmt = connection.prepareStatement("SELECT 1")) {
                stmt.executeQuery();
            }

            long queryTime = System.currentTimeMillis() - startTime;

            return QueryPerformanceMetrics.builder()
                    .averageQueryTime(queryTime)
                    .slowQueryCount(0)
                    .totalQueries(1000) // Would be actual count
                    .queriesPerSecond(50.0)
                    .cacheHitRatio(0.85)
                    .build();

        } catch (Exception e) {
            log.error("Failed to get query performance metrics", e);
            return QueryPerformanceMetrics.builder()
                    .averageQueryTime(0.0)
                    .slowQueryCount(0)
                    .totalQueries(0)
                    .build();
        }
    }

    private DatabaseSizeMetrics getDatabaseSizeMetrics() {
        try (Connection connection = dataSource.getConnection()) {
            // In real implementation, would query actual database size

            return DatabaseSizeMetrics.builder()
                    .totalSizeBytes(1024L * 1024 * 1024 * 5) // 5GB
                    .dataSize(1024L * 1024 * 1024 * 4) // 4GB
                    .indexSize(1024L * 1024 * 1024) // 1GB
                    .growthRatePerDay(100.0) // 100MB per day
                    .largestTable("content_generations")
                    .largestTableSize(1024L * 1024 * 1024 * 2) // 2GB
                    .build();

        } catch (Exception e) {
            log.error("Failed to get database size metrics", e);
            return DatabaseSizeMetrics.builder()
                    .totalSizeBytes(0L)
                    .dataSize(0L)
                    .indexSize(0L)
                    .build();
        }
    }

    private IndexUsageMetrics getIndexUsageMetrics() {
        try (Connection connection = dataSource.getConnection()) {
            // In real implementation, would query index usage statistics

            return IndexUsageMetrics.builder()
                    .totalIndexes(25)
                    .usedIndexes(20)
                    .unusedIndexCount(5)
                    .indexHitRatio(0.92)
                    .mostUsedIndex("idx_content_user_id")
                    .leastUsedIndex("idx_old_feature")
                    .build();

        } catch (Exception e) {
            log.error("Failed to get index usage metrics", e);
            return IndexUsageMetrics.builder()
                    .totalIndexes(0)
                    .usedIndexes(0)
                    .unusedIndexCount(0)
                    .build();
        }
    }

    private List<SlowQuery> getSlowQueries() {
        List<SlowQuery> slowQueries = new ArrayList<>();

        // In real implementation, would query slow query log
        // This is sample data
        slowQueries.add(SlowQuery.builder()
                .query("SELECT * FROM content_generations WHERE created_at > ?")
                .executionTime(1500.0)
                .executionCount(50)
                .averageTime(1500.0)
                .recommendation("Add index on created_at column")
                .build());

        return slowQueries;
    }

    private List<QueryOptimizationRecommendation> generateOptimizationRecommendations(List<SlowQuery> slowQueries) {
        List<QueryOptimizationRecommendation> recommendations = new ArrayList<>();

        for (SlowQuery query : slowQueries) {
            recommendations.add(QueryOptimizationRecommendation.builder()
                    .query(query.getQuery())
                    .currentExecutionTime(query.getExecutionTime())
                    .recommendation(query.getRecommendation())
                    .estimatedImprovement("50% faster")
                    .priority("HIGH")
                    .build());
        }

        return recommendations;
    }

    private double calculateAverageSlowQueryTime(List<SlowQuery> slowQueries) {
        if (slowQueries.isEmpty()) {
            return 0.0;
        }

        return slowQueries.stream()
                .mapToDouble(SlowQuery::getExecutionTime)
                .average()
                .orElse(0.0);
    }

    private void storeDatabaseMetrics(DatabaseMetrics metrics) {
        try {
            String key = DB_METRICS_PREFIX + "current";

            Map<String, Object> metricsMap = new HashMap<>();
            metricsMap.put("timestamp", metrics.getTimestamp().toEpochMilli());
            metricsMap.put("connection_pool_usage", metrics.getConnectionPoolUsagePercent());
            metricsMap.put("average_query_time", metrics.getAverageQueryTime());
            metricsMap.put("total_size_bytes", metrics.getDatabaseSizeMetrics().getTotalSizeBytes());
            metricsMap.put("growth_rate_per_day", metrics.getDatabaseSizeMetrics().getGrowthRatePerDay());

            redisTemplate.opsForHash().putAll(key, metricsMap);
            redisTemplate.expire(key, METRICS_TTL_MINUTES, TimeUnit.MINUTES);

        } catch (Exception e) {
            log.warn("Failed to store database metrics in Redis", e);
        }
    }

    private String determinePriority(List<String> recommendations) {
        if (recommendations.size() > 5) {
            return "HIGH";
        } else if (recommendations.size() > 2) {
            return "MEDIUM";
        } else {
            return "LOW";
        }
    }

    private String estimateImpact(List<String> recommendations) {
        // Simplified impact estimation
        return recommendations.size() > 3 ? "HIGH" : "MEDIUM";
    }

    private String estimateComplexity(List<String> recommendations) {
        // Simplified complexity estimation
        return recommendations.size() > 5 ? "HIGH" : "MEDIUM";
    }

    // Helper method to get metrics for external use
    public double getConnectionPoolUsagePercent() {
        try {
            ConnectionPoolMetrics poolMetrics = getConnectionPoolMetrics();
            return poolMetrics.getConnectionPoolUsagePercent();
        } catch (Exception e) {
            log.error("Failed to get connection pool usage", e);
            return 0.0;
        }
    }

    public double getAverageQueryTime() {
        try {
            QueryPerformanceMetrics queryMetrics = getQueryPerformanceMetrics();
            return queryMetrics.getAverageQueryTime();
        } catch (Exception e) {
            log.error("Failed to get average query time", e);
            return 0.0;
        }
    }
}