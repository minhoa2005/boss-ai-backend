package ai.content.auto.service.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Test for AI Provider Dashboard Service
 */
@ExtendWith(MockitoExtension.class)
class AIProviderDashboardServiceTest {

    @Mock
    private List<AIProvider> providers;

    @Mock
    private AIProviderMetricsService metricsService;

    @Mock
    private AIProviderAlertingService alertingService;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    private AIProviderDashboardService dashboardService;

    @BeforeEach
    void setUp() {
        dashboardService = new AIProviderDashboardService(
                providers,
                metricsService,
                alertingService,
                redisTemplate);
    }

    @Test
    void testRecordHourlyPerformance() {
        // Given
        String providerName = "OpenAI";
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("success_rate", 0.95);
        metrics.put("avg_response_time", 1500L);
        metrics.put("total_requests", 100L);
        metrics.put("failed_requests", 5L);

        when(redisTemplate.opsForHash()).thenReturn(mock(org.springframework.data.redis.core.HashOperations.class));

        // When
        dashboardService.recordHourlyPerformance(providerName, metrics);

        // Then
        verify(redisTemplate.opsForHash()).putAll(anyString(), any(Map.class));
        verify(redisTemplate).expire(anyString(), anyLong(), any());
    }

    @Test
    void testGetProviderComparison() {
        // Given
        List<AIProvider> mockProviders = new ArrayList<>();
        AIProvider mockProvider = mock(AIProvider.class);
        when(mockProvider.getName()).thenReturn("OpenAI");
        when(mockProvider.getCostPerToken()).thenReturn(java.math.BigDecimal.valueOf(0.002));
        when(mockProvider.isAvailable()).thenReturn(true);

        ProviderHealthStatus mockHealth = ProviderHealthStatus.builder()
                .healthLevel(ProviderHealthStatus.HealthLevel.HEALTHY)
                .build();
        when(mockProvider.getHealthStatus()).thenReturn(mockHealth);

        mockProviders.add(mockProvider);

        AIProviderDashboardService dashboardServiceWithMockProviders = new AIProviderDashboardService(
                mockProviders,
                metricsService,
                alertingService,
                redisTemplate);

        Map<String, Object> mockMetrics = new HashMap<>();
        mockMetrics.put("success_rate", "0.95");
        mockMetrics.put("avg_response_time", "1500");
        mockMetrics.put("avg_quality_score", "8.5");
        mockMetrics.put("total_requests", "100");

        when(metricsService.getProviderMetrics(anyString())).thenReturn(mockMetrics);

        // When
        ProviderComparisonData comparison = dashboardServiceWithMockProviders.getProviderComparison();

        // Then
        assertNotNull(comparison);
        assertNotNull(comparison.getProviders());
        assertEquals(1, comparison.getProviders().size());

        ProviderComparisonItem item = comparison.getProviders().get(0);
        assertEquals("OpenAI", item.getProviderName());
        assertEquals(0.95, item.getSuccessRate());
        assertEquals(1500L, item.getAvgResponseTime());
    }

    @Test
    void testGetPerformanceInsights() {
        // Given
        List<AIProvider> mockProviders = new ArrayList<>();
        AIProvider mockProvider = mock(AIProvider.class);
        when(mockProvider.getName()).thenReturn("OpenAI");
        when(mockProvider.getCostPerToken()).thenReturn(java.math.BigDecimal.valueOf(0.002));

        ProviderHealthStatus mockHealth = ProviderHealthStatus.builder()
                .healthLevel(ProviderHealthStatus.HealthLevel.HEALTHY)
                .consecutiveFailures(0)
                .build();
        when(mockProvider.getHealthStatus()).thenReturn(mockHealth);

        mockProviders.add(mockProvider);

        AIProviderDashboardService dashboardServiceWithMockProviders = new AIProviderDashboardService(
                mockProviders,
                metricsService,
                alertingService,
                redisTemplate);

        Map<String, Object> mockMetrics = new HashMap<>();
        mockMetrics.put("success_rate", "0.95");
        mockMetrics.put("avg_response_time", "1500");
        mockMetrics.put("total_requests", "100");

        when(metricsService.getProviderMetrics(anyString())).thenReturn(mockMetrics);

        // When
        ProviderPerformanceInsights insights = dashboardServiceWithMockProviders.getPerformanceInsights();

        // Then
        assertNotNull(insights);
        assertNotNull(insights.getInsights());
        assertNotNull(insights.getRecommendations());
        assertNotNull(insights.getGeneratedAt());
    }
}