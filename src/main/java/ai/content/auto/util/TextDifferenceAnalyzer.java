package ai.content.auto.util;

import ai.content.auto.dtos.ContentVersionComparisonDto.TextDifferenceDto;
import ai.content.auto.dtos.ContentVersionComparisonDto.ComparisonSummaryDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Utility class for analyzing text differences between content versions.
 * Implements algorithms for text comparison and similarity analysis.
 * 
 * Requirements: 1.3, 1.4
 */
@Component
@Slf4j
public class TextDifferenceAnalyzer {

    private static final Pattern WORD_PATTERN = Pattern.compile("\\b\\w+\\b");
    private static final Pattern SENTENCE_PATTERN = Pattern.compile("[.!?]+");
    private static final Pattern PARAGRAPH_PATTERN = Pattern.compile("\n\n+");

    /**
     * Analyze differences between two text contents.
     * 
     * @param originalText The original text (version A)
     * @param newText      The new text (version B)
     * @return List of text differences
     */
    public List<TextDifferenceDto> analyzeTextDifferences(String originalText, String newText) {
        if (originalText == null)
            originalText = "";
        if (newText == null)
            newText = "";

        List<TextDifferenceDto> differences = new ArrayList<>();

        try {
            // Split texts into words for detailed comparison
            List<String> originalWords = extractWords(originalText);
            List<String> newWords = extractWords(newText);

            // Use Myers' diff algorithm for word-level comparison
            List<DiffOperation> operations = computeDiff(originalWords, newWords);

            // Convert diff operations to TextDifferenceDto objects
            differences = convertOperationsToTextDifferences(operations, originalWords, newWords);

            log.debug("Analyzed text differences: {} changes found", differences.size());

        } catch (Exception e) {
            log.error("Error analyzing text differences", e);
            // Return basic comparison if detailed analysis fails
            differences = createBasicComparison(originalText, newText);
        }

        return differences;
    }

    /**
     * Create a comparison summary from text differences.
     * 
     * @param originalText The original text
     * @param newText      The new text
     * @param differences  List of text differences
     * @return Comparison summary
     */
    public ComparisonSummaryDto createComparisonSummary(String originalText, String newText,
            List<TextDifferenceDto> differences) {
        if (originalText == null)
            originalText = "";
        if (newText == null)
            newText = "";

        int additions = 0;
        int deletions = 0;
        int modifications = 0;

        for (TextDifferenceDto diff : differences) {
            switch (diff.getType()) {
                case "ADDITION" -> additions++;
                case "DELETION" -> deletions++;
                case "MODIFICATION" -> modifications++;
            }
        }

        int totalChanges = additions + deletions + modifications;
        BigDecimal similarityPercentage = calculateSimilarityPercentage(originalText, newText);

        int originalWordCount = countWords(originalText);
        int newWordCount = countWords(newText);
        int wordCountDifference = newWordCount - originalWordCount;

        int characterCountDifference = newText.length() - originalText.length();

        return ComparisonSummaryDto.builder()
                .totalChanges(totalChanges)
                .additions(additions)
                .deletions(deletions)
                .modifications(modifications)
                .similarityPercentage(similarityPercentage)
                .wordCountDifference(wordCountDifference)
                .characterCountDifference(characterCountDifference)
                .build();
    }

    /**
     * Calculate similarity percentage between two texts using Levenshtein distance.
     * 
     * @param text1 First text
     * @param text2 Second text
     * @return Similarity percentage (0-100)
     */
    public BigDecimal calculateSimilarityPercentage(String text1, String text2) {
        if (text1 == null)
            text1 = "";
        if (text2 == null)
            text2 = "";

        if (text1.equals(text2)) {
            return BigDecimal.valueOf(100);
        }

        if (text1.isEmpty() && text2.isEmpty()) {
            return BigDecimal.valueOf(100);
        }

        if (text1.isEmpty() || text2.isEmpty()) {
            return BigDecimal.ZERO;
        }

        int maxLength = Math.max(text1.length(), text2.length());
        int distance = calculateLevenshteinDistance(text1, text2);

        double similarity = ((double) (maxLength - distance) / maxLength) * 100;
        return BigDecimal.valueOf(similarity).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Extract words from text using regex pattern.
     * 
     * @param text Input text
     * @return List of words
     */
    private List<String> extractWords(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new ArrayList<>();
        }

        List<String> words = new ArrayList<>();
        var matcher = WORD_PATTERN.matcher(text.toLowerCase());

        while (matcher.find()) {
            words.add(matcher.group());
        }

        return words;
    }

