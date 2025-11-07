package ai.content.auto.service;

import ai.content.auto.entity.ContentTemplate;
import ai.content.auto.entity.User;
import ai.content.auto.repository.ContentTemplateRepository;
import ai.content.auto.repository.TemplateUsageLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TemplatePopularityServiceTest {

    @Mock
    private ContentTemplateRepository templateRepository;

    @Mock
    private TemplateUsageLogRepository usageLogRepository;

    @InjectMocks
    private TemplatePopularityService popularityService;

    private ContentTemplate testTemplate;
    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");

        testTemplate = new ContentTemplate();
        testTemplate.setId(1L);
        testTemplate.setName("Test Template");
        testTemplate.setCategory("Marketing");
        testTemplate.setContentType("Blog Post");
        testTemplate.setUsageCount(50);
        testTemplate.setAverageRating(BigDecimal.valueOf(4.5));
        testTemplate.setSuccessRate(BigDecimal.valueOf(95.0));
        testTemplate.setCreatedBy(testUser);
        testTemplate.setCreatedAt(OffsetDateTime.now());
    }

    @Test
    void testCalculatePopularityScore_WithAllMetrics() {
        // Given
        when(usageLogRepository.countUsageByTemplateInDateRange(
                eq(testTemplate.getId()), any(OffsetDateTime.class), any(OffsetDateTime.class)))
                .thenReturn(20L);

        // When
        double score = popularityService.calculatePopularityScore(testTemplate);

        // Then
        assertTrue(score > 0, "Popularity score should be greater than 0");
        assertTrue(score <= 100, "Popularity score should not exceed 100");

        // Verify the score is calculated based on weighted factors
        // Expected: (50/10 * 0.4) + (4.5*20 * 0.3) + (95 * 0.2) + (20 * 0.1)
        // = (5 * 0.4) + (90 * 0.3) + (95 * 0.2) + (20 * 0.1)
        // = 2 + 27 + 19 + 2 = 50
        assertEquals(50.0, score, 1.0, "Popularity score should be approximately 50");
    }

    @Test
    void testCalculatePopularityScore_WithNoRating() {
        // Given
        testTemplate.setAverageRating(null);
        when(usageLogRepository.countUsageByTemplateInDateRange(
                eq(testTemplate.getId()), any(OffsetDateTime.class), any(OffsetDateTime.class)))
                .thenReturn(20L);

        // When
        double score = popularityService.calculatePopularityScore(testTemplate);

        // Then
        assertTrue(score > 0, "Popularity score should be greater than 0 even without rating");
        assertTrue(score < 50, "Popularity score should be lower without rating");
    }

    @Test
    void testCalculatePopularityScore_WithNoSuccessRate() {
        // Given
        testTemplate.setSuccessRate(null);
        when(usageLogRepository.countUsageByTemplateInDateRange(
                eq(testTemplate.getId()), any(OffsetDateTime.class), any(OffsetDateTime.class)))
                .thenReturn(20L);

        // When
        double score = popularityService.calculatePopularityScore(testTemplate);

        // Then
        assertTrue(score > 0, "Popularity score should be greater than 0 even without success rate");
    }

    @Test
    void testUpdateTemplateMetrics_Success() {
        // Given
        when(templateRepository.findById(testTemplate.getId()))
                .thenReturn(Optional.of(testTemplate));
        when(usageLogRepository.getAverageGenerationTimeByTemplate(testTemplate.getId()))
                .thenReturn(2500.0);
        when(usageLogRepository.getAverageQualityScoreByTemplate(testTemplate.getId()))
                .thenReturn(4.2);
        when(usageLogRepository.getSuccessRateByTemplate(testTemplate.getId()))
                .thenReturn(92.5);
        when(templateRepository.save(any(ContentTemplate.class)))
                .thenReturn(testTemplate);

        // When
        popularityService.updateTemplateMetrics(testTemplate.getId());

        // Then
        verify(templateRepository).save(argThat(template -> template.getAverageGenerationTimeMs() == 2500L &&
                template.getAverageRating().compareTo(BigDecimal.valueOf(4.2)) == 0 &&
                template.getSuccessRate().compareTo(BigDecimal.valueOf(92.5)) == 0));
    }

    @Test
    void testUpdateTemplateMetrics_TemplateNotFound() {
        // Given
        when(templateRepository.findById(999L))
                .thenReturn(Optional.empty());

        // When/Then
        assertThrows(IllegalArgumentException.class, () -> popularityService.updateTemplateMetrics(999L));
    }

    @Test
    void testGetTrendingTemplateIds() {
        // Given
        when(usageLogRepository.findMostPopularTemplates(
                any(OffsetDateTime.class), any()))
                .thenReturn(java.util.List.of(
                        new Object[] { 1L, 50L },
                        new Object[] { 2L, 45L },
                        new Object[] { 3L, 40L }));

        // When
        var trendingIds = popularityService.getTrendingTemplateIds(3);

        // Then
        assertEquals(3, trendingIds.size());
        assertEquals(1L, trendingIds.get(0));
        assertEquals(2L, trendingIds.get(1));
        assertEquals(3L, trendingIds.get(2));
    }
}
