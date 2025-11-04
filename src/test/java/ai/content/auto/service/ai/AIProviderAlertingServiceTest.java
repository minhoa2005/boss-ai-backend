package ai.content.auto.service.ai;

import ai.content.auto.service.monitoring.SystemMonitoringService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Test for AI Provider Alerting Service
 */
@ExtendWith(MockitoExtension.class)
class AIProviderAlertingServiceTest {

    @Mock
    private List<AIProvider> providers;

    @Mock
    private AIProviderMetricsService metricsService;

    @Mock
    private SystemMonitoringService systemMonitoringService;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private AIProviderAlertingService alertingService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        alertingService = new AIProviderAlertingService(
                providers,
                metricsService,
                systemMonitoringService,
                redisTemplate);
    }

    @Test
    void testRecordProviderCost() {
        // Given
        String providerName = "OpenAI";
        BigDecimal cost = new BigDecimal("0.50");

        // When
        alertingService.recordProviderCost(providerName, cost);

        // Then
        verify(valueOperations, times(2)).increment(anyString(), anyDouble());
    }

    @Test
    void testGetProviderCostSummary() {
        // Given
        List<AIProvider> mockProviders = new ArrayList<>();
        AIProvider mockProvider = mock(AIProvider.class);
        when(mockProvider.getName()).thenReturn("OpenAI");
        mockProviders.add(mockProvider);

        AIProviderAlertingService alertingServiceWithMockProviders = new AIProviderAlertingService(
                mockProviders,
                metricsService,
                systemMonitoringService,
                redisTemplate);

        when(valueOperations.get(anyString())).thenReturn("10.50");

        // When
        Map<String, ProviderCostSummary> costSummary = alertingServiceWithMockProviders.getProviderCostSummary();

        // Then
        assertNotNull(costSummary);
        assertTrue(costSummary.containsKey("OpenAI"));

        ProviderCostSummary summary = costSummary.get("OpenAI");
        assertNotNull(summary);
        assertEquals("OpenAI", summary.getProviderName());
    }

    @Test
    void testGetProviderAlertHistory() {
        // Given
        String providerName = "OpenAI";
        int limit = 10;

        when(redisTemplate.opsForList()).thenReturn(mock(org.springframework.data.redis.core.ListOperations.class));
        when(redisTemplate.opsForList().range(anyString(), anyLong(), anyLong())).thenReturn(new ArrayList<>());

        // When
        List<Map<String, Object>> alertHistory = alertingService.getProviderAlertHistory(providerName, limit);

        // Then
        assertNotNull(alertHistory);
        assertTrue(alertHistory.isEmpty());
    }
}