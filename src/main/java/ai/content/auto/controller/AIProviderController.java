package ai.content.auto.controller;

import ai.content.auto.dtos.BaseResponse;
import ai.content.auto.service.ai.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller for AI provider management and monitoring
 */
@RestController
@RequestMapping("/api/v1/ai/providers")
@RequiredArgsConstructor
@Slf4j
public class AIProviderController {

    private final AIProviderManager providerManager;
    private final AIProviderHealthMonitor healthMonitor;
    private final AIProviderOptimizationService optimizationService;
    private final AIProviderLoadBalancer loadBalancer;
    private final AIProviderAlertingService alertingService;
    private final AIProviderDashboardService dashboardService;

    /**
     * Get status of all AI providers
     */
    @GetMapping("/status")
    public ResponseEntity<BaseResponse<List<ProviderStatus>>> getProviderStatuses() {
        log.info("Getting status for all AI providers");

        List<ProviderStatus> statuses = providerManager.getAllProviderStatuses();

        BaseResponse<List<ProviderStatus>> response = new BaseResponse<List<ProviderStatus>>()
                .setErrorMessage("Provider statuses retrieved successfully")
                .setData(statuses);

        return ResponseEntity.ok(response);
    }

    /**
     * Get detailed metrics for all AI providers
     */
    @GetMapping("/metrics")
    public ResponseEntity<BaseResponse<Map<String, Map<String, Object>>>> getProviderMetrics() {
        log.info("Getting metrics for all AI providers");

        Map<String, Map<String, Object>> metrics = providerManager.getAllProviderMetrics();

        BaseResponse<Map<String, Map<String, Object>>> response = new BaseResponse<Map<String, Map<String, Object>>>()
                .setErrorMessage("Provider metrics retrieved successfully")
                .setData(metrics);

        return ResponseEntity.ok(response);
    }

    /**
     * Get health summary for all providers
     */
    @GetMapping("/health/summary")
    public ResponseEntity<BaseResponse<ProviderHealthSummary>> getHealthSummary() {
        log.info("Getting health summary for all AI providers");

        ProviderHealthSummary summary = healthMonitor.getHealthSummary();

        BaseResponse<ProviderHealthSummary> response = new BaseResponse<ProviderHealthSummary>()
                .setErrorMessage("Health summary retrieved successfully")
                .setData(summary);

        return ResponseEntity.ok(response);
    }

    /**
     * Force health check for all providers
     */
    @PostMapping("/health/check")
    public ResponseEntity<BaseResponse<Void>> forceHealthCheck() {
        log.info("Forcing health check for all AI providers");

        healthMonitor.forceHealthCheck();

        BaseResponse<Void> response = new BaseResponse<Void>()
                .setErrorMessage("Health check initiated for all providers");

        return ResponseEntity.ok(response);
    }

