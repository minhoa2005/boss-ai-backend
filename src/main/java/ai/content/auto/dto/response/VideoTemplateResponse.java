package ai.content.auto.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * Response DTO for video template information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoTemplateResponse {

    private Long id;
    private String name;
    private String description;
    private String category;

    // Style Configuration
    private String styleName;
    private String animationStyle;
    private String transitionStyle;

    // Branding Options
    private String logoUrl;
    private String logoPosition;
    private String primaryColor;
    private String secondaryColor;
    private String accentColor;
    private String fontFamily;
    private String fontSize;

    // Duration and Format Options
    private Integer defaultDuration;
    private Integer minDuration;
    private Integer maxDuration;
    private String aspectRatio;
    private String resolution;
    private Integer frameRate;
    private String videoFormat;

    // Voice and Audio Options
    private Boolean voiceOverEnabled;
    private String voiceType;
    private String voiceSpeed;
    private Boolean backgroundMusicEnabled;
    private String musicGenre;
    private Integer musicVolume;

    // Advanced Configuration
    private Map<String, Object> advancedConfig;

    // Template Metadata
    private Boolean isPublic;
    private Boolean isSystemTemplate;
    private Integer usageCount;
    private BigDecimal averageRating;
    private BigDecimal successRate;

    // Ownership
    private Long createdById;
    private String createdByUsername;

    // Timestamps
    private Instant createdAt;
    private Instant updatedAt;
}
