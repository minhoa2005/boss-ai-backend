package ai.content.auto.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Unit tests for ContentVersion entity.
 * Tests entity validation, business logic, and data integrity.
 */
class ContentVersionTest {

    private ContentVersion contentVersion;

    @BeforeEach
    void setUp() {
        Map<String, Object> generationParams = new HashMap<>();
        generationParams.put("temperature", 0.7);
        generationParams.put("max_tokens", 1000);
        generationParams.put("model", "gpt-4");

        contentVersion = ContentVersion.builder()
                .contentId(1L)
                .versionNumber(1)
                .content("Test content for version 1")
                .title("Test Title")
                .generationParams(generationParams)
                .aiProvider("openai")
                .aiModel("gpt-4")
                .tokensUsed(150)
                .generationCost(BigDecimal.valueOf(0.05))
                .processingTimeMs(1500L)
                .readabilityScore(BigDecimal.valueOf(85.5))
                .seoScore(BigDecimal.valueOf(78.2))
                .qualityScore(BigDecimal.valueOf(92.1))
                .sentimentScore(BigDecimal.valueOf(0.3))
                .wordCount(25)
                .characterCount(150)
                .industry("technology")
                .targetAudience("developers")
                .tone("professional")
                .language("vi")
                .createdBy(1L)
                .createdAt(Instant.now())
                .build();
    }

    @Test
    void testContentVersionCreation() {
        assertNotNull(contentVersion);
        assertEquals(1L, contentVersion.getContentId());
        assertEquals(1, contentVersion.getVersionNumber());
        assertEquals("Test content for version 1", contentVersion.getContent());
        assertEquals("openai", contentVersion.getAiProvider());
        assertEquals("gpt-4", contentVersion.getAiModel());
    }

    @Test
    void testGenerationParamsJsonb() {
        Map<String, Object> params = contentVersion.getGenerationParams();
        assertNotNull(params);
        assertEquals(0.7, params.get("temperature"));
        assertEquals(1000, params.get("max_tokens"));
        assertEquals("gpt-4", params.get("model"));
    }

    @Test
    void testCalculateOverallScore() {
        // Test with all scores present
        BigDecimal overallScore = contentVersion.calculateOverallScore();
        assertNotNull(overallScore);

        // Expected: (92.1 * 0.5) + (85.5 * 0.3) + (78.2 * 0.2) = 46.05 + 25.65 + 15.64
        // = 87.34
        BigDecimal expected = BigDecimal.valueOf(87.34);
        assertEquals(0, expected.compareTo(overallScore),
                "Overall score calculation should match expected value");
    }

    @Test
    void testCalculateOverallScoreWithMissingScores() {
        // Test with only quality score
        ContentVersion versionWithPartialScores = ContentVersion.builder()
                .qualityScore(BigDecimal.valueOf(90.0))
                .build();

        BigDecimal overallScore = versionWithPartialScores.calculateOverallScore();
        assertNotNull(overallScore);
        // Expected: 90.0 * 0.5 = 45.0 (only quality score contributes)
        assertEquals(0, BigDecimal.valueOf(45.0).compareTo(overallScore));
    }

    @Test
    void testCalculateOverallScoreWithNoScores() {
        ContentVersion versionWithNoScores = ContentVersion.builder().build();
        BigDecimal overallScore = versionWithNoScores.calculateOverallScore();
        assertNull(overallScore);
    }

    @Test
    void testPerformsBetterThan() {
        ContentVersion lowerQualityVersion = ContentVersion.builder()
                .qualityScore(BigDecimal.valueOf(70.0))
                .readabilityScore(BigDecimal.valueOf(65.0))
                .seoScore(BigDecimal.valueOf(60.0))
                .build();

        assertTrue(contentVersion.performsBetterThan(lowerQualityVersion));
        assertFalse(lowerQualityVersion.performsBetterThan(contentVersion));
    }

    @Test
    void testPerformsBetterThanWithNullScores() {
        ContentVersion versionWithNullScores = ContentVersion.builder().build();

        assertFalse(contentVersion.performsBetterThan(versionWithNullScores));
        assertFalse(versionWithNullScores.performsBetterThan(contentVersion));
    }

    @Test
    void testPrePersistCallback() {
        ContentVersion newVersion = ContentVersion.builder()
                .contentId(1L)
                .versionNumber(1)
                .content("Test content")
                .aiProvider("openai")
                .createdBy(1L)
                .build();

        // Simulate @PrePersist
        newVersion.onCreate();

        assertNotNull(newVersion.getCreatedAt());
        assertNotNull(newVersion.getUpdatedAt());
        assertEquals("vi", newVersion.getLanguage());
    }

    @Test
    void testPreUpdateCallback() {
        // Ensure updatedAt is set first
        if (contentVersion.getUpdatedAt() == null) {
            contentVersion.onCreate(); // Initialize timestamps
        }
        Instant originalUpdatedAt = contentVersion.getUpdatedAt();

        // Wait a bit to ensure timestamp difference
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Simulate @PreUpdate
        contentVersion.onUpdate();

        assertNotNull(contentVersion.getUpdatedAt());
        assertTrue(contentVersion.getUpdatedAt().isAfter(originalUpdatedAt));
    }

    @Test
    void testBuilderPattern() {
        ContentVersion builtVersion = ContentVersion.builder()
                .contentId(2L)
                .versionNumber(3)
                .content("Built content")
                .title("Built Title")
                .aiProvider("claude")
                .aiModel("claude-3")
                .createdBy(2L)
                .build();

        assertEquals(2L, builtVersion.getContentId());
        assertEquals(3, builtVersion.getVersionNumber());
        assertEquals("Built content", builtVersion.getContent());
        assertEquals("claude", builtVersion.getAiProvider());
        assertEquals("claude-3", builtVersion.getAiModel());
        assertEquals(2L, builtVersion.getCreatedBy());
    }

    @Test
    void testScoreValidationRanges() {
        // Test valid score ranges
        assertDoesNotThrow(() -> {
            ContentVersion.builder()
                    .readabilityScore(BigDecimal.valueOf(85.5))
                    .seoScore(BigDecimal.valueOf(78.2))
                    .qualityScore(BigDecimal.valueOf(92.1))
                    .sentimentScore(BigDecimal.valueOf(0.3))
                    .build();
        });

        // Note: Actual validation would be handled by Bean Validation at runtime
        // These tests verify the entity can hold valid values
    }

    @Test
    void testDefaultValues() {
        ContentVersion defaultVersion = new ContentVersion();

        // Test that default values are properly initialized
        assertNotNull(defaultVersion.getGenerationParams());
        assertTrue(defaultVersion.getGenerationParams().isEmpty());
    }
}