    /**
     * Force health check for a specific provider
     */
    @PostMapping("/health/check/{providerName}")
    public ResponseEntity<BaseResponse<Void>> forceHealthCheck(@PathVariable String providerName) {
        log.info("Forcing health check for AI provider: {}", providerName);

        try {
            healthMonitor.forceHealthCheck(providerName);

            BaseResponse<Void> response = new BaseResponse<Void>()
                    .setErrorMessage("Health check initiated for provider: " + providerName);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            BaseResponse<Void> response = new BaseResponse<Void>()
                    .setErrorCode("PROVIDER_NOT_FOUND")
                    .setErrorMessage("Provider not found: " + providerName);

            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Get metrics for a specific provider
     */
    @GetMapping("/metrics/{providerName}")
    public ResponseEntity<BaseResponse<Map<String, Object>>> getProviderMetrics(@PathVariable String providerName) {
        log.info("Getting metrics for AI provider: {}", providerName);

        Map<String, Map<String, Object>> allMetrics = providerManager.getAllProviderMetrics();
        Map<String, Object> providerMetrics = allMetrics.get(providerName);

        if (providerMetrics == null) {
            BaseResponse<Map<String, Object>> response = new BaseResponse<Map<String, Object>>()
                    .setErrorCode("PROVIDER_NOT_FOUND")
                    .setErrorMessage("Provider not found: " + providerName);

            return ResponseEntity.badRequest().body(response);
        }

        BaseResponse<Map<String, Object>> response = new BaseResponse<Map<String, Object>>()
                .setErrorMessage("Provider metrics retrieved successfully")
                .setData(providerMetrics);

        return ResponseEntity.ok(response);
    }

    /**
     * Get optimized provider recommendation
     */
    @PostMapping("/optimize/recommend")
    public ResponseEntity<BaseResponse<ProviderRecommendation>> getProviderRecommendation(
            @RequestBody ProviderSelectionCriteria criteria) {
        log.info("Getting optimized provider recommendation for criteria: {}", criteria);

        ProviderRecommendation recommendation = optimizationService.getOptimizedProvider(criteria);

        BaseResponse<ProviderRecommendation> response = new BaseResponse<ProviderRecommendation>()
                .setErrorMessage("Provider recommendation generated successfully")
                .setData(recommendation);

        return ResponseEntity.ok(response);
    }

    /**
     * Analyze provider performance over time
     */
    @GetMapping("/analyze/{providerName}")
    public ResponseEntity<BaseResponse<ProviderPerformanceAnalysis>> analyzeProviderPerformance(
            @PathVariable String providerName,
            @RequestParam(defaultValue = "24") int hours) {
        log.info("Analyzing performance for provider: {} over {} hours", providerName, hours);

        ProviderPerformanceAnalysis analysis = optimizationService.analyzeProviderPerformance(providerName, hours);

        BaseResponse<ProviderPerformanceAnalysis> response = new BaseResponse<ProviderPerformanceAnalysis>()
                .setErrorMessage("Provider performance analysis completed")
                .setData(analysis);

        return ResponseEntity.ok(response);
    }

    /**
     * Optimize provider routing strategy
     */
    @PostMapping("/optimize/routing")
    public ResponseEntity<BaseResponse<ProviderRoutingStrategy>> optimizeRouting() {
        log.info("Optimizing provider routing strategy");

        ProviderRoutingStrategy strategy = optimizationService.optimizeRouting();

        BaseResponse<ProviderRoutingStrategy> response = new BaseResponse<ProviderRoutingStrategy>()
                .setErrorMessage("Routing strategy optimized successfully")
                .setData(strategy);

        return ResponseEntity.ok(response);
    }

    /**
     * Get cost optimization recommendations
     */
    @GetMapping("/optimize/cost")
    public ResponseEntity<BaseResponse<CostOptimizationRecommendation>> getCostOptimization() {
        log.info("Getting cost optimization recommendations");

        CostOptimizationRecommendation recommendation = optimizationService.getCostOptimizationRecommendation();

        BaseResponse<CostOptimizationRecommendation> response = new BaseResponse<CostOptimizationRecommendation>()
                .setErrorMessage("Cost optimization recommendations generated")
                .setData(recommendation);

        return ResponseEntity.ok(response);
    }

    /**
     * Get provider selection criteria builder
     */
    @GetMapping("/optimize/criteria/template")
    public ResponseEntity<BaseResponse<ProviderSelectionCriteria>> getCriteriaTemplate() {
        log.info("Getting provider selection criteria template");

        ProviderSelectionCriteria template = ProviderSelectionCriteria.builder()
                .contentType("blog")
                .language("en")
                .prioritizeCost(false)
                .prioritizeQuality(true)
                .prioritizeSpeed(false)
                .prioritizeReliability(true)
                .allowFallback(true)
                .urgencyLevel("MEDIUM")
                .build();

        BaseResponse<ProviderSelectionCriteria> response = new BaseResponse<ProviderSelectionCriteria>()
                .setErrorMessage("Provider selection criteria template")
                .setData(template);

        return ResponseEntity.ok(response);
    }

    /**
     * Get load balancing statistics
     */
    @GetMapping("/loadbalancer/stats")
    public ResponseEntity<BaseResponse<LoadBalancingStats>> getLoadBalancingStats() {
        log.info("Getting load balancing statistics");

        LoadBalancingStats stats = loadBalancer.getLoadBalancingStats();

        BaseResponse<LoadBalancingStats> response = new BaseResponse<LoadBalancingStats>()
                .setErrorMessage("Load balancing statistics retrieved successfully")
                .setData(stats);

        return ResponseEntity.ok(response);
    }

    /**
     * Get provider utilization report
     */
    @GetMapping("/loadbalancer/utilization")
    public ResponseEntity<BaseResponse<ProviderUtilizationReport>> getUtilizationReport() {
        log.info("Getting provider utilization report");

        ProviderUtilizationReport report = loadBalancer.getUtilizationReport();

        BaseResponse<ProviderUtilizationReport> response = new BaseResponse<ProviderUtilizationReport>()
                .setErrorMessage("Provider utilization report generated successfully")
                .setData(report);

        return ResponseEntity.ok(response);
    }

    /**
     * Reset load balancing statistics
     */
    @PostMapping("/loadbalancer/reset")
    public ResponseEntity<BaseResponse<Void>> resetLoadBalancingStats() {
        log.info("Resetting load balancing statistics");

        loadBalancer.resetStats();

        BaseResponse<Void> response = new BaseResponse<Void>()
                .setErrorMessage("Load balancing statistics reset successfully");

        return ResponseEntity.ok(response);
    }

    /**
     * Get comprehensive provider dashboard
     */
    @GetMapping("/dashboard")
    public ResponseEntity<BaseResponse<ProviderDashboard>> getProviderDashboard() {
        log.info("Getting comprehensive provider dashboard");

        ProviderDashboard dashboard = dashboardService.getProviderDashboard();

        BaseResponse<ProviderDashboard> response = new BaseResponse<ProviderDashboard>()
                .setErrorMessage("Provider dashboard retrieved successfully")
                .setData(dashboard);

        return ResponseEntity.ok(response);
    }

    /**
     * Get provider cost summary
     */
    @GetMapping("/costs/summary")
    public ResponseEntity<BaseResponse<Map<String, ProviderCostSummary>>> getProviderCostSummary() {
        log.info("Getting provider cost summary");

        Map<String, ProviderCostSummary> costSummary = alertingService.getProviderCostSummary();

        BaseResponse<Map<String, ProviderCostSummary>> response = new BaseResponse<Map<String, ProviderCostSummary>>()
                .setErrorMessage("Provider cost summary retrieved successfully")
                .setData(costSummary);

        return ResponseEntity.ok(response);
    }

    /**
     * Get provider alert history
     */
    @GetMapping("/alerts/{providerName}")
    public ResponseEntity<BaseResponse<List<Map<String, Object>>>> getProviderAlertHistory(
            @PathVariable String providerName,
            @RequestParam(defaultValue = "20") int limit) {
        log.info("Getting alert history for provider: {} (limit: {})", providerName, limit);

        List<Map<String, Object>> alertHistory = alertingService.getProviderAlertHistory(providerName, limit);

        BaseResponse<List<Map<String, Object>>> response = new BaseResponse<List<Map<String, Object>>>()
                .setErrorMessage("Provider alert history retrieved successfully")
                .setData(alertHistory);

        return ResponseEntity.ok(response);
    }

    /**
     * Record provider cost (for manual cost tracking)
     */
    @PostMapping("/costs/{providerName}")
    public ResponseEntity<BaseResponse<Void>> recordProviderCost(
            @PathVariable String providerName,
            @RequestParam java.math.BigDecimal cost) {
        log.info("Recording cost for provider: {} - ${}", providerName, cost);

        alertingService.recordProviderCost(providerName, cost);

        BaseResponse<Void> response = new BaseResponse<Void>()
                .setErrorMessage("Provider cost recorded successfully");

        return ResponseEntity.ok(response);
    }

    /**
     * Get provider comparison data
     */
    @GetMapping("/comparison")
    public ResponseEntity<BaseResponse<ProviderComparisonData>> getProviderComparison() {
        log.info("Getting provider comparison data");

        ProviderComparisonData comparison = dashboardService.getProviderComparison();

        BaseResponse<ProviderComparisonData> response = new BaseResponse<ProviderComparisonData>()
                .setErrorMessage("Provider comparison data retrieved successfully")
                .setData(comparison);

        return ResponseEntity.ok(response);
    }

    /**
     * Get provider performance insights
     */
    @GetMapping("/insights")
    public ResponseEntity<BaseResponse<ProviderPerformanceInsights>> getPerformanceInsights() {
        log.info("Getting provider performance insights");

        ProviderPerformanceInsights insights = dashboardService.getPerformanceInsights();

        BaseResponse<ProviderPerformanceInsights> response = new BaseResponse<ProviderPerformanceInsights>()
                .setErrorMessage("Provider performance insights generated successfully")
                .setData(insights);

        return ResponseEntity.ok(response);
    }

    /**
     * Force provider monitoring check (for testing)
     */
    @PostMapping("/monitoring/check")
    public ResponseEntity<BaseResponse<Void>> forceMonitoringCheck() {
        log.info("Forcing provider monitoring check");

        // This will trigger the monitoring service to run immediately
        try {
            alertingService.monitorProviderPerformance();

            BaseResponse<Void> response = new BaseResponse<Void>()
                    .setErrorMessage("Provider monitoring check completed successfully");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to force monitoring check", e);

            BaseResponse<Void> response = new BaseResponse<Void>()
                    .setErrorCode("MONITORING_ERROR")
                    .setErrorMessage("Failed to complete monitoring check: " + e.getMessage());

            return ResponseEntity.internalServerError().body(response);
        }
    }
}