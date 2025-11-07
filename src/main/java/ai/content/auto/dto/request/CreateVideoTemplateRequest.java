package ai.content.auto.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Request DTO for creating a new video template.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateVideoTemplateRequest {

    @NotBlank(message = "Template name is required")
    @Size(max = 200, message = "Template name must not exceed 200 characters")
    private String name;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    @Size(max = 100, message = "Category must not exceed 100 characters")
    private String category;

    // Style Configuration
    @Size(max = 100, message = "Style name must not exceed 100 characters")
    private String styleName;

    @Size(max = 100, message = "Animation style must not exceed 100 characters")
    private String animationStyle;

    @Size(max = 100, message = "Transition style must not exceed 100 characters")
    private String transitionStyle;

    // Branding Options
    @Size(max = 500, message = "Logo URL must not exceed 500 characters")
    private String logoUrl;

    @Size(max = 50, message = "Logo position must not exceed 50 characters")
    private String logoPosition;

    @Pattern(regexp = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$", message = "Primary color must be a valid hex color code")
    private String primaryColor;

    @Pattern(regexp = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$", message = "Secondary color must be a valid hex color code")
    private String secondaryColor;

    @Pattern(regexp = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$", message = "Accent color must be a valid hex color code")
    private String accentColor;

    @Size(max = 100, message = "Font family must not exceed 100 characters")
    private String fontFamily;

    @Size(max = 20, message = "Font size must not exceed 20 characters")
    private String fontSize;

    // Duration and Format Options
    @Min(value = 1, message = "Default duration must be at least 1 second")
    @Max(value = 3600, message = "Default duration must not exceed 3600 seconds")
    private Integer defaultDuration;

    @Min(value = 1, message = "Minimum duration must be at least 1 second")
    private Integer minDuration;

    @Max(value = 3600, message = "Maximum duration must not exceed 3600 seconds")
    private Integer maxDuration;

    @Size(max = 20, message = "Aspect ratio must not exceed 20 characters")
    private String aspectRatio;

    @Size(max = 20, message = "Resolution must not exceed 20 characters")
    private String resolution;

    @Min(value = 1, message = "Frame rate must be at least 1 fps")
    @Max(value = 120, message = "Frame rate must not exceed 120 fps")
    private Integer frameRate;

    @Size(max = 20, message = "Video format must not exceed 20 characters")
    private String videoFormat;

    // Voice and Audio Options
    private Boolean voiceOverEnabled;

    @Size(max = 100, message = "Voice type must not exceed 100 characters")
    private String voiceType;

    @Size(max = 20, message = "Voice speed must not exceed 20 characters")
    private String voiceSpeed;

    private Boolean backgroundMusicEnabled;

    @Size(max = 100, message = "Music genre must not exceed 100 characters")
    private String musicGenre;

    @Min(value = 0, message = "Music volume must be at least 0")
    @Max(value = 100, message = "Music volume must not exceed 100")
    private Integer musicVolume;

    // Advanced Configuration
    private Map<String, Object> advancedConfig;

    // Template Metadata
    private Boolean isPublic;
}
