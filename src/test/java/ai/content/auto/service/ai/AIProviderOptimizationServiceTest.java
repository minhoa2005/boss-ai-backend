package ai.content.auto.service.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AIProviderOptimizationServiceTest {

    @Mock
    private AIProviderMetricsService metricsService;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private AIProvider openAIProvider;

    @Mock
    private AIProvider geminiProvider;

    private AIProviderOptimizationService optimizationService;
    private List<AIProvider> providers;

    @BeforeEach
    void setUp() {
        providers = Arrays.asList(openAIProvider, geminiProvider);
        optimizationService = new AIProviderOptimizationService(providers, metricsService, redisTemplate);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void testGetOptimizedProvider_ReturnsRecommendation() {
        // Arrange
        ProviderSelectionCriteria criteria = ProviderSelectionCriteria.builder()
                .contentType("blog")
                .language("en")
                .prioritizeQuality(true)
                .build();

        setupMockProviders();

        // Act
        ProviderRecommendation recommendation = optimizationService.getOptimizedProvider(criteria);

        // Assert
        assertNotNull(recommendation);
        assertNotNull(recommendation.getPrimaryProvider());
        assertTrue(recommendation.getPrimaryScore() >= 0);
        assertNotNull(recommendation.getOptimizationReason());
        assertTrue(recommendation.getConfidence() >= 0);
    }

    @Test
    void testGetOptimizedProvider_PrioritizesCost() {
        // Arrange
        ProviderSelectionCriteria criteria = ProviderSelectionCriteria.builder()
                .contentType("blog")
                .prioritizeCost(true)
                .build();

        setupMockProviders();

        // Make OpenAI cheaper
        when(openAIProvider.getCostPerToken()).thenReturn(new BigDecimal("0.0001"));
        when(geminiProvider.getCostPerToken()).thenReturn(new BigDecimal("0.0005"));

        // Act
        ProviderRecommendation recommendation = optimizationService.getOptimizedProvider(criteria);

        // Assert
        assertNotNull(recommendation);
        assertNotNull(recommendation.getPrimaryProvider());
        assertEquals("OpenAI", recommendation.getPrimaryProvider().getName());
        assertNotNull(recommendation.getOptimizationReason());
        assertTrue(recommendation.getOptimizationReason().toLowerCase().contains("cost") ||
                recommendation.getOptimizationReason().toLowerCase().contains("efficiency"));
    }

    @Test
    void testGetOptimizedProvider_PrioritizesQuality() {
        // Arrange
        ProviderSelectionCriteria criteria = ProviderSelectionCriteria.builder()
                .contentType("blog")
                .prioritizeQuality(true)
                .build();

        setupMockProviders();

        // Make Gemini higher quality
        when(openAIProvider.getQualityScore()).thenReturn(7.0);
        when(geminiProvider.getQualityScore()).thenReturn(9.0);

        // Act
        ProviderRecommendation recommendation = optimizationService.getOptimizedProvider(criteria);

        // Assert
        assertNotNull(recommendation);
        assertNotNull(recommendation.getPrimaryProvider());
        assertEquals("Gemini", recommendation.getPrimaryProvider().getName());
        assertNotNull(recommendation.getOptimizationReason());
        assertTrue(recommendation.getOptimizationReason().toLowerCase().contains("quality") ||
                recommendation.getOptimizationReason().toLowerCase().contains("output"));
    }

    @Test
    void testGetOptimizedProvider_FiltersUnavailableProviders() {
        // Arrange
        ProviderSelectionCriteria criteria = ProviderSelectionCriteria.builder()
                .contentType("blog")
                .build();

        setupMockProviders();

        // Make OpenAI unavailable
        when(openAIProvider.isAvailable()).thenReturn(false);

        // Act
        ProviderRecommendation recommendation = optimizationService.getOptimizedProvider(criteria);

        // Assert
        assertNotNull(recommendation);
        assertNotNull(recommendation.getPrimaryProvider());
        assertEquals("Gemini", recommendation.getPrimaryProvider().getName());
    }

    @Test
    void testGetOptimizedProvider_ThrowsExceptionWhenNoProvidersAvailable() {
        // Arrange
        ProviderSelectionCriteria criteria = ProviderSelectionCriteria.builder()
                .contentType("blog")
                .build();

        when(openAIProvider.isAvailable()).thenReturn(false);
        when(geminiProvider.isAvailable()).thenReturn(false);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            optimizationService.getOptimizedProvider(criteria);
        });
    }

    @Test
    void testAnalyzeProviderPerformance_ReturnsAnalysis() {
        // Arrange
        String providerName = "OpenAI";
        int hours = 24;

        Map<String, Object> metrics = new HashMap<>();
        metrics.put("success_rate", "0.95");
        metrics.put("avg_response_time", "2000");
        metrics.put("avg_quality_score", "8.5");
        metrics.put("consecutive_failures", "0");

        when(metricsService.getProviderMetrics(providerName)).thenReturn(metrics);

        // Act
        ProviderPerformanceAnalysis analysis = optimizationService.analyzeProviderPerformance(providerName, hours);

        // Assert
        assertNotNull(analysis);
        assertEquals(providerName, analysis.getProviderName());
        assertEquals(hours, analysis.getAnalysisWindow());
        assertEquals(0.95, analysis.getCurrentSuccessRate());
        assertEquals(2000L, analysis.getCurrentAvgResponseTime());
        assertEquals(8.5, analysis.getCurrentQualityScore());
        assertNotNull(analysis.getRiskLevel());
        assertNotNull(analysis.getRecommendations());
    }

    @Test
    void testOptimizeRouting_ReturnsStrategy() {
        // Arrange
        setupMockProviders();

        // Act
        ProviderRoutingStrategy strategy = optimizationService.optimizeRouting();

        // Assert
        assertNotNull(strategy);
        assertNotNull(strategy.getStrategy());
        assertNotNull(strategy.getRoutingWeights());
        assertNotNull(strategy.getFailoverOrder());
        assertNotNull(strategy.getOptimizationReason());
        assertTrue(strategy.getRoutingWeights().size() > 0);
    }

    @Test
    void testOptimizeRouting_LoadBalancedWhenOverloaded() {
        // Arrange
        setupMockProviders();

        // Make one provider overloaded
        when(openAIProvider.getCurrentLoad()).thenReturn(0.9);
        when(geminiProvider.getCurrentLoad()).thenReturn(0.3);

        // Act
        ProviderRoutingStrategy strategy = optimizationService.optimizeRouting();

        // Assert
        assertEquals(AIProviderOptimizationService.RoutingStrategy.LOAD_BALANCED, strategy.getStrategy());
        assertTrue(strategy.isLoadBalancingEnabled());
    }

    @Test
    void testGetCostOptimizationRecommendation_ReturnsRecommendation() {
        // Arrange
        setupMockProviders();
        setupMockMetrics();

        // Act
        CostOptimizationRecommendation recommendation = optimizationService.getCostOptimizationRecommendation();

        // Assert
        assertNotNull(recommendation);
        assertTrue(recommendation.getCurrentAverageCost() > 0);
        assertNotNull(recommendation.getCheapestProvider());
        assertTrue(recommendation.getPotentialSavingsPerRequest() >= 0);
        assertNotNull(recommendation.getRecommendations());
        assertNotNull(recommendation.getProviderCostAnalysis());
    }

    private void setupMockProviders() {
        // OpenAI Provider
        when(openAIProvider.getName()).thenReturn("OpenAI");
        when(openAIProvider.isAvailable()).thenReturn(true);
        when(openAIProvider.getCostPerToken()).thenReturn(new BigDecimal("0.0004"));
        when(openAIProvider.getSuccessRate()).thenReturn(0.95);
        when(openAIProvider.getQualityScore()).thenReturn(8.0);
        when(openAIProvider.getAverageResponseTime()).thenReturn(2000L);
        when(openAIProvider.getCurrentLoad()).thenReturn(0.3);

        ProviderCapabilities openAICapabilities = ProviderCapabilities.builder()
                .supportedContentTypes(Set.of("blog", "article", "social"))
                .supportedLanguages(Set.of("en", "vi"))
                .maxTokensPerRequest(4000)
                .maxRequestsPerMinute(60)
                .supportsStreaming(false)
                .supportsFunctionCalling(true)
                .supportsImageGeneration(false)
                .supportsImageAnalysis(false)
                .minQualityScore(3.0)
                .maxQualityScore(9.0)
                .build();
        when(openAIProvider.getCapabilities()).thenReturn(openAICapabilities);

        // Gemini Provider
        when(geminiProvider.getName()).thenReturn("Gemini");
        when(geminiProvider.isAvailable()).thenReturn(true);
        when(geminiProvider.getCostPerToken()).thenReturn(new BigDecimal("0.0003"));
        when(geminiProvider.getSuccessRate()).thenReturn(0.92);
        when(geminiProvider.getQualityScore()).thenReturn(7.5);
        when(geminiProvider.getAverageResponseTime()).thenReturn(2500L);
        when(geminiProvider.getCurrentLoad()).thenReturn(0.4);

        ProviderCapabilities geminiCapabilities = ProviderCapabilities.builder()
                .supportedContentTypes(Set.of("blog", "article", "social"))
                .supportedLanguages(Set.of("en", "vi"))
                .maxTokensPerRequest(4000)
                .maxRequestsPerMinute(60)
                .supportsStreaming(false)
                .supportsFunctionCalling(true)
                .supportsImageGeneration(false)
                .supportsImageAnalysis(false)
                .minQualityScore(3.0)
                .maxQualityScore(9.0)
                .build();
        when(geminiProvider.getCapabilities()).thenReturn(geminiCapabilities);

        // Setup metrics for both providers
        setupMockMetrics();
    }

    private void setupMockMetrics() {
        Map<String, Object> openAIMetrics = new HashMap<>();
        openAIMetrics.put("total_requests", "1000");
        openAIMetrics.put("successful_requests", "950");
        openAIMetrics.put("success_rate", "0.95");
        openAIMetrics.put("avg_response_time", "2000");
        openAIMetrics.put("consecutive_failures", "0");

        Map<String, Object> geminiMetrics = new HashMap<>();
        geminiMetrics.put("total_requests", "800");
        geminiMetrics.put("successful_requests", "736");
        geminiMetrics.put("success_rate", "0.92");
        geminiMetrics.put("avg_response_time", "2500");
        geminiMetrics.put("consecutive_failures", "1");

        when(metricsService.getProviderMetrics("OpenAI")).thenReturn(openAIMetrics);
        when(metricsService.getProviderMetrics("Gemini")).thenReturn(geminiMetrics);
    }
}