    /**
     * Compute diff operations using a simplified Myers' algorithm.
     * 
     * @param original Original word list
     * @param revised  Revised word list
     * @return List of diff operations
     */
    private List<DiffOperation> computeDiff(List<String> original, List<String> revised) {
        List<DiffOperation> operations = new ArrayList<>();

        int i = 0, j = 0;

        while (i < original.size() || j < revised.size()) {
            if (i >= original.size()) {
                // Remaining items in revised are additions
                operations.add(new DiffOperation(DiffType.ADDITION, null, revised.get(j), j));
                j++;
            } else if (j >= revised.size()) {
                // Remaining items in original are deletions
                operations.add(new DiffOperation(DiffType.DELETION, original.get(i), null, i));
                i++;
            } else if (original.get(i).equals(revised.get(j))) {
                // Items are equal - no change
                operations.add(new DiffOperation(DiffType.UNCHANGED, original.get(i), revised.get(j), i));
                i++;
                j++;
            } else {
                // Items are different - check if it's a modification or separate add/delete
                if (findInRange(revised, original.get(i), j, Math.min(j + 3, revised.size())) != -1) {
                    // Original word found later in revised - this is a deletion
                    operations.add(new DiffOperation(DiffType.DELETION, original.get(i), null, i));
                    i++;
                } else if (findInRange(original, revised.get(j), i, Math.min(i + 3, original.size())) != -1) {
                    // Revised word found later in original - this is an addition
                    operations.add(new DiffOperation(DiffType.ADDITION, null, revised.get(j), j));
                    j++;
                } else {
                    // Words are different and not found nearby - modification
                    operations.add(new DiffOperation(DiffType.MODIFICATION, original.get(i), revised.get(j), i));
                    i++;
                    j++;
                }
            }
        }

        return operations;
    }

    /**
     * Find a word in a range of a list.
     * 
     * @param list  List to search in
     * @param word  Word to find
     * @param start Start index
     * @param end   End index
     * @return Index of word or -1 if not found
     */
    private int findInRange(List<String> list, String word, int start, int end) {
        for (int i = start; i < end && i < list.size(); i++) {
            if (list.get(i).equals(word)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Convert diff operations to TextDifferenceDto objects.
     * 
     * @param operations    List of diff operations
     * @param originalWords Original words
     * @param newWords      New words
     * @return List of TextDifferenceDto
     */
    private List<TextDifferenceDto> convertOperationsToTextDifferences(List<DiffOperation> operations,
            List<String> originalWords,
            List<String> newWords) {
        List<TextDifferenceDto> differences = new ArrayList<>();

        for (DiffOperation op : operations) {
            if (op.getType() != DiffType.UNCHANGED) {
                TextDifferenceDto diff = TextDifferenceDto.builder()
                        .type(op.getType().name())
                        .originalText(op.getOriginalText())
                        .newText(op.getNewText())
                        .startPosition(op.getPosition())
                        .endPosition(
                                op.getPosition() + (op.getOriginalText() != null ? op.getOriginalText().length() : 0))
                        .lineNumber(calculateLineNumber(op.getPosition(), originalWords))
                        .build();
                differences.add(diff);
            }
        }

        return differences;
    }

    /**
     * Create basic comparison when detailed analysis fails.
     * 
     * @param originalText Original text
     * @param newText      New text
     * @return Basic comparison
     */
    private List<TextDifferenceDto> createBasicComparison(String originalText, String newText) {
        List<TextDifferenceDto> differences = new ArrayList<>();

        if (!originalText.equals(newText)) {
            differences.add(TextDifferenceDto.builder()
                    .type("MODIFICATION")
                    .originalText(originalText)
                    .newText(newText)
                    .startPosition(0)
                    .endPosition(Math.max(originalText.length(), newText.length()))
                    .lineNumber(1)
                    .build());
        }

        return differences;
    }

    /**
     * Calculate line number for a given position.
     * 
     * @param position Position in text
     * @param words    List of words
     * @return Line number (1-based)
     */
    private Integer calculateLineNumber(int position, List<String> words) {
        // Simplified line number calculation
        // In a real implementation, this would track actual line breaks
        return (position / 10) + 1;
    }

    /**
     * Calculate Levenshtein distance between two strings.
     * 
     * @param s1 First string
     * @param s2 Second string
     * @return Levenshtein distance
     */
    private int calculateLevenshteinDistance(String s1, String s2) {
        int len1 = s1.length();
        int len2 = s2.length();

        int[][] dp = new int[len1 + 1][len2 + 1];

        for (int i = 0; i <= len1; i++) {
            dp[i][0] = i;
        }

        for (int j = 0; j <= len2; j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= len1; i++) {
            for (int j = 1; j <= len2; j++) {
                if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = 1 + Math.min(Math.min(dp[i - 1][j], dp[i][j - 1]), dp[i - 1][j - 1]);
                }
            }
        }

        return dp[len1][len2];
    }

    /**
     * Count words in text.
     * 
     * @param text Input text
     * @return Word count
     */
    private int countWords(String text) {
        if (text == null || text.trim().isEmpty()) {
            return 0;
        }
        return text.trim().split("\\s+").length;
    }

    /**
     * Enum for diff operation types.
     */
    private enum DiffType {
        ADDITION, DELETION, MODIFICATION, UNCHANGED
    }

    /**
     * Class representing a diff operation.
     */
    private static class DiffOperation {
        private final DiffType type;
        private final String originalText;
        private final String newText;
        private final int position;

        public DiffOperation(DiffType type, String originalText, String newText, int position) {
            this.type = type;
            this.originalText = originalText;
            this.newText = newText;
            this.position = position;
        }

        public DiffType getType() {
            return type;
        }

        public String getOriginalText() {
            return originalText;
        }

        public String getNewText() {
            return newText;
        }

        public int getPosition() {
            return position;
        }
    }
}