package ai.content.auto.service.monitoring;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Service for predictive scaling and performance optimization
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PredictiveScalingService {

    private final SystemMonitoringService systemMonitoringService;
    private final DatabaseMonitoringService databaseMonitoringService;
    private final CacheMonitoringService cacheMonitoringService;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String PREDICTION_PREFIX = "prediction:";
    private static final String SCALING_PREFIX = "scaling:";
    private static final int PREDICTION_TTL_HOURS = 24;

    /**
     * Analyze usage patterns and predict future resource needs
     */
    public UsagePatternAnalysis analyzeUsagePatterns(int days) {
        try {
            log.info("Analyzing usage patterns for {} days", days);

            // Collect historical data
            Map<String, List<Double>> historicalMetrics = collectHistoricalMetrics(days);

            // Analyze patterns
            Map<String, UsagePattern> patterns = analyzePatterns(historicalMetrics);

            // Generate predictions
            Map<String, ResourcePrediction> predictions = generatePredictions(patterns);

            UsagePatternAnalysis analysis = UsagePatternAnalysis.builder()
                    .analysisWindow(days)
                    .timestamp(Instant.now())
                    .historicalMetrics(historicalMetrics)
                    .patterns(patterns)
                    .predictions(predictions)
                    .confidence(calculateConfidence(patterns))
                    .build();

            // Store analysis
            storeUsageAnalysis(analysis);

            log.info("Usage pattern analysis completed - Confidence: {:.2f}%", analysis.getConfidence() * 100);
            return analysis;

        } catch (Exception e) {
            log.error("Failed to analyze usage patterns", e);
            throw new RuntimeException("Usage pattern analysis failed", e);
        }
    }

    /**
     * Generate scaling recommendations based on predicted load
     */
    public ScalingRecommendations generateScalingRecommendations() {
        try {
            log.info("Generating scaling recommendations");

            // Get current system state
            SystemMetrics currentMetrics = systemMonitoringService.collectSystemMetrics();
            DatabaseMetrics dbMetrics = databaseMonitoringService.collectDatabaseMetrics();
            CacheMetrics cacheMetrics = cacheMonitoringService.collectCacheMetrics();

            // Analyze usage patterns
            UsagePatternAnalysis patterns = analyzeUsagePatterns(7); // 7 days

            // Generate recommendations
            List<ScalingRecommendation> recommendations = new ArrayList<>();

            // CPU scaling recommendations
            if (patterns.getPredictions().containsKey("cpu_usage")) {
                ResourcePrediction cpuPrediction = patterns.getPredictions().get("cpu_usage");
                if (cpuPrediction.getPredictedValue() > 80) {
                    recommendations.add(ScalingRecommendation.builder()
                            .resourceType("CPU")
                            .action("SCALE_UP")
                            .currentValue(currentMetrics.getCpuUsagePercent())
                            .predictedValue(cpuPrediction.getPredictedValue())
                            .recommendation("Increase CPU allocation by 50%")
                            .priority("HIGH")
                            .estimatedCost(calculateScalingCost("CPU", 1.5))
                            .build());
                }
            }

            // Memory scaling recommendations
            if (patterns.getPredictions().containsKey("memory_usage")) {
                ResourcePrediction memoryPrediction = patterns.getPredictions().get("memory_usage");
                if (memoryPrediction.getPredictedValue() > 80) {
                    recommendations.add(ScalingRecommendation.builder()
                            .resourceType("MEMORY")
                            .action("SCALE_UP")
                            .currentValue(currentMetrics.getHeapMemoryUsagePercent())
                            .predictedValue(memoryPrediction.getPredictedValue())
                            .recommendation("Increase memory allocation by 25%")
                            .priority("MEDIUM")
                            .estimatedCost(calculateScalingCost("MEMORY", 1.25))
                            .build());
                }
            }

            // Database scaling recommendations
            if (dbMetrics.getConnectionPoolUsagePercent() > 70) {
                recommendations.add(ScalingRecommendation.builder()
                        .resourceType("DATABASE")
                        .action("SCALE_UP")
                        .currentValue(dbMetrics.getConnectionPoolUsagePercent())
                        .predictedValue(dbMetrics.getConnectionPoolUsagePercent() * 1.2)
                        .recommendation("Increase database connection pool size")
                        .priority("MEDIUM")
                        .estimatedCost(calculateScalingCost("DATABASE", 1.2))
                        .build());
            }

            ScalingRecommendations scalingRecs = ScalingRecommendations.builder()
                    .timestamp(Instant.now())
                    .recommendations(recommendations)
                    .totalEstimatedCost(recommendations.stream()
                            .mapToDouble(ScalingRecommendation::getEstimatedCost)
                            .sum())
                    .implementationPriority(determineImplementationPriority(recommendations))
                    .build();

            log.info("Generated {} scaling recommendations with total cost: ${:.2f}",
                    recommendations.size(), scalingRecs.getTotalEstimatedCost());

            return scalingRecs;

        } catch (Exception e) {
            log.error("Failed to generate scaling recommendations", e);
            throw new RuntimeException("Scaling recommendations generation failed", e);
        }
    }

    /**
     * Create capacity planning forecast
     */
    public CapacityPlanningForecast createCapacityForecast(int months) {
        try {
            log.info("Creating capacity planning forecast for {} months", months);

            // Analyze growth trends
            UsagePatternAnalysis patterns = analyzeUsagePatterns(30); // 30 days for trend analysis

            // Project future capacity needs
            Map<String, CapacityProjection> projections = new HashMap<>();

            for (Map.Entry<String, ResourcePrediction> entry : patterns.getPredictions().entrySet()) {
                String resource = entry.getKey();
                ResourcePrediction prediction = entry.getValue();

                // Project growth over months
                double monthlyGrowthRate = calculateMonthlyGrowthRate(prediction);
                List<Double> monthlyProjections = new ArrayList<>();

                for (int month = 1; month <= months; month++) {
                    double projectedValue = prediction.getPredictedValue() * Math.pow(1 + monthlyGrowthRate, month);
                    monthlyProjections.add(projectedValue);
                }

                projections.put(resource, CapacityProjection.builder()
                        .resourceType(resource)
                        .currentCapacity(prediction.getCurrentValue())
                        .monthlyProjections(monthlyProjections)
                        .growthRate(monthlyGrowthRate)
                        .capacityThreshold(80.0) // 80% threshold
                        .recommendedAction(determineCapacityAction(monthlyProjections))
                        .build());
            }

            CapacityPlanningForecast forecast = CapacityPlanningForecast.builder()
                    .forecastWindow(months)
                    .timestamp(Instant.now())
                    .projections(projections)
                    .totalEstimatedCost(calculateTotalCapacityCost(projections))
                    .riskAssessment(assessCapacityRisks(projections))
                    .recommendations(generateCapacityRecommendations(projections))
                    .build();

            log.info("Capacity forecast created - Total estimated cost: ${:.2f}", forecast.getTotalEstimatedCost());
            return forecast;

        } catch (Exception e) {
            log.error("Failed to create capacity forecast", e);
            throw new RuntimeException("Capacity forecast creation failed", e);
        }
    }

    /**
     * Detect performance regression and generate alerts
     */
    public PerformanceRegressionAnalysis detectPerformanceRegression() {
        try {
            log.info("Detecting performance regression");

            // Get current performance metrics
            SystemMetrics currentMetrics = systemMonitoringService.collectSystemMetrics();

            // Get baseline metrics (from 7 days ago)
            Map<String, Object> baselineMetrics = getBaselineMetrics(7);

            // Compare metrics and detect regressions
            List<PerformanceRegression> regressions = new ArrayList<>();

            // CPU regression check
            double currentCpu = currentMetrics.getCpuUsagePercent();
            double baselineCpu = Double.parseDouble(baselineMetrics.getOrDefault("cpu_usage_percent", "0").toString());
            if (currentCpu > baselineCpu * 1.2) { // 20% increase
                regressions.add(PerformanceRegression.builder()
                        .metric("CPU Usage")
                        .currentValue(currentCpu)
                        .baselineValue(baselineCpu)
                        .regressionPercent((currentCpu - baselineCpu) / baselineCpu * 100)
                        .severity(determineSeverity(currentCpu, baselineCpu))
                        .recommendation("Investigate CPU-intensive processes")
                        .build());
            }

            // Memory regression check
            double currentMemory = currentMetrics.getHeapMemoryUsagePercent();
            double baselineMemory = Double
                    .parseDouble(baselineMetrics.getOrDefault("heap_usage_percent", "0").toString());
            if (currentMemory > baselineMemory * 1.15) { // 15% increase
                regressions.add(PerformanceRegression.builder()
                        .metric("Memory Usage")
                        .currentValue(currentMemory)
                        .baselineValue(baselineMemory)
                        .regressionPercent((currentMemory - baselineMemory) / baselineMemory * 100)
                        .severity(determineSeverity(currentMemory, baselineMemory))
                        .recommendation("Check for memory leaks or optimize memory usage")
                        .build());
            }

            PerformanceRegressionAnalysis analysis = PerformanceRegressionAnalysis.builder()
                    .timestamp(Instant.now())
                    .regressions(regressions)
                    .overallRegressionScore(calculateRegressionScore(regressions))
                    .alertLevel(determineAlertLevel(regressions))
                    .recommendations(generateRegressionRecommendations(regressions))
                    .build();

            // Create alerts for significant regressions
            if (!regressions.isEmpty()) {
                systemMonitoringService.createAlert(
                        SystemMonitoringService.AlertType.SYSTEM_ERROR,
                        String.format("Performance regression detected - %d metrics degraded", regressions.size()),
                        SystemMonitoringService.AlertSeverity.MEDIUM);
            }

            log.info("Performance regression analysis completed - {} regressions detected", regressions.size());
            return analysis;

        } catch (Exception e) {
            log.error("Failed to detect performance regression", e);
            throw new RuntimeException("Performance regression detection failed", e);
        }
    }

    /**
     * Generate cost optimization recommendations
     */
    public CostOptimizationAnalysis generateCostOptimization() {
        try {
            log.info("Generating cost optimization analysis");

            // Analyze current resource utilization
            SystemMetrics systemMetrics = systemMonitoringService.collectSystemMetrics();
            DatabaseMetrics dbMetrics = databaseMonitoringService.collectDatabaseMetrics();
            CacheMetrics cacheMetrics = cacheMonitoringService.collectCacheMetrics();

            List<CostOptimizationOpportunity> opportunities = new ArrayList<>();

            // CPU optimization
            if (systemMetrics.getCpuUsagePercent() < 30) {
                opportunities.add(CostOptimizationOpportunity.builder()
                        .resourceType("CPU")
                        .currentUtilization(systemMetrics.getCpuUsagePercent())
                        .recommendedAction("Downsize CPU allocation")
                        .estimatedSavings(calculateCostSavings("CPU", 0.7))
                        .riskLevel("LOW")
                        .build());
            }

            // Memory optimization
            if (systemMetrics.getHeapMemoryUsagePercent() < 40) {
                opportunities.add(CostOptimizationOpportunity.builder()
                        .resourceType("MEMORY")
                        .currentUtilization(systemMetrics.getHeapMemoryUsagePercent())
                        .recommendedAction("Reduce memory allocation")
                        .estimatedSavings(calculateCostSavings("MEMORY", 0.8))
                        .riskLevel("LOW")
                        .build());
            }

            // Database optimization
            if (dbMetrics.getConnectionPoolUsagePercent() < 25) {
                opportunities.add(CostOptimizationOpportunity.builder()
                        .resourceType("DATABASE")
                        .currentUtilization(dbMetrics.getConnectionPoolUsagePercent())
                        .recommendedAction("Reduce database instance size")
                        .estimatedSavings(calculateCostSavings("DATABASE", 0.75))
                        .riskLevel("MEDIUM")
                        .build());
            }

            CostOptimizationAnalysis analysis = CostOptimizationAnalysis.builder()
                    .timestamp(Instant.now())
                    .opportunities(opportunities)
                    .totalPotentialSavings(opportunities.stream()
                            .mapToDouble(CostOptimizationOpportunity::getEstimatedSavings)
                            .sum())
                    .implementationComplexity(determineImplementationComplexity(opportunities))
                    .riskAssessment(assessOptimizationRisks(opportunities))
                    .build();

            log.info("Cost optimization analysis completed - Potential savings: ${:.2f}",
                    analysis.getTotalPotentialSavings());

            return analysis;

        } catch (Exception e) {
            log.error("Failed to generate cost optimization analysis", e);
            throw new RuntimeException("Cost optimization analysis failed", e);
        }
    }

    // Helper methods
    private Map<String, List<Double>> collectHistoricalMetrics(int days) {
        Map<String, List<Double>> metrics = new HashMap<>();

        // Simulate historical data collection
        // In real implementation, would query time-series database
        List<Double> cpuHistory = new ArrayList<>();
        List<Double> memoryHistory = new ArrayList<>();

        for (int i = 0; i < days; i++) {
            cpuHistory.add(30.0 + Math.random() * 40); // 30-70% CPU
            memoryHistory.add(40.0 + Math.random() * 30); // 40-70% Memory
        }

        metrics.put("cpu_usage", cpuHistory);
        metrics.put("memory_usage", memoryHistory);

        return metrics;
    }

    private Map<String, UsagePattern> analyzePatterns(Map<String, List<Double>> historicalMetrics) {
        Map<String, UsagePattern> patterns = new HashMap<>();

        for (Map.Entry<String, List<Double>> entry : historicalMetrics.entrySet()) {
            String metric = entry.getKey();
            List<Double> values = entry.getValue();

            // Calculate trend
            double trend = calculateTrend(values);
            double average = values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            double variance = calculateVariance(values, average);

            patterns.put(metric, UsagePattern.builder()
                    .metric(metric)
                    .trend(trend)
                    .average(average)
                    .variance(variance)
                    .seasonality(detectSeasonality(values))
                    .build());
        }

        return patterns;
    }

    private Map<String, ResourcePrediction> generatePredictions(Map<String, UsagePattern> patterns) {
        Map<String, ResourcePrediction> predictions = new HashMap<>();

        for (Map.Entry<String, UsagePattern> entry : patterns.entrySet()) {
            String metric = entry.getKey();
            UsagePattern pattern = entry.getValue();

            // Simple linear prediction
            double predictedValue = pattern.getAverage() + (pattern.getTrend() * 7); // 7 days ahead

            predictions.put(metric, ResourcePrediction.builder()
                    .resourceType(metric)
                    .currentValue(pattern.getAverage())
                    .predictedValue(Math.max(0, predictedValue))
                    .confidence(calculatePredictionConfidence(pattern))
                    .timeframe("7 days")
                    .build());
        }

        return predictions;
    }

    private double calculateTrend(List<Double> values) {
        if (values.size() < 2)
            return 0.0;

        // Simple linear regression slope
        int n = values.size();
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;

        for (int i = 0; i < n; i++) {
            sumX += i;
            sumY += values.get(i);
            sumXY += i * values.get(i);
            sumX2 += i * i;
        }

        return (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);
    }

    private double calculateVariance(List<Double> values, double mean) {
        return values.stream()
                .mapToDouble(value -> Math.pow(value - mean, 2))
                .average()
                .orElse(0.0);
    }

    private String detectSeasonality(List<Double> values) {
        // Simplified seasonality detection
        return values.size() > 7 ? "WEEKLY" : "NONE";
    }

    private double calculateConfidence(Map<String, UsagePattern> patterns) {
        return patterns.values().stream()
                .mapToDouble(pattern -> 1.0 - (pattern.getVariance() / 100.0))
                .average()
                .orElse(0.5);
    }

    private double calculatePredictionConfidence(UsagePattern pattern) {
        // Higher confidence for lower variance and clear trends
        double varianceScore = Math.max(0, 1.0 - (pattern.getVariance() / 100.0));
        double trendScore = Math.abs(pattern.getTrend()) > 0.1 ? 0.8 : 0.5;
        return (varianceScore + trendScore) / 2.0;
    }

    private double calculateScalingCost(String resourceType, double scalingFactor) {
        // Simplified cost calculation
        Map<String, Double> baseCosts = Map.of(
                "CPU", 100.0,
                "MEMORY", 50.0,
                "DATABASE", 200.0);

        double baseCost = baseCosts.getOrDefault(resourceType, 100.0);
        return baseCost * (scalingFactor - 1.0);
    }

    private double calculateCostSavings(String resourceType, double scalingFactor) {
        // Simplified savings calculation
        Map<String, Double> baseCosts = Map.of(
                "CPU", 100.0,
                "MEMORY", 50.0,
                "DATABASE", 200.0);

        double baseCost = baseCosts.getOrDefault(resourceType, 100.0);
        return baseCost * (1.0 - scalingFactor);
    }

    private void storeUsageAnalysis(UsagePatternAnalysis analysis) {
        try {
            String key = PREDICTION_PREFIX + "usage_analysis";
            redisTemplate.opsForValue().set(key, analysis, PREDICTION_TTL_HOURS, TimeUnit.HOURS);
        } catch (Exception e) {
            log.warn("Failed to store usage analysis", e);
        }
    }

    private Map<String, Object> getBaselineMetrics(int daysAgo) {
        // Simplified baseline retrieval
        Map<String, Object> baseline = new HashMap<>();
        baseline.put("cpu_usage_percent", "45.0");
        baseline.put("heap_usage_percent", "55.0");
        return baseline;
    }

    // Additional helper methods for calculations...
    private String determineImplementationPriority(List<ScalingRecommendation> recommendations) {
        long highPriority = recommendations.stream().filter(r -> "HIGH".equals(r.getPriority())).count();
        return highPriority > 0 ? "HIGH" : "MEDIUM";
    }

    private double calculateMonthlyGrowthRate(ResourcePrediction prediction) {
        return Math.max(0.01, Math.min(0.1, prediction.getPredictedValue() / prediction.getCurrentValue() - 1.0));
    }

    private String determineCapacityAction(List<Double> projections) {
        return projections.get(projections.size() - 1) > 80 ? "SCALE_UP" : "MONITOR";
    }

    private double calculateTotalCapacityCost(Map<String, CapacityProjection> projections) {
        return projections.size() * 500.0; // Simplified calculation
    }

    private String assessCapacityRisks(Map<String, CapacityProjection> projections) {
        long highRisk = projections.values().stream()
                .filter(p -> p.getMonthlyProjections().get(p.getMonthlyProjections().size() - 1) > 90)
                .count();
        return highRisk > 0 ? "HIGH" : "MEDIUM";
    }

    private List<String> generateCapacityRecommendations(Map<String, CapacityProjection> projections) {
        List<String> recommendations = new ArrayList<>();
        recommendations.add("Monitor resource utilization trends closely");
        recommendations.add("Plan capacity upgrades 2-3 months in advance");
        return recommendations;
    }

    private String determineSeverity(double current, double baseline) {
        double increase = (current - baseline) / baseline;
        if (increase > 0.5)
            return "HIGH";
        if (increase > 0.2)
            return "MEDIUM";
        return "LOW";
    }

    private double calculateRegressionScore(List<PerformanceRegression> regressions) {
        return regressions.stream()
                .mapToDouble(PerformanceRegression::getRegressionPercent)
                .average()
                .orElse(0.0);
    }

    private String determineAlertLevel(List<PerformanceRegression> regressions) {
        long highSeverity = regressions.stream().filter(r -> "HIGH".equals(r.getSeverity())).count();
        return highSeverity > 0 ? "HIGH" : "MEDIUM";
    }

    private List<String> generateRegressionRecommendations(List<PerformanceRegression> regressions) {
        List<String> recommendations = new ArrayList<>();
        for (PerformanceRegression regression : regressions) {
            recommendations.add(regression.getRecommendation());
        }
        return recommendations;
    }

    private String determineImplementationComplexity(List<CostOptimizationOpportunity> opportunities) {
        long highRisk = opportunities.stream().filter(o -> "HIGH".equals(o.getRiskLevel())).count();
        return highRisk > 0 ? "HIGH" : "MEDIUM";
    }

    private String assessOptimizationRisks(List<CostOptimizationOpportunity> opportunities) {
        return opportunities.size() > 3 ? "MEDIUM" : "LOW";
    }
}