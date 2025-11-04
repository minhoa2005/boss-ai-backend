package ai.content.auto.service.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Service for tracking AI provider metrics and performance
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AIProviderMetricsService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String METRICS_PREFIX = "ai:provider:metrics:";
    private static final String HEALTH_PREFIX = "ai:provider:health:";
    private static final int METRICS_TTL_HOURS = 24;
    private static final int HEALTH_TTL_MINUTES = 5;

    /**
     * Record a successful request for a provider
     */
    public void recordSuccess(String providerName, long responseTimeMs, double qualityScore) {
        try {
            String key = METRICS_PREFIX + providerName;
            Instant now = Instant.now();

            // Update success metrics
            redisTemplate.opsForHash().increment(key, "total_requests", 1);
            redisTemplate.opsForHash().increment(key, "successful_requests", 1);
            redisTemplate.opsForHash().put(key, "last_success", now.toEpochMilli());

            // Update response time metrics
            updateResponseTimeMetrics(key, responseTimeMs);

            // Update quality metrics
            updateQualityMetrics(key, qualityScore);

            // Reset consecutive failures
            redisTemplate.opsForHash().put(key, "consecutive_failures", 0);

            // Set TTL
            redisTemplate.expire(key, METRICS_TTL_HOURS, TimeUnit.HOURS);

            log.debug("Recorded success for provider: {} - response time: {}ms, quality: {}",
                    providerName, responseTimeMs, qualityScore);

        } catch (Exception e) {
            log.error("Failed to record success metrics for provider: {}", providerName, e);
        }
    }

    /**
     * Record a failed request for a provider
     */
    public void recordFailure(String providerName, String errorType, String errorMessage) {
        try {
            String key = METRICS_PREFIX + providerName;
            Instant now = Instant.now();

            // Update failure metrics
            redisTemplate.opsForHash().increment(key, "total_requests", 1);
            redisTemplate.opsForHash().increment(key, "failed_requests", 1);
            redisTemplate.opsForHash().put(key, "last_failure", now.toEpochMilli());
            redisTemplate.opsForHash().increment(key, "consecutive_failures", 1);

            // Track error types
            String errorKey = key + ":errors:" + errorType;
            redisTemplate.opsForHash().increment(errorKey, "count", 1);
            redisTemplate.opsForHash().put(errorKey, "last_occurrence", now.toEpochMilli());
            redisTemplate.expire(errorKey, METRICS_TTL_HOURS, TimeUnit.HOURS);

            // Set TTL
            redisTemplate.expire(key, METRICS_TTL_HOURS, TimeUnit.HOURS);

            log.warn("Recorded failure for provider: {} - error: {} - {}",
                    providerName, errorType, errorMessage);

        } catch (Exception e) {
            log.error("Failed to record failure metrics for provider: {}", providerName, e);
        }
    }

    /**
     * Record a fallback success (when primary provider failed)
     */
    public void recordFallbackSuccess(String providerName, long responseTimeMs, double qualityScore) {
        try {
            String key = METRICS_PREFIX + providerName;

            // Record as regular success
            recordSuccess(providerName, responseTimeMs, qualityScore);

            // Also track fallback metrics
            redisTemplate.opsForHash().increment(key, "fallback_requests", 1);
            redisTemplate.opsForHash().increment(key, "fallback_successes", 1);

            log.info("Recorded fallback success for provider: {} - response time: {}ms",
                    providerName, responseTimeMs);

        } catch (Exception e) {
            log.error("Failed to record fallback success metrics for provider: {}", providerName, e);
        }
    }

    /**
     * Update health status for a provider
     */
    public void updateHealthStatus(String providerName, ProviderHealthStatus healthStatus) {
        try {
            String key = HEALTH_PREFIX + providerName;

            Map<String, Object> healthData = new HashMap<>();
            healthData.put("health_level", healthStatus.getHealthLevel().name());
            healthData.put("is_available", healthStatus.isAvailable());
            healthData.put("consecutive_failures", healthStatus.getConsecutiveFailures());
            healthData.put("current_response_time", healthStatus.getCurrentResponseTime());
            healthData.put("error_rate", healthStatus.getErrorRate());
            healthData.put("message", healthStatus.getMessage());
            healthData.put("last_check", Instant.now().toEpochMilli());

            if (healthStatus.getLastSuccessfulRequest() != null) {
                healthData.put("last_success", healthStatus.getLastSuccessfulRequest().toEpochMilli());
            }

            if (healthStatus.getLastFailedRequest() != null) {
                healthData.put("last_failure", healthStatus.getLastFailedRequest().toEpochMilli());
            }

            redisTemplate.opsForHash().putAll(key, healthData);
            redisTemplate.expire(key, HEALTH_TTL_MINUTES, TimeUnit.MINUTES);

        } catch (Exception e) {
            log.error("Failed to update health status for provider: {}", providerName, e);
        }
    }

    /**
     * Get provider metrics
     */
    public Map<String, Object> getProviderMetrics(String providerName) {
        try {
            String key = METRICS_PREFIX + providerName;
            Map<Object, Object> rawMetrics = redisTemplate.opsForHash().entries(key);

            Map<String, Object> metrics = new HashMap<>();
            rawMetrics.forEach((k, v) -> metrics.put(k.toString(), v));

            // Calculate derived metrics
            calculateDerivedMetrics(metrics);

            return metrics;

        } catch (Exception e) {
            log.error("Failed to get metrics for provider: {}", providerName, e);
            return new HashMap<>();
        }
    }

    /**
     * Get provider health status
     */
    public ProviderHealthStatus getProviderHealthStatus(String providerName) {
        try {
            String key = HEALTH_PREFIX + providerName;
            Map<Object, Object> healthData = redisTemplate.opsForHash().entries(key);

            if (healthData.isEmpty()) {
                return ProviderHealthStatus.builder()
                        .healthLevel(ProviderHealthStatus.HealthLevel.DOWN)
                        .isAvailable(false)
                        .message("No health data available")
                        .lastHealthCheck(Instant.now())
                        .build();
            }

            return ProviderHealthStatus.builder()
                    .healthLevel(ProviderHealthStatus.HealthLevel.valueOf(
                            healthData.getOrDefault("health_level", "DOWN").toString()))
                    .isAvailable(Boolean.parseBoolean(
                            healthData.getOrDefault("is_available", "false").toString()))
                    .consecutiveFailures(Integer.parseInt(
                            healthData.getOrDefault("consecutive_failures", "0").toString()))
                    .currentResponseTime(Long.parseLong(
                            healthData.getOrDefault("current_response_time", "0").toString()))
                    .errorRate(Double.parseDouble(
                            healthData.getOrDefault("error_rate", "0.0").toString()))
                    .message(healthData.getOrDefault("message", "").toString())
                    .lastHealthCheck(Instant.ofEpochMilli(Long.parseLong(
                            healthData.getOrDefault("last_check", "0").toString())))
                    .build();

        } catch (Exception e) {
            log.error("Failed to get health status for provider: {}", providerName, e);
            return ProviderHealthStatus.builder()
                    .healthLevel(ProviderHealthStatus.HealthLevel.DOWN)
                    .isAvailable(false)
                    .message("Error retrieving health status")
                    .lastHealthCheck(Instant.now())
                    .build();
        }
    }

    /**
     * Calculate success rate for a provider
     */
    public double calculateSuccessRate(String providerName) {
        try {
            Map<String, Object> metrics = getProviderMetrics(providerName);

            long totalRequests = Long.parseLong(metrics.getOrDefault("total_requests", "0").toString());
            long successfulRequests = Long.parseLong(metrics.getOrDefault("successful_requests", "0").toString());

            if (totalRequests == 0) {
                return 0.0;
            }

            return (double) successfulRequests / totalRequests;

        } catch (Exception e) {
            log.error("Failed to calculate success rate for provider: {}", providerName, e);
            return 0.0;
        }
    }

    /**
     * Calculate average response time for a provider
     */
    public long calculateAverageResponseTime(String providerName) {
        try {
            Map<String, Object> metrics = getProviderMetrics(providerName);

            long totalResponseTime = Long.parseLong(metrics.getOrDefault("total_response_time", "0").toString());
            long successfulRequests = Long.parseLong(metrics.getOrDefault("successful_requests", "0").toString());

            if (successfulRequests == 0) {
                return 0L;
            }

            return totalResponseTime / successfulRequests;

        } catch (Exception e) {
            log.error("Failed to calculate average response time for provider: {}", providerName, e);
            return 0L;
        }
    }

    /**
     * Calculate average quality score for a provider
     */
    public double calculateAverageQualityScore(String providerName) {
        try {
            Map<String, Object> metrics = getProviderMetrics(providerName);

            double totalQualityScore = Double
                    .parseDouble(metrics.getOrDefault("total_quality_score", "0.0").toString());
            long qualityMeasurements = Long.parseLong(metrics.getOrDefault("quality_measurements", "0").toString());

            if (qualityMeasurements == 0) {
                return 0.0;
            }

            return totalQualityScore / qualityMeasurements;

        } catch (Exception e) {
            log.error("Failed to calculate average quality score for provider: {}", providerName, e);
            return 0.0;
        }
    }

    private void updateResponseTimeMetrics(String key, long responseTimeMs) {
        redisTemplate.opsForHash().increment(key, "total_response_time", responseTimeMs);

        // Update min/max response times
        Object currentMin = redisTemplate.opsForHash().get(key, "min_response_time");
        if (currentMin == null || responseTimeMs < Long.parseLong(currentMin.toString())) {
            redisTemplate.opsForHash().put(key, "min_response_time", responseTimeMs);
        }

        Object currentMax = redisTemplate.opsForHash().get(key, "max_response_time");
        if (currentMax == null || responseTimeMs > Long.parseLong(currentMax.toString())) {
            redisTemplate.opsForHash().put(key, "max_response_time", responseTimeMs);
        }
    }

    private void updateQualityMetrics(String key, double qualityScore) {
        // Convert to long for Redis increment (multiply by 1000 for precision)
        long qualityScoreLong = Math.round(qualityScore * 1000);
        redisTemplate.opsForHash().increment(key, "total_quality_score", qualityScoreLong);
        redisTemplate.opsForHash().increment(key, "quality_measurements", 1);
    }

    private void calculateDerivedMetrics(Map<String, Object> metrics) {
        // Calculate success rate
        long totalRequests = Long.parseLong(metrics.getOrDefault("total_requests", "0").toString());
        long successfulRequests = Long.parseLong(metrics.getOrDefault("successful_requests", "0").toString());

        if (totalRequests > 0) {
            double successRate = (double) successfulRequests / totalRequests;
            metrics.put("success_rate", successRate);
            metrics.put("error_rate", 1.0 - successRate);
        }

        // Calculate average response time
        long totalResponseTime = Long.parseLong(metrics.getOrDefault("total_response_time", "0").toString());
        if (successfulRequests > 0) {
            long avgResponseTime = totalResponseTime / successfulRequests;
            metrics.put("avg_response_time", avgResponseTime);
        }

        // Calculate average quality score (convert back from long)
        long totalQualityScore = Long.parseLong(metrics.getOrDefault("total_quality_score", "0").toString());
        long qualityMeasurements = Long.parseLong(metrics.getOrDefault("quality_measurements", "0").toString());
        if (qualityMeasurements > 0) {
            double avgQualityScore = (double) totalQualityScore / (qualityMeasurements * 1000);
            metrics.put("avg_quality_score", avgQualityScore);
        }
    }
}