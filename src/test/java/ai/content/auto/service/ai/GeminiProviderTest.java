package ai.content.auto.service.ai;

import ai.content.auto.dtos.ContentGenerateRequest;
import ai.content.auto.entity.N8nConfig;
import ai.content.auto.entity.User;
import ai.content.auto.exception.BusinessException;
import ai.content.auto.repository.N8nConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GeminiProviderTest {

    @Mock
    private N8nConfigRepository n8nConfigRepository;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private AIProviderMetricsService metricsService;

    @InjectMocks
    private GeminiProvider geminiProvider;

    private N8nConfig geminiConfig;
    private User testUser;
    private ContentGenerateRequest testRequest;

    @BeforeEach
    void setUp() {
        // Setup test data
        geminiConfig = new N8nConfig();
        geminiConfig.setAgentName("gemini");
        geminiConfig.setAgentUrl("https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent");
        geminiConfig.setXApiKey("test-api-key");
        geminiConfig.setModel("gemini-pro");
        geminiConfig.setTemperature(0.7);

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");

        testRequest = new ContentGenerateRequest();
        testRequest.setContent("Write a blog post about AI");
        testRequest.setContentType("blog");
        testRequest.setLanguage("vi");
        testRequest.setTone("professional");
    }

    @Test
    void testGetName() {
        assertEquals("Gemini", geminiProvider.getName());
    }

    @Test
    void testGetCostPerToken() {
        BigDecimal expectedCost = new BigDecimal("0.0002");
        assertEquals(expectedCost, geminiProvider.getCostPerToken());
    }

    @Test
    void testGetCapabilities() {
        ProviderCapabilities capabilities = geminiProvider.getCapabilities();

        assertNotNull(capabilities);
        assertEquals(8192, capabilities.getMaxTokensPerRequest());
        assertTrue(capabilities.getSupportedContentTypes().contains("blog"));
        assertTrue(capabilities.getSupportedLanguages().contains("vi"));
        assertTrue(capabilities.getSupportedTones().contains("professional"));
        assertTrue(capabilities.isSupportsImageAnalysis());
        assertFalse(capabilities.isSupportsImageGeneration());
    }

    @Test
    void testIsAvailable_WithHealthyProvider() {
        // Mock metrics service to return healthy metrics
        when(metricsService.getProviderMetrics(anyString())).thenReturn(
                java.util.Map.of(
                        "consecutive_failures", "0",
                        "error_rate", "0.0",
                        "avg_response_time", "2000"));

        // The provider should be available when healthy
        assertTrue(geminiProvider.isAvailable());
    }

    @Test
    void testGenerateContent_MissingConfig() {
        // Mock repository to return empty config
        when(n8nConfigRepository.findN8nConfigByAgentName("gemini"))
                .thenReturn(Optional.empty());

        // Should throw BusinessException when config is missing
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            geminiProvider.generateContent(testRequest, testUser);
        });

        assertEquals("Gemini configuration not found", exception.getMessage());
    }

    @Test
    void testGenerateContent_InvalidRequest() {
        // Test with null request
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            geminiProvider.generateContent(null, testUser);
        });

        assertEquals("Generate request is required", exception.getMessage());

        // Test with null user
        exception = assertThrows(BusinessException.class, () -> {
            geminiProvider.generateContent(testRequest, null);
        });

        assertEquals("User is required", exception.getMessage());

        // Test with empty content
        ContentGenerateRequest emptyRequest = new ContentGenerateRequest();
        emptyRequest.setContent("");

        exception = assertThrows(BusinessException.class, () -> {
            geminiProvider.generateContent(emptyRequest, testUser);
        });

        assertEquals("Content is required", exception.getMessage());
    }

    @Test
    void testGetHealthStatus() {
        // Mock metrics service
        when(metricsService.getProviderMetrics(anyString())).thenReturn(
                java.util.Map.of(
                        "consecutive_failures", "0",
                        "error_rate", "0.1",
                        "avg_response_time", "3000",
                        "last_success", String.valueOf(System.currentTimeMillis()),
                        "last_failure", String.valueOf(System.currentTimeMillis() - 10000)));

        ProviderHealthStatus healthStatus = geminiProvider.getHealthStatus();

        assertNotNull(healthStatus);
        assertTrue(healthStatus.isAvailable());
        assertEquals(ProviderHealthStatus.HealthLevel.HEALTHY, healthStatus.getHealthLevel());
        assertNotNull(healthStatus.getMessage());
        assertNotNull(healthStatus.getLastHealthCheck());
    }

    @Test
    void testGetHealthStatus_Degraded() {
        // Mock metrics service to return degraded metrics
        when(metricsService.getProviderMetrics(anyString())).thenReturn(
                java.util.Map.of(
                        "consecutive_failures", "2",
                        "error_rate", "0.3", // High error rate
                        "avg_response_time", "8000" // Slow response
                ));

        ProviderHealthStatus healthStatus = geminiProvider.getHealthStatus();

        assertNotNull(healthStatus);
        assertTrue(healthStatus.isAvailable());
        assertEquals(ProviderHealthStatus.HealthLevel.DEGRADED, healthStatus.getHealthLevel());
        assertTrue(healthStatus.getMessage().contains("degraded"));
    }

    @Test
    void testGetHealthStatus_Down() {
        // Mock metrics service to return down metrics
        when(metricsService.getProviderMetrics(anyString())).thenReturn(
                java.util.Map.of(
                        "consecutive_failures", "5", // Too many failures
                        "error_rate", "0.8",
                        "avg_response_time", "15000"));

        ProviderHealthStatus healthStatus = geminiProvider.getHealthStatus();

        assertNotNull(healthStatus);
        assertFalse(healthStatus.isAvailable());
        assertEquals(ProviderHealthStatus.HealthLevel.DOWN, healthStatus.getHealthLevel());
        assertTrue(healthStatus.getMessage().contains("down"));
    }

    @Test
    void testGetAverageResponseTime() {
        when(metricsService.calculateAverageResponseTime("Gemini")).thenReturn(2500L);

        assertEquals(2500L, geminiProvider.getAverageResponseTime());
    }

    @Test
    void testGetSuccessRate() {
        when(metricsService.calculateSuccessRate("Gemini")).thenReturn(0.95);

        assertEquals(0.95, geminiProvider.getSuccessRate());
    }

    @Test
    void testGetQualityScore() {
        when(metricsService.calculateAverageQualityScore("Gemini")).thenReturn(7.5);

        assertEquals(7.5, geminiProvider.getQualityScore());
    }

    @Test
    void testGetCurrentLoad() {
        // Mock metrics to simulate current load
        when(metricsService.getProviderMetrics("Gemini")).thenReturn(
                java.util.Map.of("total_requests", "30"));

        double load = geminiProvider.getCurrentLoad();

        assertTrue(load >= 0.0 && load <= 1.0);
        assertEquals(0.5, load, 0.01); // 30/60 = 0.5
    }
}