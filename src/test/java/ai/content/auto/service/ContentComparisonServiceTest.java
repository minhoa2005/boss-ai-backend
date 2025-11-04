package ai.content.auto.service;

import ai.content.auto.dtos.ContentVersionComparisonDto;
import ai.content.auto.entity.ContentVersion;
import ai.content.auto.entity.User;
import ai.content.auto.mapper.ContentVersionMapper;
import ai.content.auto.repository.ContentVersionRepository;
import ai.content.auto.util.SecurityUtil;
import ai.content.auto.util.TextDifferenceAnalyzer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ContentComparisonService.
 * Tests core comparison functionality.
 * 
 * Requirements: 1.3, 1.4
 */
@ExtendWith(MockitoExtension.class)
class ContentComparisonServiceTest {

    @Mock
    private ContentVersionRepository contentVersionRepository;

    @Mock
    private ContentVersionMapper contentVersionMapper;

    @Mock
    private SecurityUtil securityUtil;

    @Mock
    private TextDifferenceAnalyzer textDifferenceAnalyzer;

    @Mock
    private VersionRecommendationEngine recommendationEngine;

    @InjectMocks
    private ContentComparisonService contentComparisonService;

    private ContentVersion versionA;
    private ContentVersion versionB;
    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");

        versionA = ContentVersion.builder()
                .id(1L)
                .contentId(100L)
                .versionNumber(1)
                .content("Original content for testing")
                .title("Original Title")
                .aiProvider("OpenAI")
                .aiModel("gpt-3.5-turbo")
                .qualityScore(BigDecimal.valueOf(75.0))
                .readabilityScore(BigDecimal.valueOf(80.0))
                .seoScore(BigDecimal.valueOf(70.0))
                .wordCount(5)
                .characterCount(30)
                .createdBy(1L)
                .createdAt(Instant.now().minusSeconds(3600))
                .build();

