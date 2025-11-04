package ai.content.auto.service.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Service for AI provider optimization and intelligent routing
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AIProviderOptimizationService {

    private final List<AIProvider> providers;
    private final AIProviderMetricsService metricsService;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String OPTIMIZATION_PREFIX = "ai:provider:optimization:";
    private static final String ROUTING_PREFIX = "ai:provider:routing:";
    private static final String PERFORMANCE_PREFIX = "ai:provider:performance:";
    private static final int OPTIMIZATION_TTL_HOURS = 1;
    private static final int PERFORMANCE_TTL_HOURS = 24;

    // Optimization weights (can be configured)
    private static final double COST_WEIGHT = 0.35;
    private static final double QUALITY_WEIGHT = 0.25;
    private static final double AVAILABILITY_WEIGHT = 0.20;
    private static final double RESPONSE_TIME_WEIGHT = 0.15;
    private static final double LOAD_WEIGHT = 0.05;

    // Performance thresholds
    private static final double HIGH_QUALITY_THRESHOLD = 8.0;
    private static final double LOW_QUALITY_THRESHOLD = 5.0;
    private static final double HIGH_SUCCESS_RATE_THRESHOLD = 0.95;
    private static final double LOW_SUCCESS_RATE_THRESHOLD = 0.85;
    private static final long FAST_RESPONSE_THRESHOLD_MS = 2000;
    private static final long SLOW_RESPONSE_THRESHOLD_MS = 10000;

    /**
     * Get optimized provider recommendation based on request characteristics
     */
    public ProviderRecommendation getOptimizedProvider(ProviderSelectionCriteria criteria) {
        log.debug("Getting optimized provider for criteria: {}", criteria);

        // Check cache first
        ProviderRecommendation cachedRecommendation = getCachedRecommendation(criteria);
        if (cachedRecommendation != null) {
            log.debug("Returning cached recommendation for criteria: {}", criteria);
            return cachedRecommendation;
        }

        List<AIProvider> availableProviders = getAvailableProviders(criteria);

        if (availableProviders.isEmpty()) {
            throw new RuntimeException("No providers available for the given criteria");
        }

        // Calculate scores for all available providers with enhanced scoring
        List<ProviderScore> providerScores = availableProviders.stream()
                .map(provider -> calculateEnhancedProviderScore(provider, criteria))
                .sorted(Comparator.comparingDouble(ProviderScore::getTotalScore).reversed())
                .collect(Collectors.toList());

        ProviderScore bestProvider = providerScores.get(0);

        // Create recommendation with alternatives
        List<ProviderScore> alternatives = providerScores.stream()
                .skip(1)
                .limit(2) // Top 2 alternatives
                .collect(Collectors.toList());

        // Generate risk assessment
        String riskAssessment = generateRiskAssessment(bestProvider, criteria);
        List<String> warnings = generateWarnings(bestProvider, criteria);

        ProviderRecommendation recommendation = ProviderRecommendation.builder()
                .primaryProvider(bestProvider.getProvider())
                .primaryScore(bestProvider.getTotalScore())
                .scoreBreakdown(bestProvider.getScoreBreakdown())
                .alternatives(alternatives.stream()
                        .map(score -> ProviderRecommendation.ProviderAlternative.builder()
                                .provider(score.getProvider())
                                .score(score.getTotalScore())
                                .reason(generateAlternativeReason(score, bestProvider))
                                .scoreBreakdown(score.getScoreBreakdown())
                                .build())
                        .collect(Collectors.toList()))
                .optimizationReason(generateOptimizationReason(bestProvider, criteria))
                .confidence(calculateConfidence(bestProvider, providerScores))
                .riskAssessment(riskAssessment)
                .warnings(warnings)
                .build();

        // Cache the recommendation
        cacheRecommendation(criteria, recommendation);

        log.info("Optimized provider recommendation: {} (score: {:.3f}, confidence: {:.2f}) for criteria: {}",
                bestProvider.getProvider().getName(), bestProvider.getTotalScore(),
                recommendation.getConfidence(), criteria);

        return recommendation;
    }

    /**
     * Analyze provider performance trends over time with predictive analytics
     */
    public ProviderPerformanceAnalysis analyzeProviderPerformance(String providerName, int hours) {
        log.debug("Analyzing performance for provider: {} over {} hours", providerName, hours);

        Map<String, Object> currentMetrics = metricsService.getProviderMetrics(providerName);
        Map<String, Object> historicalMetrics = getHistoricalMetrics(providerName, hours);

        // Enhanced analysis with predictions
        double currentSuccessRate = Double.parseDouble(currentMetrics.getOrDefault("success_rate", "0.0").toString());
        long currentAvgResponseTime = Long.parseLong(currentMetrics.getOrDefault("avg_response_time", "0").toString());
        double currentQualityScore = Double
                .parseDouble(currentMetrics.getOrDefault("avg_quality_score", "0.0").toString());
        double currentErrorRate = Double.parseDouble(currentMetrics.getOrDefault("error_rate", "0.0").toString());

        // Historical comparison
        double previousSuccessRate = Double
                .parseDouble(historicalMetrics.getOrDefault("success_rate", "0.0").toString());
        long previousAvgResponseTime = Long
                .parseLong(historicalMetrics.getOrDefault("avg_response_time", "0").toString());
        double previousQualityScore = Double
                .parseDouble(historicalMetrics.getOrDefault("avg_quality_score", "0.0").toString());

        // Generate predictions
        PredictionResult predictions = generatePerformancePredictions(currentMetrics, historicalMetrics);

        ProviderPerformanceAnalysis analysis = ProviderPerformanceAnalysis.builder()
                .providerName(providerName)
                .analysisWindow(hours)
                .currentSuccessRate(currentSuccessRate)
                .currentAvgResponseTime(currentAvgResponseTime)
                .currentQualityScore(currentQualityScore)
                .currentErrorRate(currentErrorRate)
                .previousSuccessRate(previousSuccessRate)
                .previousAvgResponseTime(previousAvgResponseTime)
                .previousQualityScore(previousQualityScore)
                .trends(calculateEnhancedPerformanceTrends(currentMetrics, historicalMetrics))
                .recommendations(generateEnhancedPerformanceRecommendations(currentMetrics, historicalMetrics))
                .alerts(generatePerformanceAlerts(currentMetrics))
                .riskLevel(calculateEnhancedRiskLevel(currentMetrics))
                .predictedSuccessRate(predictions.predictedSuccessRate)
                .predictedResponseTime(predictions.predictedResponseTime)
                .confidenceLevel(predictions.confidenceLevel)
                .build();

        // Cache the analysis
        cachePerformanceAnalysis(providerName, analysis);

        log.info(
                "Enhanced performance analysis completed for provider: {} - risk level: {}, predicted success rate: {:.2f}%",
                providerName, analysis.getRiskLevel(), predictions.predictedSuccessRate * 100);

        return analysis;
    }

    /**
     * Optimize provider routing based on current system state with intelligent
     * adaptation
     */
    public ProviderRoutingStrategy optimizeRouting() {
        log.debug("Optimizing provider routing strategy with intelligent adaptation");

        // Gather comprehensive system state
        SystemState systemState = analyzeCurrentSystemState();

        // Determine optimal routing strategy based on multiple factors
        RoutingStrategy strategy = determineIntelligentRoutingStrategy(systemState);

        // Calculate dynamic routing weights
        Map<String, Double> routingWeights = calculateIntelligentRoutingWeights(systemState, strategy);

        // Advanced failover configuration
        List<String> failoverOrder = determineIntelligentFailoverOrder(systemState);

        // Circuit breaker and retry configuration
        int maxRetries = calculateOptimalRetries(systemState);
        long retryDelayMs = calculateOptimalRetryDelay(systemState);
        double circuitBreakerThreshold = calculateOptimalCircuitBreakerThreshold(systemState);

        ProviderRoutingStrategy routingStrategy = ProviderRoutingStrategy.builder()
                .strategy(strategy)
                .routingWeights(routingWeights)
                .loadBalancingEnabled(strategy == RoutingStrategy.LOAD_BALANCED)
                .failoverOrder(failoverOrder)
                .maxRetries(maxRetries)
                .retryDelayMs(retryDelayMs)
                .circuitBreakerThreshold(circuitBreakerThreshold)
                .lastOptimized(Instant.now())
                .optimizationReason(generateIntelligentRoutingReason(strategy, systemState))
                .requestCounts(systemState.requestCounts)
                .lastUsed(systemState.lastUsedTimes)
                .build();

        // Cache the routing strategy
        cacheRoutingStrategy(routingStrategy);

        log.info(
                "Intelligent routing strategy optimized: {} with {} providers, max retries: {}, circuit breaker: {:.2f}",
                strategy, routingWeights.size(), maxRetries, circuitBreakerThreshold);

        return routingStrategy;
    }

    /**
     * Get enhanced cost optimization recommendations with risk analysis
     */
    public CostOptimizationRecommendation getCostOptimizationRecommendation() {
        log.debug("Generating enhanced cost optimization recommendations");

        Map<String, CostAnalysis> providerCosts = analyzeEnhancedCosts();

        // Find most cost-effective provider considering quality
        String cheapestProvider = findOptimalCostProvider(providerCosts);

        // Calculate comprehensive cost metrics
        CostMetrics costMetrics = calculateCostMetrics(providerCosts);

        List<CostOptimizationAction> recommendations = generateEnhancedCostOptimizationActions(providerCosts);

        // Risk assessment for cost optimization
        String riskLevel = assessCostOptimizationRisk(providerCosts, cheapestProvider);
        List<String> riskFactors = identifyRiskFactors(providerCosts, cheapestProvider);

        // Implementation complexity analysis
        String implementationComplexity = assessImplementationComplexity(recommendations);
        int estimatedDays = estimateImplementationDays(recommendations);

        CostOptimizationRecommendation recommendation = CostOptimizationRecommendation.builder()
                .currentAverageCost(costMetrics.currentAverageCost)
                .currentTotalCost(costMetrics.currentTotalCost)
                .totalRequests(costMetrics.totalRequests)
                .cheapestProvider(cheapestProvider)
                .cheapestCost(providerCosts.get(cheapestProvider).getCostPerRequest())
                .potentialSavingsPerRequest(costMetrics.potentialSavingsPerRequest)
                .potentialMonthlySavings(costMetrics.potentialMonthlySavings)
                .potentialAnnualSavings(costMetrics.potentialMonthlySavings * 12)
                .recommendations(recommendations)
                .providerCostAnalysis(providerCosts)
                .riskLevel(riskLevel)
                .riskFactors(riskFactors)
                .implementationComplexity(implementationComplexity)
                .estimatedImplementationDays(estimatedDays)
                .build();

        log.info(
                "Enhanced cost optimization recommendation generated - potential annual savings: ${:.2f}, risk level: {}",
                recommendation.getPotentialAnnualSavings(), riskLevel);

        return recommendation;
    }

    private List<AIProvider> getAvailableProviders(ProviderSelectionCriteria criteria) {
        return providers.stream()
                .filter(provider -> {
                    if (!provider.isAvailable()) {
                        return false;
                    }

                    ProviderCapabilities capabilities = provider.getCapabilities();

                    // Check content type support
                    if (criteria.getContentType() != null &&
                            !capabilities.getSupportedContentTypes().contains(criteria.getContentType())) {
                        return false;
                    }

                    // Check language support
                    if (criteria.getLanguage() != null &&
                            !capabilities.getSupportedLanguages().contains(criteria.getLanguage())) {
                        return false;
                    }

                    // Check quality requirements
                    if (criteria.getMinQualityScore() != null &&
                            provider.getQualityScore() < criteria.getMinQualityScore()) {
                        return false;
                    }

                    // Check cost constraints
                    if (criteria.getMaxCostPerToken() != null &&
                            provider.getCostPerToken().doubleValue() > criteria.getMaxCostPerToken()) {
                        return false;
                    }

                    return true;
                })
                .collect(Collectors.toList());
    }

    /**
     * Enhanced provider scoring with more sophisticated algorithms
     */
    private ProviderScore calculateEnhancedProviderScore(AIProvider provider, ProviderSelectionCriteria criteria) {
        Map<String, Object> metrics = metricsService.getProviderMetrics(provider.getName());

        // Cost score (lower cost = higher score) with dynamic pricing consideration
        double costScore = calculateEnhancedCostScore(provider, criteria);

        // Quality score with content type specific adjustments
        double qualityScore = calculateEnhancedQualityScore(provider, criteria);

        // Availability score with recent performance weighting
        double availabilityScore = calculateEnhancedAvailabilityScore(provider, metrics);

        // Response time score with urgency consideration
        double responseTimeScore = calculateEnhancedResponseTimeScore(provider, criteria);

        // Load score with capacity planning
        double loadScore = calculateEnhancedLoadScore(provider);

        // Reliability score based on historical performance
        double reliabilityScore = calculateReliabilityScore(provider, metrics);

        // Apply criteria-specific weights with dynamic adjustment
        WeightAdjustment weights = calculateDynamicWeights(criteria, provider);

        double totalScore = (costScore * weights.costWeight +
                qualityScore * weights.qualityWeight +
                availabilityScore * weights.availabilityWeight +
                responseTimeScore * weights.responseTimeWeight +
                loadScore * weights.loadWeight +
                reliabilityScore * weights.reliabilityWeight) / weights.totalWeight;

        // Apply bonus/penalty modifiers
        totalScore = applyPerformanceModifiers(totalScore, provider, criteria);

        Map<String, Double> scoreBreakdown = Map.of(
                "cost", costScore,
                "quality", qualityScore,
                "availability", availabilityScore,
                "responseTime", responseTimeScore,
                "load", loadScore,
                "reliability", reliabilityScore);

        return ProviderScore.builder()
                .provider(provider)
                .totalScore(Math.max(0.0, Math.min(1.0, totalScore))) // Clamp to [0,1]
                .scoreBreakdown(scoreBreakdown)
                .costScore(costScore)
                .qualityScore(qualityScore)
                .availabilityScore(availabilityScore)
                .responseTimeScore(responseTimeScore)
                .loadScore(loadScore)
                .scoringReason(generateScoringReason(provider, scoreBreakdown))
                .calculatedAt(System.currentTimeMillis())
                .build();
    }

    private ProviderScore calculateProviderScore(AIProvider provider, ProviderSelectionCriteria criteria) {
        // Fallback to enhanced scoring for backward compatibility
        return calculateEnhancedProviderScore(provider, criteria);
    }

    private double calculateCostScore(AIProvider provider, ProviderSelectionCriteria criteria) {
        double cost = provider.getCostPerToken().doubleValue();

        // If criteria specifies max cost, use that as reference
        double maxCost = criteria.getMaxCostPerToken() != null ? criteria.getMaxCostPerToken() : 0.01; // Default max
        double minCost = 0.0001; // Minimum expected cost

        return Math.max(0.0, (maxCost - cost) / (maxCost - minCost));
    }

    private double calculateResponseTimeScore(AIProvider provider) {
        long responseTime = provider.getAverageResponseTime();
        double maxResponseTime = 30000.0; // 30 seconds
        double minResponseTime = 1000.0; // 1 second

        if (responseTime <= 0) {
            return 1.0; // No data, assume best case
        }

        return Math.max(0.0, (maxResponseTime - responseTime) / (maxResponseTime - minResponseTime));
    }

    private String generateOptimizationReason(ProviderScore bestProvider, ProviderSelectionCriteria criteria) {
        StringBuilder reason = new StringBuilder();
        Map<String, Double> breakdown = bestProvider.getScoreBreakdown();

        reason.append("Selected ").append(bestProvider.getProvider().getName()).append(" because: ");

        List<String> reasons = new ArrayList<>();

        if (breakdown.get("cost") > 0.8) {
            reasons.add("excellent cost efficiency");
        }
        if (breakdown.get("quality") > 0.8) {
            reasons.add("high quality output");
        }
        if (breakdown.get("availability") > 0.9) {
            reasons.add("high reliability");
        }
        if (breakdown.get("responseTime") > 0.8) {
            reasons.add("fast response times");
        }

        if (reasons.isEmpty()) {
            reasons.add("best overall balance of factors");
        }

        return reason.append(String.join(", ", reasons)).toString();
    }

    private String generateAlternativeReason(ProviderScore alternative, ProviderScore primary) {
        Map<String, Double> altBreakdown = alternative.getScoreBreakdown();
        Map<String, Double> primaryBreakdown = primary.getScoreBreakdown();

        // Find the alternative's strongest point
        String strongestPoint = altBreakdown.entrySet().stream()
                .max(Comparator.comparingDouble(Map.Entry::getValue))
                .map(Map.Entry::getKey)
                .orElse("overall performance");

        return String.format("Better %s than primary choice", strongestPoint);
    }

    private double calculateConfidence(ProviderScore bestProvider, List<ProviderScore> allScores) {
        if (allScores.size() < 2) {
            return 1.0;
        }

        double bestScore = bestProvider.getTotalScore();
        double secondBestScore = allScores.get(1).getTotalScore();

        // Confidence based on score gap
        double scoreGap = bestScore - secondBestScore;
        return Math.min(1.0, 0.5 + scoreGap); // Base confidence 0.5, increased by score gap
    }

    private void cacheRecommendation(ProviderSelectionCriteria criteria, ProviderRecommendation recommendation) {
        try {
            String key = OPTIMIZATION_PREFIX + "recommendation:" + criteria.hashCode();
            redisTemplate.opsForValue().set(key, recommendation, OPTIMIZATION_TTL_HOURS, TimeUnit.HOURS);
        } catch (Exception e) {
            log.warn("Failed to cache provider recommendation", e);
        }
    }

    private Map<String, Object> getHistoricalMetrics(String providerName, int hours) {
        // Simplified implementation - in real system would query time-series data
        return metricsService.getProviderMetrics(providerName);
    }

    private Map<String, ProviderLoadInfo> calculateProviderLoads() {
        Map<String, ProviderLoadInfo> loads = new HashMap<>();

        for (AIProvider provider : providers) {
            double currentLoad = provider.getCurrentLoad();
            long avgResponseTime = provider.getAverageResponseTime();
            double successRate = provider.getSuccessRate();

            loads.put(provider.getName(), ProviderLoadInfo.builder()
                    .currentLoad(currentLoad)
                    .averageResponseTime(avgResponseTime)
                    .successRate(successRate)
                    .isOverloaded(currentLoad > 0.8)
                    .build());
        }

        return loads;
    }

    private Map<String, Double> calculateCurrentProviderScores() {
        Map<String, Double> scores = new HashMap<>();

        for (AIProvider provider : providers) {
            // Simple scoring based on current metrics
            double score = (provider.getSuccessRate() * 0.4) +
                    ((1.0 - provider.getCurrentLoad()) * 0.3) +
                    (Math.min(provider.getQualityScore() / 10.0, 1.0) * 0.3);
            scores.put(provider.getName(), score);
        }

        return scores;
    }

    private RoutingStrategy determineOptimalRoutingStrategy(
            Map<String, ProviderLoadInfo> loads, Map<String, Double> scores) {

        // Check if any provider is overloaded
        boolean hasOverloadedProvider = loads.values().stream()
                .anyMatch(ProviderLoadInfo::isOverloaded);

        // Check score distribution
        double maxScore = scores.values().stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
        double minScore = scores.values().stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
        double scoreRange = maxScore - minScore;

        if (hasOverloadedProvider) {
            return RoutingStrategy.LOAD_BALANCED;
        } else if (scoreRange > 0.3) {
            return RoutingStrategy.PERFORMANCE_BASED;
        } else {
            return RoutingStrategy.ROUND_ROBIN;
        }
    }

    private Map<String, Double> calculateRoutingWeights(
            Map<String, ProviderLoadInfo> loads,
            Map<String, Double> scores,
            RoutingStrategy strategy) {

        Map<String, Double> weights = new HashMap<>();

        switch (strategy) {
            case LOAD_BALANCED:
                // Inverse of load - lower load gets higher weight
                double totalInverseLoad = loads.values().stream()
                        .mapToDouble(load -> 1.0 - load.getCurrentLoad())
                        .sum();

                loads.forEach((provider, load) -> {
                    double weight = (1.0 - load.getCurrentLoad()) / totalInverseLoad;
                    weights.put(provider, weight);
                });
                break;

            case PERFORMANCE_BASED:
                // Based on scores
                double totalScore = scores.values().stream().mapToDouble(Double::doubleValue).sum();
                scores.forEach((provider, score) -> {
                    double weight = score / totalScore;
                    weights.put(provider, weight);
                });
                break;

            case ROUND_ROBIN:
            default:
                // Equal weights
                double equalWeight = 1.0 / providers.size();
                providers.forEach(provider -> weights.put(provider.getName(), equalWeight));
                break;
        }

        return weights;
    }

    private List<String> determineFailoverOrder(Map<String, Double> scores) {
        return scores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    private String generateRoutingOptimizationReason(RoutingStrategy strategy, Map<String, ProviderLoadInfo> loads) {
        switch (strategy) {
            case LOAD_BALANCED:
                return "Load balancing enabled due to high load on some providers";
            case PERFORMANCE_BASED:
                return "Performance-based routing due to significant score differences";
            case ROUND_ROBIN:
            default:
                return "Round-robin routing for balanced distribution";
        }
    }

    private void cacheRoutingStrategy(ProviderRoutingStrategy strategy) {
        try {
            String key = ROUTING_PREFIX + "strategy";
            redisTemplate.opsForValue().set(key, strategy, OPTIMIZATION_TTL_HOURS, TimeUnit.HOURS);
        } catch (Exception e) {
            log.warn("Failed to cache routing strategy", e);
        }
    }

    // Enhanced cost analysis methods
    private Map<String, CostAnalysis> analyzeEnhancedCosts() {
        Map<String, CostAnalysis> costAnalysis = new HashMap<>();
        long totalSystemRequests = 0;

        // First pass: calculate individual provider costs
        for (AIProvider provider : providers) {
            Map<String, Object> metrics = metricsService.getProviderMetrics(provider.getName());

            long totalRequests = Long.parseLong(metrics.getOrDefault("total_requests", "0").toString());
            totalSystemRequests += totalRequests;

            // More accurate token calculation based on content type
            double avgTokensPerRequest = calculateAverageTokensPerRequest(provider, metrics);

            double costPerRequest = provider.getCostPerToken().doubleValue() * avgTokensPerRequest;
            double totalCost = costPerRequest * totalRequests;

            costAnalysis.put(provider.getName(), CostAnalysis.builder()
                    .providerName(provider.getName())
                    .costPerToken(provider.getCostPerToken().doubleValue())
                    .avgTokensPerRequest(avgTokensPerRequest)
                    .costPerRequest(costPerRequest)
                    .totalRequests(totalRequests)
                    .totalCost(totalCost)
                    .marketShare(0.0) // Will be calculated in second pass
                    .build());
        }

        // Second pass: calculate market share
        final long finalTotalRequests = totalSystemRequests;
        costAnalysis.values().forEach(analysis -> {
            if (finalTotalRequests > 0) {
                double marketShare = (double) analysis.getTotalRequests() / finalTotalRequests * 100;
                analysis.setMarketShare(marketShare);
            }
        });

        return costAnalysis;
    }

    private Map<String, CostAnalysis> analyzeCosts() {
        return analyzeEnhancedCosts();
    }

    private double calculateAverageTokensPerRequest(AIProvider provider, Map<String, Object> metrics) {
        // In a real implementation, this would analyze historical token usage
        // For now, use provider-specific estimates based on capabilities
        ProviderCapabilities capabilities = provider.getCapabilities();

        // Base estimate
        double baseTokens = 150.0;

        // Adjust based on provider capabilities
        if (capabilities.getMaxTokensPerRequest() > 4000) {
            baseTokens *= 1.2; // Providers with higher limits tend to generate more
        }

        return baseTokens;
    }

    private String findOptimalCostProvider(Map<String, CostAnalysis> providerCosts) {
        // Find provider with best cost-to-quality ratio, not just cheapest
        return providerCosts.entrySet().stream()
                .min(Comparator.comparingDouble(entry -> {
                    CostAnalysis cost = entry.getValue();
                    AIProvider provider = providers.stream()
                            .filter(p -> p.getName().equals(entry.getKey()))
                            .findFirst()
                            .orElse(null);

                    if (provider == null)
                        return Double.MAX_VALUE;

                    // Cost-quality ratio (lower is better)
                    double qualityFactor = Math.max(0.1, provider.getQualityScore() / 10.0);
                    return cost.getCostPerRequest() / qualityFactor;
                }))
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    private CostMetrics calculateCostMetrics(Map<String, CostAnalysis> providerCosts) {
        long totalRequests = providerCosts.values().stream()
                .mapToLong(CostAnalysis::getTotalRequests)
                .sum();

        double totalCost = providerCosts.values().stream()
                .mapToDouble(CostAnalysis::getTotalCost)
                .sum();

        double currentAverageCost = totalRequests > 0 ? totalCost / totalRequests : 0.0;

        // Find the optimal cost (best cost-quality ratio)
        String optimalProvider = findOptimalCostProvider(providerCosts);
        double optimalCost = optimalProvider != null ? providerCosts.get(optimalProvider).getCostPerRequest()
                : currentAverageCost;

        double potentialSavingsPerRequest = Math.max(0, currentAverageCost - optimalCost);
        double potentialMonthlySavings = estimateMonthlySavings(potentialSavingsPerRequest);

        return new CostMetrics(currentAverageCost, totalCost, totalRequests,
                potentialSavingsPerRequest, potentialMonthlySavings);
    }

    private List<CostOptimizationAction> generateEnhancedCostOptimizationActions(
            Map<String, CostAnalysis> costAnalysis) {
        List<CostOptimizationAction> actions = new ArrayList<>();

        // Provider switching recommendations
        String optimalProvider = findOptimalCostProvider(costAnalysis);
        String mostExpensiveProvider = findMostExpensiveProvider(costAnalysis);

        if (optimalProvider != null && mostExpensiveProvider != null &&
                !optimalProvider.equals(mostExpensiveProvider)) {

            CostAnalysis optimal = costAnalysis.get(optimalProvider);
            CostAnalysis expensive = costAnalysis.get(mostExpensiveProvider);

            double savings = expensive.getCostPerRequest() - optimal.getCostPerRequest();

            actions.add(CostOptimizationAction.builder()
                    .actionType("SWITCH_PRIMARY_PROVIDER")
                    .description(String.format("Switch primary provider from %s to %s",
                            mostExpensiveProvider, optimalProvider))
                    .estimatedSavings(savings)
                    .impact("HIGH")
                    .effort("MEDIUM")
                    .requirements(List.of("Update routing configuration", "Monitor quality metrics"))
                    .risks(List.of("Potential quality changes", "Initial performance variance"))
                    .build());
        }

        // Load balancing optimization
        if (hasUnbalancedCostDistribution(costAnalysis)) {
            actions.add(CostOptimizationAction.builder()
                    .actionType("OPTIMIZE_LOAD_BALANCING")
                    .description("Rebalance traffic to favor cost-effective providers")
                    .estimatedSavings(calculateLoadBalancingSavings(costAnalysis))
                    .impact("MEDIUM")
                    .effort("LOW")
                    .requirements(List.of("Update load balancing weights"))
                    .risks(List.of("Temporary performance fluctuations"))
                    .build());
        }

        // Volume-based optimization
        actions.add(CostOptimizationAction.builder()
                .actionType("IMPLEMENT_VOLUME_DISCOUNTS")
                .description("Negotiate volume discounts with high-usage providers")
                .estimatedSavings(calculateVolumeDiscountSavings(costAnalysis))
                .impact("MEDIUM")
                .effort("HIGH")
                .requirements(List.of("Contract negotiations", "Usage commitment"))
                .risks(List.of("Vendor lock-in", "Minimum usage requirements"))
                .build());

        return actions;
    }

    private String findMostExpensiveProvider(Map<String, CostAnalysis> costAnalysis) {
        return costAnalysis.entrySet().stream()
                .max(Comparator.comparingDouble(entry -> entry.getValue().getCostPerRequest()))
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    private boolean hasUnbalancedCostDistribution(Map<String, CostAnalysis> costAnalysis) {
        // Check if high-cost providers are getting disproportionate traffic
        return costAnalysis.values().stream()
                .anyMatch(analysis -> analysis.getMarketShare() > 40 &&
                        analysis.getCostPerRequest() > getAverageCostPerRequest(costAnalysis) * 1.2);
    }

    private double getAverageCostPerRequest(Map<String, CostAnalysis> costAnalysis) {
        return costAnalysis.values().stream()
                .mapToDouble(CostAnalysis::getCostPerRequest)
                .average()
                .orElse(0.0);
    }

    private double calculateLoadBalancingSavings(Map<String, CostAnalysis> costAnalysis) {
        // Estimate savings from optimal load balancing
        double currentWeightedCost = costAnalysis.values().stream()
                .mapToDouble(analysis -> analysis.getCostPerRequest() * (analysis.getMarketShare() / 100))
                .sum();

        // Optimal would favor cheapest providers
        String cheapestProvider = costAnalysis.entrySet().stream()
                .min(Comparator.comparingDouble(entry -> entry.getValue().getCostPerRequest()))
                .map(Map.Entry::getKey)
                .orElse(null);

        if (cheapestProvider != null) {
            double cheapestCost = costAnalysis.get(cheapestProvider).getCostPerRequest();
            return Math.max(0, currentWeightedCost - cheapestCost) * 0.3; // Conservative 30% improvement
        }

        return 0.0;
    }

    private double calculateVolumeDiscountSavings(Map<String, CostAnalysis> costAnalysis) {
        // Estimate potential volume discount savings (typically 5-15%)
        double totalCost = costAnalysis.values().stream()
                .mapToDouble(CostAnalysis::getTotalCost)
                .sum();

        return totalCost * 0.08; // Conservative 8% volume discount
    }

    private String assessCostOptimizationRisk(Map<String, CostAnalysis> costAnalysis, String cheapestProvider) {
        if (cheapestProvider == null)
            return "HIGH";

        AIProvider provider = providers.stream()
                .filter(p -> p.getName().equals(cheapestProvider))
                .findFirst()
                .orElse(null);

        if (provider == null)
            return "HIGH";

        // Risk factors
        boolean lowQuality = provider.getQualityScore() < 6.0;
        boolean lowReliability = provider.getSuccessRate() < 0.9;
        boolean highLoad = provider.getCurrentLoad() > 0.8;

        if (lowQuality && lowReliability)
            return "HIGH";
        if (lowQuality || lowReliability || highLoad)
            return "MEDIUM";
        return "LOW";
    }

    private List<String> identifyRiskFactors(Map<String, CostAnalysis> costAnalysis, String cheapestProvider) {
        List<String> risks = new ArrayList<>();

        if (cheapestProvider == null) {
            risks.add("No clear cost-optimal provider identified");
            return risks;
        }

        AIProvider provider = providers.stream()
                .filter(p -> p.getName().equals(cheapestProvider))
                .findFirst()
                .orElse(null);

        if (provider == null) {
            risks.add("Recommended provider not available");
            return risks;
        }

        if (provider.getQualityScore() < 6.0) {
            risks.add("Cheapest provider has below-average quality scores");
        }

        if (provider.getSuccessRate() < 0.9) {
            risks.add("Cheapest provider has reliability concerns");
        }

        if (provider.getCurrentLoad() > 0.8) {
            risks.add("Cheapest provider is under high load");
        }

        CostAnalysis analysis = costAnalysis.get(cheapestProvider);
        if (analysis.getMarketShare() < 10) {
            risks.add("Limited historical data for cheapest provider");
        }

        return risks;
    }

    private String assessImplementationComplexity(List<CostOptimizationAction> recommendations) {
        boolean hasHighEffortActions = recommendations.stream()
                .anyMatch(action -> "HIGH".equals(action.getEffort()));

        boolean hasMultipleActions = recommendations.size() > 2;

        if (hasHighEffortActions && hasMultipleActions)
            return "HIGH";
        if (hasHighEffortActions || hasMultipleActions)
            return "MEDIUM";
        return "LOW";
    }

    private int estimateImplementationDays(List<CostOptimizationAction> recommendations) {
        return recommendations.stream()
                .mapToInt(action -> {
                    switch (action.getEffort()) {
                        case "HIGH":
                            return 10;
                        case "MEDIUM":
                            return 5;
                        case "LOW":
                            return 2;
                        default:
                            return 3;
                    }
                })
                .sum();
    }

    // Helper classes
    private static class CostMetrics {
        final double currentAverageCost;
        final double currentTotalCost;
        final long totalRequests;
        final double potentialSavingsPerRequest;
        final double potentialMonthlySavings;

        CostMetrics(double currentAverageCost, double currentTotalCost, long totalRequests,
                double potentialSavingsPerRequest, double potentialMonthlySavings) {
            this.currentAverageCost = currentAverageCost;
            this.currentTotalCost = currentTotalCost;
            this.totalRequests = totalRequests;
            this.potentialSavingsPerRequest = potentialSavingsPerRequest;
            this.potentialMonthlySavings = potentialMonthlySavings;
        }
    }

    private List<CostOptimizationAction> generateCostOptimizationActions(Map<String, CostAnalysis> costAnalysis) {
        List<CostOptimizationAction> actions = new ArrayList<>();

        // Find most expensive provider
        String mostExpensive = costAnalysis.entrySet().stream()
                .max(Comparator.comparingDouble(entry -> entry.getValue().getCostPerRequest()))
                .map(Map.Entry::getKey)
                .orElse(null);

        // Find cheapest provider
        String cheapest = costAnalysis.entrySet().stream()
                .min(Comparator.comparingDouble(entry -> entry.getValue().getCostPerRequest()))
                .map(Map.Entry::getKey)
                .orElse(null);

        if (mostExpensive != null && cheapest != null && !mostExpensive.equals(cheapest)) {
            CostAnalysis expensiveAnalysis = costAnalysis.get(mostExpensive);
            CostAnalysis cheapAnalysis = costAnalysis.get(cheapest);

            double savings = expensiveAnalysis.getCostPerRequest() - cheapAnalysis.getCostPerRequest();

            actions.add(CostOptimizationAction.builder()
                    .actionType("SWITCH_PROVIDER")
                    .description(String.format("Switch from %s to %s for cost savings", mostExpensive, cheapest))
                    .estimatedSavings(savings)
                    .impact("HIGH")
                    .effort("LOW")
                    .build());
        }

        return actions;
    }

    private double estimateMonthlySavings(double savingsPerRequest) {
        // Estimate based on average requests per month (simplified)
        double avgRequestsPerMonth = 10000.0; // Would be calculated from actual data
        return savingsPerRequest * avgRequestsPerMonth;
    }

    // Enhanced scoring methods
    private double calculateEnhancedCostScore(AIProvider provider, ProviderSelectionCriteria criteria) {
        double cost = provider.getCostPerToken().doubleValue();

        // Dynamic cost scoring based on criteria
        double maxCost = criteria.getMaxCostPerToken() != null ? criteria.getMaxCostPerToken() : 0.01;
        double minCost = 0.0001;

        // Base cost score
        double baseScore = Math.max(0.0, (maxCost - cost) / (maxCost - minCost));

        // Apply volume discounts for high-volume requests
        if (criteria.getExpectedTokenCount() != null && criteria.getExpectedTokenCount() > 1000) {
            baseScore *= 1.1; // 10% bonus for high-volume efficiency
        }

        return Math.min(1.0, baseScore);
    }

    private double calculateEnhancedQualityScore(AIProvider provider, ProviderSelectionCriteria criteria) {
        double baseQuality = Math.min(provider.getQualityScore() / 10.0, 1.0);

        // Content type specific quality adjustments
        if (criteria.getContentType() != null) {
            ProviderCapabilities capabilities = provider.getCapabilities();
            if (capabilities.getSupportedContentTypes().contains(criteria.getContentType())) {
                // Provider specializes in this content type
                baseQuality *= 1.05;
            }
        }

        // Language specific adjustments
        if (criteria.getLanguage() != null) {
            ProviderCapabilities capabilities = provider.getCapabilities();
            if (capabilities.getSupportedLanguages().contains(criteria.getLanguage())) {
                baseQuality *= 1.03;
            }
        }

        return Math.min(1.0, baseQuality);
    }

    private double calculateEnhancedAvailabilityScore(AIProvider provider, Map<String, Object> metrics) {
        double baseAvailability = provider.getSuccessRate();

        // Recent performance weighting (last hour more important)
        double recentErrorRate = Double.parseDouble(metrics.getOrDefault("error_rate", "0.0").toString());
        double recentAvailability = 1.0 - recentErrorRate;

        // Weighted average: 70% recent, 30% historical
        double weightedAvailability = (recentAvailability * 0.7) + (baseAvailability * 0.3);

        // Penalty for consecutive failures
        int consecutiveFailures = Integer.parseInt(metrics.getOrDefault("consecutive_failures", "0").toString());
        if (consecutiveFailures > 0) {
            weightedAvailability *= Math.pow(0.9, consecutiveFailures); // Exponential penalty
        }

        return Math.max(0.0, weightedAvailability);
    }

    private double calculateEnhancedResponseTimeScore(AIProvider provider, ProviderSelectionCriteria criteria) {
        long responseTime = provider.getAverageResponseTime();

        // Urgency-based thresholds
        double maxResponseTime = 30000.0; // 30 seconds default
        double minResponseTime = 1000.0; // 1 second

        if ("HIGH".equals(criteria.getUrgencyLevel())) {
            maxResponseTime = 10000.0; // 10 seconds for high urgency
        } else if ("LOW".equals(criteria.getUrgencyLevel())) {
            maxResponseTime = 60000.0; // 60 seconds for low urgency
        }

        if (responseTime <= 0) {
            return 1.0; // No data, assume best case
        }

        double baseScore = Math.max(0.0, (maxResponseTime - responseTime) / (maxResponseTime - minResponseTime));

        // Bonus for consistently fast responses
        if (responseTime < FAST_RESPONSE_THRESHOLD_MS) {
            baseScore *= 1.1;
        }

        return Math.min(1.0, baseScore);
    }

    private double calculateEnhancedLoadScore(AIProvider provider) {
        double currentLoad = provider.getCurrentLoad();

        // Non-linear load scoring - heavily penalize high load
        double loadScore = 1.0 - currentLoad;

        if (currentLoad > 0.8) {
            loadScore *= 0.5; // Heavy penalty for overloaded providers
        } else if (currentLoad > 0.6) {
            loadScore *= 0.8; // Moderate penalty for high load
        }

        return Math.max(0.0, loadScore);
    }

    private double calculateReliabilityScore(AIProvider provider, Map<String, Object> metrics) {
        // Historical reliability based on uptime and consistency
        double successRate = provider.getSuccessRate();

        // Consistency factor - penalize providers with high variance
        long totalRequests = Long.parseLong(metrics.getOrDefault("total_requests", "0").toString());
        if (totalRequests < 100) {
            return successRate * 0.8; // Penalty for insufficient data
        }

        // Stability bonus for providers with consistent performance
        if (successRate > HIGH_SUCCESS_RATE_THRESHOLD) {
            return Math.min(1.0, successRate * 1.05);
        }

        return successRate;
    }

    private WeightAdjustment calculateDynamicWeights(ProviderSelectionCriteria criteria, AIProvider provider) {
        double costWeight = COST_WEIGHT;
        double qualityWeight = QUALITY_WEIGHT;
        double availabilityWeight = AVAILABILITY_WEIGHT;
        double responseTimeWeight = RESPONSE_TIME_WEIGHT;
        double loadWeight = LOAD_WEIGHT;
        double reliabilityWeight = 0.05; // New reliability weight

        // Apply priority adjustments
        if (Boolean.TRUE.equals(criteria.getPrioritizeCost())) {
            costWeight *= 1.5;
        }
        if (Boolean.TRUE.equals(criteria.getPrioritizeQuality())) {
            qualityWeight *= 1.5;
        }
        if (Boolean.TRUE.equals(criteria.getPrioritizeSpeed())) {
            responseTimeWeight *= 1.5;
        }
        if (Boolean.TRUE.equals(criteria.getPrioritizeReliability())) {
            reliabilityWeight *= 2.0;
            availabilityWeight *= 1.3;
        }

        double totalWeight = costWeight + qualityWeight + availabilityWeight +
                responseTimeWeight + loadWeight + reliabilityWeight;

        return new WeightAdjustment(costWeight, qualityWeight, availabilityWeight,
                responseTimeWeight, loadWeight, reliabilityWeight, totalWeight);
    }

    private double applyPerformanceModifiers(double baseScore, AIProvider provider,
            ProviderSelectionCriteria criteria) {
        double modifiedScore = baseScore;

        // Bonus for high-quality providers
        if (provider.getQualityScore() > HIGH_QUALITY_THRESHOLD) {
            modifiedScore *= 1.02;
        }

        // Penalty for low-quality providers
        if (provider.getQualityScore() < LOW_QUALITY_THRESHOLD) {
            modifiedScore *= 0.95;
        }

        // Bonus for fast providers
        if (provider.getAverageResponseTime() < FAST_RESPONSE_THRESHOLD_MS) {
            modifiedScore *= 1.01;
        }

        // Penalty for slow providers
        if (provider.getAverageResponseTime() > SLOW_RESPONSE_THRESHOLD_MS) {
            modifiedScore *= 0.98;
        }

        return modifiedScore;
    }

    private String generateScoringReason(AIProvider provider, Map<String, Double> scoreBreakdown) {
        List<String> reasons = new ArrayList<>();

        scoreBreakdown.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(2)
                .forEach(entry -> {
                    if (entry.getValue() > 0.8) {
                        reasons.add("excellent " + entry.getKey());
                    } else if (entry.getValue() > 0.6) {
                        reasons.add("good " + entry.getKey());
                    }
                });

        if (reasons.isEmpty()) {
            reasons.add("balanced performance");
        }

        return "Selected for " + String.join(" and ", reasons);
    }

    private ProviderRecommendation getCachedRecommendation(ProviderSelectionCriteria criteria) {
        try {
            String key = OPTIMIZATION_PREFIX + "recommendation:" + criteria.hashCode();
            return (ProviderRecommendation) redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.warn("Failed to get cached recommendation", e);
            return null;
        }
    }

    private String generateRiskAssessment(ProviderScore bestProvider, ProviderSelectionCriteria criteria) {
        AIProvider provider = bestProvider.getProvider();

        if (provider.getSuccessRate() < LOW_SUCCESS_RATE_THRESHOLD) {
            return "HIGH - Provider has low success rate";
        }

        if (provider.getCurrentLoad() > 0.8) {
            return "MEDIUM - Provider is under high load";
        }

        if (provider.getAverageResponseTime() > SLOW_RESPONSE_THRESHOLD_MS) {
            return "MEDIUM - Provider has slow response times";
        }

        return "LOW - Provider shows good performance metrics";
    }

    private List<String> generateWarnings(ProviderScore bestProvider, ProviderSelectionCriteria criteria) {
        List<String> warnings = new ArrayList<>();
        AIProvider provider = bestProvider.getProvider();

        if (provider.getCurrentLoad() > 0.9) {
            warnings.add("Provider is near capacity - consider load balancing");
        }

        if (provider.getSuccessRate() < 0.9) {
            warnings.add("Provider success rate is below 90%");
        }

        if (provider.getAverageResponseTime() > 15000) {
            warnings.add("Provider response time is above 15 seconds");
        }

        return warnings;
    }

    // Helper class for weight adjustments
    private static class WeightAdjustment {
        final double costWeight;
        final double qualityWeight;
        final double availabilityWeight;
        final double responseTimeWeight;
        final double loadWeight;
        final double reliabilityWeight;
        final double totalWeight;

        WeightAdjustment(double costWeight, double qualityWeight, double availabilityWeight,
                double responseTimeWeight, double loadWeight, double reliabilityWeight, double totalWeight) {
            this.costWeight = costWeight;
            this.qualityWeight = qualityWeight;
            this.availabilityWeight = availabilityWeight;
            this.responseTimeWeight = responseTimeWeight;
            this.loadWeight = loadWeight;
            this.reliabilityWeight = reliabilityWeight;
            this.totalWeight = totalWeight;
        }
    }

    private List<PerformanceTrend> calculatePerformanceTrends(
            Map<String, Object> current, Map<String, Object> historical) {
        // Simplified trend calculation
        List<PerformanceTrend> trends = new ArrayList<>();

        trends.add(PerformanceTrend.builder()
                .metric("success_rate")
                .trend("STABLE")
                .change(0.0)
                .description("Success rate remains stable")
                .build());

        return trends;
    }

    private List<String> generatePerformanceRecommendations(
            Map<String, Object> current, Map<String, Object> historical) {
        List<String> recommendations = new ArrayList<>();

        double errorRate = Double.parseDouble(current.getOrDefault("error_rate", "0.0").toString());
        if (errorRate > 0.1) {
            recommendations.add("Monitor error rate - currently above 10%");
        }

        long avgResponseTime = Long.parseLong(current.getOrDefault("avg_response_time", "0").toString());
        if (avgResponseTime > 5000) {
            recommendations.add("Response time is slow - consider optimization");
        }

        if (recommendations.isEmpty()) {
            recommendations.add("Provider is performing well");
        }

        return recommendations;
    }

    private String calculateEnhancedRiskLevel(Map<String, Object> metrics) {
        double errorRate = Double.parseDouble(metrics.getOrDefault("error_rate", "0.0").toString());
        int consecutiveFailures = Integer.parseInt(metrics.getOrDefault("consecutive_failures", "0").toString());
        long avgResponseTime = Long.parseLong(metrics.getOrDefault("avg_response_time", "0").toString());
        double successRate = Double.parseDouble(metrics.getOrDefault("success_rate", "1.0").toString());

        // Multi-factor risk assessment
        int riskScore = 0;

        // Error rate factors
        if (errorRate > 0.2)
            riskScore += 3;
        else if (errorRate > 0.1)
            riskScore += 2;
        else if (errorRate > 0.05)
            riskScore += 1;

        // Consecutive failures
        if (consecutiveFailures >= 5)
            riskScore += 3;
        else if (consecutiveFailures >= 3)
            riskScore += 2;
        else if (consecutiveFailures >= 1)
            riskScore += 1;

        // Response time factors
        if (avgResponseTime > 30000)
            riskScore += 2;
        else if (avgResponseTime > 15000)
            riskScore += 1;

        // Success rate factors
        if (successRate < 0.8)
            riskScore += 3;
        else if (successRate < 0.9)
            riskScore += 2;
        else if (successRate < 0.95)
            riskScore += 1;

        // Determine risk level
        if (riskScore >= 6)
            return "CRITICAL";
        else if (riskScore >= 4)
            return "HIGH";
        else if (riskScore >= 2)
            return "MEDIUM";
        else
            return "LOW";
    }

    private String calculateRiskLevel(Map<String, Object> metrics) {
        return calculateEnhancedRiskLevel(metrics);
    }

    private List<PerformanceTrend> calculateEnhancedPerformanceTrends(
            Map<String, Object> current, Map<String, Object> historical) {
        List<PerformanceTrend> trends = new ArrayList<>();

        // Success rate trend
        double currentSuccessRate = Double.parseDouble(current.getOrDefault("success_rate", "0.0").toString());
        double historicalSuccessRate = Double.parseDouble(historical.getOrDefault("success_rate", "0.0").toString());
        double successRateChange = ((currentSuccessRate - historicalSuccessRate) / historicalSuccessRate) * 100;

        trends.add(PerformanceTrend.builder()
                .metric("success_rate")
                .trend(determineTrend(successRateChange))
                .change(successRateChange)
                .description(String.format("Success rate %s by %.1f%%",
                        successRateChange > 0 ? "improved" : "declined", Math.abs(successRateChange)))
                .build());

        // Response time trend
        long currentResponseTime = Long.parseLong(current.getOrDefault("avg_response_time", "0").toString());
        long historicalResponseTime = Long.parseLong(historical.getOrDefault("avg_response_time", "0").toString());
        if (historicalResponseTime > 0) {
            double responseTimeChange = ((double) (currentResponseTime - historicalResponseTime)
                    / historicalResponseTime) * 100;
            trends.add(PerformanceTrend.builder()
                    .metric("response_time")
                    .trend(determineTrend(-responseTimeChange)) // Negative because lower is better
                    .change(responseTimeChange)
                    .description(String.format("Response time %s by %.1f%%",
                            responseTimeChange > 0 ? "increased" : "decreased", Math.abs(responseTimeChange)))
                    .build());
        }

        // Quality score trend
        double currentQuality = Double.parseDouble(current.getOrDefault("avg_quality_score", "0.0").toString());
        double historicalQuality = Double.parseDouble(historical.getOrDefault("avg_quality_score", "0.0").toString());
        if (historicalQuality > 0) {
            double qualityChange = ((currentQuality - historicalQuality) / historicalQuality) * 100;
            trends.add(PerformanceTrend.builder()
                    .metric("quality_score")
                    .trend(determineTrend(qualityChange))
                    .change(qualityChange)
                    .description(String.format("Quality score %s by %.1f%%",
                            qualityChange > 0 ? "improved" : "declined", Math.abs(qualityChange)))
                    .build());
        }

        return trends;
    }

    private String determineTrend(double changePercentage) {
        if (changePercentage > 5)
            return "IMPROVING";
        else if (changePercentage < -5)
            return "DEGRADING";
        else
            return "STABLE";
    }

    private List<String> generateEnhancedPerformanceRecommendations(
            Map<String, Object> current, Map<String, Object> historical) {
        List<String> recommendations = new ArrayList<>();

        double errorRate = Double.parseDouble(current.getOrDefault("error_rate", "0.0").toString());
        long avgResponseTime = Long.parseLong(current.getOrDefault("avg_response_time", "0").toString());
        double successRate = Double.parseDouble(current.getOrDefault("success_rate", "1.0").toString());
        int consecutiveFailures = Integer.parseInt(current.getOrDefault("consecutive_failures", "0").toString());

        // Error rate recommendations
        if (errorRate > 0.15) {
            recommendations.add("URGENT: Error rate is critically high - investigate immediately");
        } else if (errorRate > 0.1) {
            recommendations.add("Monitor error rate closely - consider reducing load");
        }

        // Response time recommendations
        if (avgResponseTime > 20000) {
            recommendations.add("Response time is very slow - check provider capacity");
        } else if (avgResponseTime > 10000) {
            recommendations.add("Consider optimizing requests to improve response time");
        }

        // Success rate recommendations
        if (successRate < 0.85) {
            recommendations.add("Success rate is below acceptable threshold - consider failover");
        } else if (successRate < 0.95) {
            recommendations.add("Monitor success rate and prepare backup providers");
        }

        // Consecutive failures
        if (consecutiveFailures >= 3) {
            recommendations.add("Multiple consecutive failures detected - activate circuit breaker");
        }

        // Positive recommendations
        if (successRate > 0.98 && avgResponseTime < 3000 && errorRate < 0.02) {
            recommendations.add("Provider is performing excellently - consider increasing load");
        }

        if (recommendations.isEmpty()) {
            recommendations.add("Provider is performing within acceptable parameters");
        }

        return recommendations;
    }

    private List<String> generatePerformanceAlerts(Map<String, Object> metrics) {
        List<String> alerts = new ArrayList<>();

        double errorRate = Double.parseDouble(metrics.getOrDefault("error_rate", "0.0").toString());
        int consecutiveFailures = Integer.parseInt(metrics.getOrDefault("consecutive_failures", "0").toString());
        long avgResponseTime = Long.parseLong(metrics.getOrDefault("avg_response_time", "0").toString());

        if (errorRate > 0.2) {
            alerts.add("CRITICAL: Error rate exceeds 20%");
        }

        if (consecutiveFailures >= 5) {
            alerts.add("CRITICAL: 5 or more consecutive failures");
        }

        if (avgResponseTime > 30000) {
            alerts.add("WARNING: Response time exceeds 30 seconds");
        }

        return alerts;
    }

    private PredictionResult generatePerformancePredictions(
            Map<String, Object> current, Map<String, Object> historical) {

        // Simple linear prediction based on trends (in production, use more
        // sophisticated ML models)
        double currentSuccessRate = Double.parseDouble(current.getOrDefault("success_rate", "0.0").toString());
        double historicalSuccessRate = Double.parseDouble(historical.getOrDefault("success_rate", "0.0").toString());

        long currentResponseTime = Long.parseLong(current.getOrDefault("avg_response_time", "0").toString());
        long historicalResponseTime = Long.parseLong(historical.getOrDefault("avg_response_time", "0").toString());

        // Predict success rate (simple trend extrapolation)
        double successRateTrend = currentSuccessRate - historicalSuccessRate;
        double predictedSuccessRate = Math.max(0.0, Math.min(1.0, currentSuccessRate + successRateTrend));

        // Predict response time
        long responseTimeTrend = currentResponseTime - historicalResponseTime;
        long predictedResponseTime = Math.max(0L, currentResponseTime + responseTimeTrend);

        // Calculate confidence based on data stability
        String confidenceLevel = calculatePredictionConfidence(current, historical);

        return new PredictionResult(predictedSuccessRate, predictedResponseTime, confidenceLevel);
    }

    private String calculatePredictionConfidence(Map<String, Object> current, Map<String, Object> historical) {
        long totalRequests = Long.parseLong(current.getOrDefault("total_requests", "0").toString());

        if (totalRequests < 100)
            return "LOW";
        else if (totalRequests < 1000)
            return "MEDIUM";
        else
            return "HIGH";
    }

    private void cachePerformanceAnalysis(String providerName, ProviderPerformanceAnalysis analysis) {
        try {
            String key = PERFORMANCE_PREFIX + "analysis:" + providerName;
            redisTemplate.opsForValue().set(key, analysis, PERFORMANCE_TTL_HOURS, TimeUnit.HOURS);
        } catch (Exception e) {
            log.warn("Failed to cache performance analysis for provider: {}", providerName, e);
        }
    }

    // Intelligent routing methods
    private SystemState analyzeCurrentSystemState() {
        Map<String, ProviderLoadInfo> providerLoads = calculateProviderLoads();
        Map<String, Double> providerScores = calculateCurrentProviderScores();
        Map<String, Integer> requestCounts = new HashMap<>();
        Map<String, Long> lastUsedTimes = new HashMap<>();

        // Collect request counts and usage patterns
        for (AIProvider provider : providers) {
            Map<String, Object> metrics = metricsService.getProviderMetrics(provider.getName());
            requestCounts.put(provider.getName(),
                    Integer.parseInt(metrics.getOrDefault("total_requests", "0").toString()));
            lastUsedTimes.put(provider.getName(),
                    Long.parseLong(metrics.getOrDefault("last_success", "0").toString()));
        }

        // Calculate system-wide metrics
        double systemLoad = calculateSystemLoad(providerLoads);
        double systemReliability = calculateSystemReliability();
        boolean hasFailingProviders = hasFailingProviders(providerLoads);
        boolean hasOverloadedProviders = hasOverloadedProviders(providerLoads);

        return new SystemState(providerLoads, providerScores, requestCounts, lastUsedTimes,
                systemLoad, systemReliability, hasFailingProviders, hasOverloadedProviders);
    }

    private RoutingStrategy determineIntelligentRoutingStrategy(SystemState systemState) {
        // Emergency mode - if system reliability is low
        if (systemState.systemReliability < 0.8) {
            return RoutingStrategy.PERFORMANCE_BASED; // Route to most reliable providers
        }

        // Load balancing mode - if providers are overloaded
        if (systemState.hasOverloadedProviders) {
            return RoutingStrategy.LOAD_BALANCED;
        }

        // Performance mode - if there are significant performance differences
        double maxScore = systemState.providerScores.values().stream()
                .mapToDouble(Double::doubleValue).max().orElse(0.0);
        double minScore = systemState.providerScores.values().stream()
                .mapToDouble(Double::doubleValue).min().orElse(0.0);

        if (maxScore - minScore > 0.3) {
            return RoutingStrategy.PERFORMANCE_BASED;
        }

        // Default to round-robin for balanced systems
        return RoutingStrategy.ROUND_ROBIN;
    }

    private Map<String, Double> calculateIntelligentRoutingWeights(SystemState systemState, RoutingStrategy strategy) {
        Map<String, Double> weights = new HashMap<>();

        switch (strategy) {
            case LOAD_BALANCED:
                weights = calculateLoadBalancedWeights(systemState);
                break;
            case PERFORMANCE_BASED:
                weights = calculatePerformanceBasedWeights(systemState);
                break;
            case ROUND_ROBIN:
            default:
                weights = calculateEqualWeights();
                break;
        }

        // Apply intelligent adjustments
        weights = applyIntelligentWeightAdjustments(weights, systemState);

        return weights;
    }

    private Map<String, Double> calculateLoadBalancedWeights(SystemState systemState) {
        Map<String, Double> weights = new HashMap<>();
        double totalInverseLoad = 0.0;

        // Calculate total inverse load
        for (Map.Entry<String, ProviderLoadInfo> entry : systemState.providerLoads.entrySet()) {
            double inverseLoad = Math.max(0.1, 1.0 - entry.getValue().getCurrentLoad());
            totalInverseLoad += inverseLoad;
        }

        // Distribute weights based on inverse load
        for (Map.Entry<String, ProviderLoadInfo> entry : systemState.providerLoads.entrySet()) {
            double inverseLoad = Math.max(0.1, 1.0 - entry.getValue().getCurrentLoad());
            weights.put(entry.getKey(), inverseLoad / totalInverseLoad);
        }

        return weights;
    }

    private Map<String, Double> calculatePerformanceBasedWeights(SystemState systemState) {
        Map<String, Double> weights = new HashMap<>();
        double totalScore = systemState.providerScores.values().stream()
                .mapToDouble(Double::doubleValue).sum();

        if (totalScore > 0) {
            systemState.providerScores.forEach((provider, score) -> {
                weights.put(provider, score / totalScore);
            });
        } else {
            weights = calculateEqualWeights();
        }

        return weights;
    }

    private Map<String, Double> calculateEqualWeights() {
        Map<String, Double> weights = new HashMap<>();
        double equalWeight = 1.0 / providers.size();

        providers.forEach(provider -> weights.put(provider.getName(), equalWeight));

        return weights;
    }

    private Map<String, Double> applyIntelligentWeightAdjustments(Map<String, Double> weights,
            SystemState systemState) {
        Map<String, Double> adjustedWeights = new HashMap<>(weights);

        // Reduce weight for failing providers
        for (Map.Entry<String, ProviderLoadInfo> entry : systemState.providerLoads.entrySet()) {
            String providerName = entry.getKey();
            ProviderLoadInfo loadInfo = entry.getValue();

            if (loadInfo.getSuccessRate() < 0.8) {
                // Reduce weight for unreliable providers
                adjustedWeights.put(providerName, adjustedWeights.get(providerName) * 0.5);
            }

            if (loadInfo.getAverageResponseTime() > 20000) {
                // Reduce weight for slow providers
                adjustedWeights.put(providerName, adjustedWeights.get(providerName) * 0.8);
            }
        }

        // Normalize weights
        double totalWeight = adjustedWeights.values().stream().mapToDouble(Double::doubleValue).sum();
        if (totalWeight > 0) {
            adjustedWeights.replaceAll((k, v) -> v / totalWeight);
        }

        return adjustedWeights;
    }

    private List<String> determineIntelligentFailoverOrder(SystemState systemState) {
        // Sort providers by composite score (reliability + performance + cost)
        return systemState.providerScores.entrySet().stream()
                .sorted((e1, e2) -> {
                    double score1 = calculateCompositeFailoverScore(e1.getKey(), systemState);
                    double score2 = calculateCompositeFailoverScore(e2.getKey(), systemState);
                    return Double.compare(score2, score1); // Descending order
                })
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    private double calculateCompositeFailoverScore(String providerName, SystemState systemState) {
        double performanceScore = systemState.providerScores.getOrDefault(providerName, 0.0);
        ProviderLoadInfo loadInfo = systemState.providerLoads.get(providerName);

        if (loadInfo == null)
            return 0.0;

        // Composite score: 50% performance, 30% reliability, 20% load
        double reliabilityScore = loadInfo.getSuccessRate();
        double loadScore = 1.0 - loadInfo.getCurrentLoad();

        return (performanceScore * 0.5) + (reliabilityScore * 0.3) + (loadScore * 0.2);
    }

    private int calculateOptimalRetries(SystemState systemState) {
        // More retries for less reliable systems
        if (systemState.systemReliability < 0.8)
            return 5;
        if (systemState.systemReliability < 0.9)
            return 3;
        return 2;
    }

    private long calculateOptimalRetryDelay(SystemState systemState) {
        // Longer delays for overloaded systems
        if (systemState.hasOverloadedProviders)
            return 2000L;
        if (systemState.systemLoad > 0.7)
            return 1500L;
        return 1000L;
    }

    private double calculateOptimalCircuitBreakerThreshold(SystemState systemState) {
        // Lower threshold for unreliable systems
        if (systemState.systemReliability < 0.8)
            return 0.3;
        if (systemState.systemReliability < 0.9)
            return 0.4;
        return 0.5;
    }

    private String generateIntelligentRoutingReason(RoutingStrategy strategy, SystemState systemState) {
        StringBuilder reason = new StringBuilder();
        reason.append("Intelligent routing selected ").append(strategy.name().toLowerCase().replace('_', ' '));

        if (systemState.hasFailingProviders) {
            reason.append(" due to provider failures");
        } else if (systemState.hasOverloadedProviders) {
            reason.append(" due to high system load");
        } else if (systemState.systemReliability < 0.9) {
            reason.append(" due to reliability concerns");
        } else {
            reason.append(" for optimal performance");
        }

        return reason.toString();
    }

    private double calculateSystemLoad(Map<String, ProviderLoadInfo> providerLoads) {
        return providerLoads.values().stream()
                .mapToDouble(ProviderLoadInfo::getCurrentLoad)
                .average()
                .orElse(0.0);
    }

    private double calculateSystemReliability() {
        return providers.stream()
                .mapToDouble(AIProvider::getSuccessRate)
                .average()
                .orElse(0.0);
    }

    private boolean hasFailingProviders(Map<String, ProviderLoadInfo> providerLoads) {
        return providerLoads.values().stream()
                .anyMatch(load -> load.getSuccessRate() < 0.8);
    }

    private boolean hasOverloadedProviders(Map<String, ProviderLoadInfo> providerLoads) {
        return providerLoads.values().stream()
                .anyMatch(ProviderLoadInfo::isOverloaded);
    }

    // Helper classes
    private static class SystemState {
        final Map<String, ProviderLoadInfo> providerLoads;
        final Map<String, Double> providerScores;
        final Map<String, Integer> requestCounts;
        final Map<String, Long> lastUsedTimes;
        final double systemLoad;
        final double systemReliability;
        final boolean hasFailingProviders;
        final boolean hasOverloadedProviders;

        SystemState(Map<String, ProviderLoadInfo> providerLoads, Map<String, Double> providerScores,
                Map<String, Integer> requestCounts, Map<String, Long> lastUsedTimes,
                double systemLoad, double systemReliability,
                boolean hasFailingProviders, boolean hasOverloadedProviders) {
            this.providerLoads = providerLoads;
            this.providerScores = providerScores;
            this.requestCounts = requestCounts;
            this.lastUsedTimes = lastUsedTimes;
            this.systemLoad = systemLoad;
            this.systemReliability = systemReliability;
            this.hasFailingProviders = hasFailingProviders;
            this.hasOverloadedProviders = hasOverloadedProviders;
        }
    }

    private static class CostMetrics {
        final double currentAverageCost;
        final double currentTotalCost;
        final long totalRequests;
        final double potentialSavingsPerRequest;
        final double potentialMonthlySavings;

        CostMetrics(double currentAverageCost, double currentTotalCost, long totalRequests,
                double potentialSavingsPerRequest, double potentialMonthlySavings) {
            this.currentAverageCost = currentAverageCost;
            this.currentTotalCost = currentTotalCost;
            this.totalRequests = totalRequests;
            this.potentialSavingsPerRequest = potentialSavingsPerRequest;
            this.potentialMonthlySavings = potentialMonthlySavings;
        }
    }

    // Helper class for predictions
    private static class PredictionResult {
        final Double predictedSuccessRate;
        final Long predictedResponseTime;
        final String confidenceLevel;

        PredictionResult(Double predictedSuccessRate, Long predictedResponseTime, String confidenceLevel) {
            this.predictedSuccessRate = predictedSuccessRate;
            this.predictedResponseTime = predictedResponseTime;
            this.confidenceLevel = confidenceLevel;
        }
    }

    // Enums and inner classes
    public enum RoutingStrategy {
        ROUND_ROBIN,
        LOAD_BALANCED,
        PERFORMANCE_BASED
    }
}