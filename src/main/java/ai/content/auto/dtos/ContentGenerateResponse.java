package ai.content.auto.dtos;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Map;

@Data
public class ContentGenerateResponse {
    private String generatedContent;
    private String title;
    private Integer wordCount;
    private Integer characterCount;
    private Integer tokensUsed;
    private BigDecimal generationCost;
    private Long processingTimeMs;
    private String status;
    private String errorMessage;

    // Generation parameters and metadata
    private Map<String, Object> generationParams;
    private String aiProvider;
    private String aiModel;

    // Quality metrics
    private BigDecimal readabilityScore;
    private BigDecimal seoScore;
    private BigDecimal qualityScore;
    private BigDecimal sentimentScore;

    // Content metadata
    private String industry;
    private String targetAudience;
    private String tone;
    private String language;
}