        versionB = ContentVersion.builder()
                .id(2L)
                .contentId(100L)
                .versionNumber(2)
                .content("Updated content for testing with improvements")
                .title("Updated Title")
                .aiProvider("OpenAI")
                .aiModel("gpt-3.5-turbo")
                .qualityScore(BigDecimal.valueOf(85.0))
                .readabilityScore(BigDecimal.valueOf(82.0))
                .seoScore(BigDecimal.valueOf(75.0))
                .wordCount(7)
                .characterCount(45)
                .createdBy(1L)
                .createdAt(Instant.now())
                .build();
    }

    @Test
    void compareVersions_ShouldReturnDetailedComparison_WhenValidVersionsProvided() {
        // Arrange
        Long contentId = 100L;
        Integer versionNumberA = 1;
        Integer versionNumberB = 2;

        when(securityUtil.getCurrentUserId()).thenReturn(1L);
        when(securityUtil.getCurrentUser()).thenReturn(testUser);
        when(contentVersionRepository.findByContentIdOrderByVersionNumberDesc(contentId))
                .thenReturn(java.util.List.of(versionB, versionA));
        when(contentVersionRepository.findByContentIdAndVersionNumber(contentId, versionNumberA))
                .thenReturn(Optional.of(versionA));
        when(contentVersionRepository.findByContentIdAndVersionNumber(contentId, versionNumberB))
                .thenReturn(Optional.of(versionB));
        when(contentVersionMapper.toDto(any(ContentVersion.class)))
                .thenReturn(new ai.content.auto.dtos.ContentVersionDto());
        when(textDifferenceAnalyzer.analyzeTextDifferences(anyString(), anyString()))
                .thenReturn(java.util.List.of());
        when(textDifferenceAnalyzer.createComparisonSummary(anyString(), anyString(), anyList()))
                .thenReturn(ContentVersionComparisonDto.ComparisonSummaryDto.builder()
                        .totalChanges(3)
                        .additions(2)
                        .deletions(0)
                        .modifications(1)
                        .similarityPercentage(BigDecimal.valueOf(85.0))
                        .build());
        when(recommendationEngine.generateRecommendation(any(), any(), any(), any()))
                .thenReturn(ContentVersionComparisonDto.VersionRecommendationDto.builder()
                        .recommendedVersion("B")
                        .confidenceScore(BigDecimal.valueOf(75.0))
                        .reasoning("Version B shows improved quality scores")
                        .build());

        // Act
        ContentVersionComparisonDto result = contentComparisonService.compareVersions(
                contentId, versionNumberA, versionNumberB);

        // Assert
        assertNotNull(result);
        assertEquals(contentId, result.getContentId());
        assertNotNull(result.getVersionA());
        assertNotNull(result.getVersionB());
        assertNotNull(result.getComparisonSummary());
        assertNotNull(result.getMetricsComparison());
        assertNotNull(result.getPerformanceComparison());
        assertNotNull(result.getRecommendation());
        assertEquals(1L, result.getComparedBy());
        assertEquals("testuser", result.getComparedByUsername());
        assertNotNull(result.getComparedAt());

        // Verify interactions
        verify(contentVersionRepository).findByContentIdAndVersionNumber(contentId, versionNumberA);
        verify(contentVersionRepository).findByContentIdAndVersionNumber(contentId, versionNumberB);
        verify(textDifferenceAnalyzer).analyzeTextDifferences(versionA.getContent(), versionB.getContent());
        verify(recommendationEngine).generateRecommendation(any(), any(), any(), any());
    }

    @Test
    void compareVersions_ShouldThrowException_WhenSameVersionsProvided() {
        // Arrange
        Long contentId = 100L;
        Integer versionNumber = 1;

        when(securityUtil.getCurrentUserId()).thenReturn(1L);

        // Act & Assert
        assertThrows(ai.content.auto.exception.BusinessException.class,
                () -> contentComparisonService.compareVersions(contentId, versionNumber, versionNumber));
    }

    @Test
    void compareVersions_ShouldThrowException_WhenVersionNotFound() {
        // Arrange
        Long contentId = 100L;
        Integer versionNumberA = 1;
        Integer versionNumberB = 999; // Non-existent version

        when(securityUtil.getCurrentUserId()).thenReturn(1L);
        when(contentVersionRepository.findByContentIdOrderByVersionNumberDesc(contentId))
                .thenReturn(java.util.List.of(versionA));
        when(contentVersionRepository.findByContentIdAndVersionNumber(contentId, versionNumberA))
                .thenReturn(Optional.of(versionA));
        when(contentVersionRepository.findByContentIdAndVersionNumber(contentId, versionNumberB))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ai.content.auto.exception.NotFoundException.class,
                () -> contentComparisonService.compareVersions(contentId, versionNumberA, versionNumberB));
    }

    @Test
    void getSideBySideComparison_ShouldReturnFormattedData_WhenValidVersionsProvided() {
        // Arrange
        Long contentId = 100L;
        Integer versionNumberA = 1;
        Integer versionNumberB = 2;

        when(securityUtil.getCurrentUserId()).thenReturn(1L);
        when(contentVersionRepository.findByContentIdOrderByVersionNumberDesc(contentId))
                .thenReturn(java.util.List.of(versionB, versionA));
        when(contentVersionRepository.findByContentIdAndVersionNumber(contentId, versionNumberA))
                .thenReturn(Optional.of(versionA));
        when(contentVersionRepository.findByContentIdAndVersionNumber(contentId, versionNumberB))
                .thenReturn(Optional.of(versionB));
        when(textDifferenceAnalyzer.analyzeTextDifferences(anyString(), anyString()))
                .thenReturn(java.util.List.of());
        when(textDifferenceAnalyzer.createComparisonSummary(anyString(), anyString(), anyList()))
                .thenReturn(ContentVersionComparisonDto.ComparisonSummaryDto.builder()
                        .totalChanges(3)
                        .similarityPercentage(BigDecimal.valueOf(85.0))
                        .build());

        // Act
        java.util.Map<String, Object> result = contentComparisonService.getSideBySideComparison(
                contentId, versionNumberA, versionNumberB);

        // Assert
        assertNotNull(result);
        assertTrue(result.containsKey("versionA"));
        assertTrue(result.containsKey("versionB"));
        assertTrue(result.containsKey("differences"));
        assertTrue(result.containsKey("summary"));
        assertTrue(result.containsKey("quickMetrics"));

        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> versionAData = (java.util.Map<String, Object>) result.get("versionA");
        assertEquals(1, versionAData.get("number"));
        assertEquals("Original content for testing", versionAData.get("content"));

        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> versionBData = (java.util.Map<String, Object>) result.get("versionB");
        assertEquals(2, versionBData.get("number"));
        assertEquals("Updated content for testing with improvements", versionBData.get("content"));
    }
}