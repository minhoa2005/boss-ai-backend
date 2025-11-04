package ai.content.auto.service.ai;

import ai.content.auto.dtos.ContentGenerateRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Advanced load balancer for AI providers with multiple strategies
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AIProviderLoadBalancer {

    private final List<AIProvider> providers;
    private final AIProviderOptimizationService optimizationService;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String LOAD_BALANCER_PREFIX = "ai:loadbalancer:";
    private static final int STATS_TTL_MINUTES = 60;

    // Round-robin state
    private final AtomicInteger roundRobinIndex = new AtomicInteger(0);

    // Request tracking
    private final Map<String, AtomicLong> providerRequestCounts = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> providerLastUsed = new ConcurrentHashMap<>();

    /**
     * Select provider using the current routing strategy
     */
    public AIProvider selectProvider(ContentGenerateRequest request) {
        // Get current routing strategy
        ProviderRoutingStrategy strategy = getCurrentRoutingStrategy();

        if (strategy == null) {
            // Fallback to round-robin if no strategy available
            return selectProviderRoundRobin();
        }

        switch (strategy.getStrategy()) {
            case LOAD_BALANCED:
                return selectProviderWeighted(strategy.getRoutingWeights());
            case PERFORMANCE_BASED:
                return selectProviderPerformanceBased(strategy.getRoutingWeights());
            case ROUND_ROBIN:
            default:
                return selectProviderRoundRobin();
        }
    }

    /**
     * Select provider using weighted random selection
     */
    public AIProvider selectProviderWeighted(Map<String, Double> weights) {
        List<AIProvider> availableProviders = getAvailableProviders();

        if (availableProviders.isEmpty()) {
            throw new RuntimeException("No providers available");
        }

        if (availableProviders.size() == 1) {
            return trackProviderSelection(availableProviders.get(0));
        }

        // Calculate cumulative weights
        double totalWeight = 0.0;
        Map<AIProvider, Double> cumulativeWeights = new LinkedHashMap<>();

        for (AIProvider provider : availableProviders) {
            double weight = weights.getOrDefault(provider.getName(), 1.0 / availableProviders.size());
            totalWeight += weight;
            cumulativeWeights.put(provider, totalWeight);
        }

        // Select based on random value
        double random = ThreadLocalRandom.current().nextDouble() * totalWeight;

        for (Map.Entry<AIProvider, Double> entry : cumulativeWeights.entrySet()) {
            if (random <= entry.getValue()) {
                return trackProviderSelection(entry.getKey());
            }
        }

        // Fallback to first available provider
        return trackProviderSelection(availableProviders.get(0));
    }

    /**
     * Select provider using round-robin strategy
     */
    public AIProvider selectProviderRoundRobin() {
        List<AIProvider> availableProviders = getAvailableProviders();

        if (availableProviders.isEmpty()) {
            throw new RuntimeException("No providers available");
        }

        if (availableProviders.size() == 1) {
            return trackProviderSelection(availableProviders.get(0));
        }

        int index = roundRobinIndex.getAndIncrement() % availableProviders.size();
        return trackProviderSelection(availableProviders.get(index));
    }

    /**
     * Select provider based on performance metrics
     */
    public AIProvider selectProviderPerformanceBased(Map<String, Double> performanceScores) {
        List<AIProvider> availableProviders = getAvailableProviders();

        if (availableProviders.isEmpty()) {
            throw new RuntimeException("No providers available");
        }

        // Find provider with highest performance score
        AIProvider bestProvider = availableProviders.stream()
                .max(Comparator.comparingDouble(provider -> performanceScores.getOrDefault(provider.getName(), 0.0)))
                .orElse(availableProviders.get(0));

        return trackProviderSelection(bestProvider);
    }

    /**
     * Select provider with least current load
     */
    public AIProvider selectProviderLeastLoaded() {
        List<AIProvider> availableProviders = getAvailableProviders();

        if (availableProviders.isEmpty()) {
            throw new RuntimeException("No providers available");
        }

        AIProvider leastLoadedProvider = availableProviders.stream()
                .min(Comparator.comparingDouble(AIProvider::getCurrentLoad))
                .orElse(availableProviders.get(0));

        return trackProviderSelection(leastLoadedProvider);
    }

    /**
     * Select provider with fastest average response time
     */
    public AIProvider selectProviderFastest() {
        List<AIProvider> availableProviders = getAvailableProviders();

        if (availableProviders.isEmpty()) {
            throw new RuntimeException("No providers available");
        }

        AIProvider fastestProvider = availableProviders.stream()
                .min(Comparator.comparingLong(AIProvider::getAverageResponseTime))
                .orElse(availableProviders.get(0));

        return trackProviderSelection(fastestProvider);
    }

    /**
     * Select provider with lowest cost
     */
    public AIProvider selectProviderCheapest() {
        List<AIProvider> availableProviders = getAvailableProviders();

        if (availableProviders.isEmpty()) {
            throw new RuntimeException("No providers available");
        }

        AIProvider cheapestProvider = availableProviders.stream()
                .min(Comparator.comparing(AIProvider::getCostPerToken))
                .orElse(availableProviders.get(0));

        return trackProviderSelection(cheapestProvider);
    }

    /**
     * Get load balancing statistics
     */
    public LoadBalancingStats getLoadBalancingStats() {
        Map<String, Long> requestCounts = new HashMap<>();
        Map<String, Long> lastUsedTimes = new HashMap<>();

        for (AIProvider provider : providers) {
            String providerName = provider.getName();
            requestCounts.put(providerName,
                    providerRequestCounts.getOrDefault(providerName, new AtomicLong(0)).get());
            lastUsedTimes.put(providerName,
                    providerLastUsed.getOrDefault(providerName, new AtomicLong(0)).get());
        }

        long totalRequests = requestCounts.values().stream().mapToLong(Long::longValue).sum();

        Map<String, Double> distributionPercentages = new HashMap<>();
        requestCounts.forEach((provider, count) -> {
            double percentage = totalRequests > 0 ? (count.doubleValue() / totalRequests) * 100 : 0.0;
            distributionPercentages.put(provider, percentage);
        });

        return LoadBalancingStats.builder()
                .totalRequests(totalRequests)
                .requestCounts(requestCounts)
                .distributionPercentages(distributionPercentages)
                .lastUsedTimes(lastUsedTimes)
                .currentStrategy(getCurrentRoutingStrategy() != null ? getCurrentRoutingStrategy().getStrategy().name()
                        : "ROUND_ROBIN")
                .build();
    }

    /**
     * Reset load balancing statistics
     */
    public void resetStats() {
        log.info("Resetting load balancing statistics");
        providerRequestCounts.clear();
        providerLastUsed.clear();
        roundRobinIndex.set(0);
    }

    /**
     * Get provider utilization report
     */
    public ProviderUtilizationReport getUtilizationReport() {
        Map<String, ProviderUtilization> utilizations = new HashMap<>();

        for (AIProvider provider : providers) {
            String providerName = provider.getName();
            long requestCount = providerRequestCounts.getOrDefault(providerName, new AtomicLong(0)).get();
            long lastUsed = providerLastUsed.getOrDefault(providerName, new AtomicLong(0)).get();

            double currentLoad = provider.getCurrentLoad();
            double successRate = provider.getSuccessRate();
            long avgResponseTime = provider.getAverageResponseTime();

            // Calculate utilization efficiency
            double efficiency = calculateUtilizationEfficiency(successRate, avgResponseTime, currentLoad);

            utilizations.put(providerName, ProviderUtilization.builder()
                    .providerName(providerName)
                    .requestCount(requestCount)
                    .currentLoad(currentLoad)
                    .successRate(successRate)
                    .averageResponseTime(avgResponseTime)
                    .lastUsed(Instant.ofEpochMilli(lastUsed))
                    .efficiency(efficiency)
                    .isUnderUtilized(currentLoad < 0.3 && requestCount > 0)
                    .isOverUtilized(currentLoad > 0.8)
                    .build());
        }

        return ProviderUtilizationReport.builder()
                .utilizations(utilizations)
                .reportGeneratedAt(Instant.now())
                .recommendations(generateUtilizationRecommendations(utilizations))
                .build();
    }

    private List<AIProvider> getAvailableProviders() {
        return providers.stream()
                .filter(AIProvider::isAvailable)
                .toList();
    }

    private AIProvider trackProviderSelection(AIProvider provider) {
        String providerName = provider.getName();

        // Update request count
        providerRequestCounts.computeIfAbsent(providerName, k -> new AtomicLong(0)).incrementAndGet();

        // Update last used time
        providerLastUsed.computeIfAbsent(providerName, k -> new AtomicLong(0))
                .set(System.currentTimeMillis());

        // Store stats in Redis for persistence
        storeStatsInRedis(providerName);

        log.debug("Selected provider: {} for load balancing", providerName);
        return provider;
    }

    private ProviderRoutingStrategy getCurrentRoutingStrategy() {
        try {
            String key = "ai:provider:routing:strategy";
            return (ProviderRoutingStrategy) redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.warn("Failed to get current routing strategy from Redis", e);
            return null;
        }
    }

    private void storeStatsInRedis(String providerName) {
        try {
            String key = LOAD_BALANCER_PREFIX + "stats:" + providerName;

            Map<String, Object> stats = new HashMap<>();
            stats.put("request_count", providerRequestCounts.get(providerName).get());
            stats.put("last_used", providerLastUsed.get(providerName).get());
            stats.put("updated_at", System.currentTimeMillis());

            redisTemplate.opsForHash().putAll(key, stats);
            redisTemplate.expire(key, STATS_TTL_MINUTES, TimeUnit.MINUTES);

        } catch (Exception e) {
            log.warn("Failed to store load balancing stats in Redis for provider: {}", providerName, e);
        }
    }

    private double calculateUtilizationEfficiency(double successRate, long avgResponseTime, double currentLoad) {
        // Efficiency formula: (success_rate * speed_factor) / load_factor
        double speedFactor = Math.max(0.1, 1.0 - (avgResponseTime / 10000.0)); // Normalize to 10s max
        double loadFactor = Math.max(0.1, currentLoad);

        return (successRate * speedFactor) / loadFactor;
    }

    private List<String> generateUtilizationRecommendations(Map<String, ProviderUtilization> utilizations) {
        List<String> recommendations = new ArrayList<>();

        // Find under-utilized providers
        List<String> underUtilized = utilizations.entrySet().stream()
                .filter(entry -> entry.getValue().isUnderUtilized())
                .map(Map.Entry::getKey)
                .toList();

        if (!underUtilized.isEmpty()) {
            recommendations.add("Consider increasing traffic to under-utilized providers: " +
                    String.join(", ", underUtilized));
        }

        // Find over-utilized providers
        List<String> overUtilized = utilizations.entrySet().stream()
                .filter(entry -> entry.getValue().isOverUtilized())
                .map(Map.Entry::getKey)
                .toList();

        if (!overUtilized.isEmpty()) {
            recommendations.add("Consider reducing load on over-utilized providers: " +
                    String.join(", ", overUtilized));
        }

        // Find most efficient provider
        String mostEfficient = utilizations.entrySet().stream()
                .max(Comparator.comparingDouble(entry -> entry.getValue().getEfficiency()))
                .map(Map.Entry::getKey)
                .orElse(null);

        if (mostEfficient != null) {
            recommendations.add("Most efficient provider: " + mostEfficient +
                    " - consider routing more traffic here");
        }

        return recommendations;
    }
}