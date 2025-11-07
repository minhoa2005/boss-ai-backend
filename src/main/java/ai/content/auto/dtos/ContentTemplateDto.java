package ai.content.auto.dtos;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Data
public class ContentTemplateDto {
    private Long id;
    private String name;
    private String description;
    private String category;
    private Long categoryId;
    private String categoryName;
    private String categorySlug;
    private String promptTemplate;
    private Map<String, Object> defaultParams;
    private String industry;
    private String contentType;
    private String targetAudience;
    private String tone;
    private String language;
    private Integer usageCount;
    private BigDecimal averageRating;
    private BigDecimal successRate;
    private Integer totalRatings;
    private String status;
    private String visibility;
    private Boolean isSystemTemplate;
    private Boolean isFeatured;
    private List<String> tags;
    private List<String> requiredFields;
    private List<String> optionalFields;
    private Map<String, Object> validationRules;
    private Long averageGenerationTimeMs;
    private Integer averageTokensUsed;
    private BigDecimal averageCost;
    private Long createdById;
    private String createdByUsername;
    private Long updatedById;
    private String updatedByUsername;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    private OffsetDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    private OffsetDateTime updatedAt;

    private Long version;
}