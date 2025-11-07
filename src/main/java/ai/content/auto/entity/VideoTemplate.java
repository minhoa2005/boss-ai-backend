package ai.content.auto.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * Entity representing a video template with customizable styles and branding
 * options.
 * Supports template-based video generation with predefined configurations.
 */
@Entity
@Table(name = "video_templates")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 100)
    private String category;

    // Template Style Configuration
    @Column(name = "style_name", length = 100)
    private String styleName; // professional, casual, creative, corporate, etc.

    @Column(name = "animation_style", length = 100)
    private String animationStyle; // smooth, dynamic, minimal, energetic

    @Column(name = "transition_style", length = 100)
    private String transitionStyle; // fade, slide, zoom, cut

    // Branding Options
    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(name = "logo_position", length = 50)
    private String logoPosition; // top-left, top-right, bottom-left, bottom-right, center

    @Column(name = "primary_color", length = 7)
    private String primaryColor; // Hex color code

    @Column(name = "secondary_color", length = 7)
    private String secondaryColor; // Hex color code

    @Column(name = "accent_color", length = 7)
    private String accentColor; // Hex color code

    @Column(name = "font_family", length = 100)
    private String fontFamily;

    @Column(name = "font_size", length = 20)
    private String fontSize; // small, medium, large, or specific px value

    // Duration and Format Options
    @Column(name = "default_duration")
    private Integer defaultDuration; // in seconds

    @Column(name = "min_duration")
    private Integer minDuration; // in seconds

    @Column(name = "max_duration")
    private Integer maxDuration; // in seconds

    @Column(name = "aspect_ratio", length = 20)
    private String aspectRatio; // 16:9, 9:16, 1:1, 4:3

    @Column(name = "resolution", length = 20)
    private String resolution; // 1080p, 720p, 4K, etc.

    @Column(name = "frame_rate")
    private Integer frameRate; // 24, 30, 60 fps

    @Column(name = "video_format", length = 20)
    private String videoFormat; // mp4, webm, mov

    // Voice and Audio Options
    @Column(name = "voice_over_enabled")
    private Boolean voiceOverEnabled;

    @Column(name = "voice_type", length = 100)
    private String voiceType; // male, female, neutral

    @Column(name = "voice_speed", length = 20)
    private String voiceSpeed; // slow, normal, fast

    @Column(name = "background_music_enabled")
    private Boolean backgroundMusicEnabled;

    @Column(name = "music_genre", length = 100)
    private String musicGenre;

    @Column(name = "music_volume")
    private Integer musicVolume; // 0-100

    // Advanced Configuration (JSON)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "advanced_config", columnDefinition = "jsonb")
    private Map<String, Object> advancedConfig;

    // Template Metadata
    @Column(name = "is_public")
    private Boolean isPublic;

    @Column(name = "is_system_template")
    private Boolean isSystemTemplate;

    @Column(name = "usage_count")
    private Integer usageCount;

    @Column(name = "average_rating", precision = 3, scale = 2)
    private BigDecimal averageRating;

    @Column(name = "success_rate", precision = 5, scale = 2)
    private BigDecimal successRate;

    // Ownership
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    // Timestamps
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
        if (usageCount == null) {
            usageCount = 0;
        }
        if (averageRating == null) {
            averageRating = BigDecimal.ZERO;
        }
        if (successRate == null) {
            successRate = BigDecimal.ZERO;
        }
        if (isPublic == null) {
            isPublic = false;
        }
        if (isSystemTemplate == null) {
            isSystemTemplate = false;
        }
        if (voiceOverEnabled == null) {
            voiceOverEnabled = true;
        }
        if (backgroundMusicEnabled == null) {
            backgroundMusicEnabled = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
