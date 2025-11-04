package ai.content.auto.util;

import ai.content.auto.dtos.ContentVersionComparisonDto.TextDifferenceDto;
import ai.content.auto.dtos.ContentVersionComparisonDto.ComparisonSummaryDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TextDifferenceAnalyzer.
 * Tests text comparison and similarity calculation functionality.
 * 
 * Requirements: 1.3, 1.4
 */
class TextDifferenceAnalyzerTest {

    private TextDifferenceAnalyzer textDifferenceAnalyzer;

    @BeforeEach
    void setUp() {
        textDifferenceAnalyzer = new TextDifferenceAnalyzer();
    }

    @Test
    void analyzeTextDifferences_ShouldReturnEmptyList_WhenTextsAreIdentical() {
        // Arrange
        String originalText = "This is a test content";
        String newText = "This is a test content";

        // Act
        List<TextDifferenceDto> differences = textDifferenceAnalyzer.analyzeTextDifferences(originalText, newText);

        // Assert
        assertNotNull(differences);
        assertTrue(differences.isEmpty());
    }

    @Test
    void analyzeTextDifferences_ShouldDetectChanges_WhenTextsAreDifferent() {
        // Arrange
        String originalText = "This is the original content";
        String newText = "This is the updated content with improvements";

        // Act
        List<TextDifferenceDto> differences = textDifferenceAnalyzer.analyzeTextDifferences(originalText, newText);

        // Assert
        assertNotNull(differences);
        assertFalse(differences.isEmpty());

        // Should detect some form of change
        boolean hasChanges = differences.stream()
                .anyMatch(diff -> !diff.getType().equals("UNCHANGED"));
        assertTrue(hasChanges);
    }

    @Test
    void analyzeTextDifferences_ShouldHandleNullInputs_Gracefully() {
        // Act & Assert - should not throw exceptions
        assertDoesNotThrow(() -> {
            List<TextDifferenceDto> differences1 = textDifferenceAnalyzer.analyzeTextDifferences(null, "test");
            assertNotNull(differences1);

            List<TextDifferenceDto> differences2 = textDifferenceAnalyzer.analyzeTextDifferences("test", null);
            assertNotNull(differences2);

            List<TextDifferenceDto> differences3 = textDifferenceAnalyzer.analyzeTextDifferences(null, null);
            assertNotNull(differences3);
        });
    }

    @Test
    void createComparisonSummary_ShouldCalculateCorrectStatistics() {
        // Arrange
        String originalText = "This is original content";
        String newText = "This is updated content with more words";
        List<TextDifferenceDto> differences = textDifferenceAnalyzer.analyzeTextDifferences(originalText, newText);

        // Act
        ComparisonSummaryDto summary = textDifferenceAnalyzer.createComparisonSummary(originalText, newText,
                differences);

        // Assert
        assertNotNull(summary);
        assertNotNull(summary.getTotalChanges());
        assertNotNull(summary.getSimilarityPercentage());
        assertNotNull(summary.getWordCountDifference());
        assertNotNull(summary.getCharacterCountDifference());

        // Word count difference should be positive (new text has more words)
        assertTrue(summary.getWordCountDifference() > 0);

        // Character count difference should be positive (new text is longer)
        assertTrue(summary.getCharacterCountDifference() > 0);

        // Similarity should be between 0 and 100
        assertTrue(summary.getSimilarityPercentage().compareTo(BigDecimal.ZERO) >= 0);
        assertTrue(summary.getSimilarityPercentage().compareTo(BigDecimal.valueOf(100)) <= 0);
    }

    @Test
    void calculateSimilarityPercentage_ShouldReturn100_ForIdenticalTexts() {
        // Arrange
        String text1 = "Identical content";
        String text2 = "Identical content";

        // Act
        BigDecimal similarity = textDifferenceAnalyzer.calculateSimilarityPercentage(text1, text2);

        // Assert
        assertEquals(BigDecimal.valueOf(100), similarity);
    }

    @Test
    void calculateSimilarityPercentage_ShouldReturn100_ForEmptyTexts() {
        // Arrange
        String text1 = "";
        String text2 = "";

        // Act
        BigDecimal similarity = textDifferenceAnalyzer.calculateSimilarityPercentage(text1, text2);

        // Assert
        assertEquals(BigDecimal.valueOf(100), similarity);
    }

    @Test
    void calculateSimilarityPercentage_ShouldReturnZero_WhenOneTextIsEmpty() {
        // Arrange
        String text1 = "Some content";
        String text2 = "";

        // Act
        BigDecimal similarity = textDifferenceAnalyzer.calculateSimilarityPercentage(text1, text2);

        // Assert
        assertEquals(BigDecimal.ZERO, similarity);
    }

    @Test
    void calculateSimilarityPercentage_ShouldReturnReasonableValue_ForSimilarTexts() {
        // Arrange
        String text1 = "This is a test content";
        String text2 = "This is a test content with additions";

        // Act
        BigDecimal similarity = textDifferenceAnalyzer.calculateSimilarityPercentage(text1, text2);

        // Assert
        assertNotNull(similarity);
        // Should be high similarity but not 100%
        assertTrue(similarity.compareTo(BigDecimal.valueOf(50)) > 0);
        assertTrue(similarity.compareTo(BigDecimal.valueOf(100)) < 0);
    }

    @Test
    void calculateSimilarityPercentage_ShouldHandleNullInputs() {
        // Act & Assert
        assertDoesNotThrow(() -> {
            BigDecimal similarity1 = textDifferenceAnalyzer.calculateSimilarityPercentage(null, "test");
            assertEquals(BigDecimal.ZERO, similarity1);

            BigDecimal similarity2 = textDifferenceAnalyzer.calculateSimilarityPercentage("test", null);
            assertEquals(BigDecimal.ZERO, similarity2);

            BigDecimal similarity3 = textDifferenceAnalyzer.calculateSimilarityPercentage(null, null);
            assertEquals(BigDecimal.valueOf(100), similarity3);
        });
    }

    @Test
    void analyzeTextDifferences_ShouldHandleComplexTextChanges() {
        // Arrange
        String originalText = "The quick brown fox jumps over the lazy dog. This is a simple sentence.";
        String newText = "The quick red fox leaps over the sleeping dog. This is a complex sentence with more details.";

        // Act
        List<TextDifferenceDto> differences = textDifferenceAnalyzer.analyzeTextDifferences(originalText, newText);
        ComparisonSummaryDto summary = textDifferenceAnalyzer.createComparisonSummary(originalText, newText,
                differences);

        // Assert
        assertNotNull(differences);
        assertNotNull(summary);

        // Should detect multiple changes
        assertTrue(summary.getTotalChanges() > 0);

        // Should have reasonable similarity (not too low, not 100%)
        assertTrue(summary.getSimilarityPercentage().compareTo(BigDecimal.valueOf(30)) > 0);
        assertTrue(summary.getSimilarityPercentage().compareTo(BigDecimal.valueOf(95)) < 0);
    }
}