package ai.content.auto.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Entity for video SEO optimization metadata
 */
@Entity
@Table(name = "video_seo_metadata")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoSEOMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_job_id", nullable = false, unique = true)
    private VideoGenerationJob videoJob;

    @Column(name = "optimized_title", length = 500)
    private String optimizedTitle;

    @Column(name = "optimized_description", columnDefinition = "TEXT")
    private String optimizedDescription;

    @Column(name = "keywords", columnDefinition = "TEXT")
    private String keywords; // Comma-separated keywords

    @Column(name = "hashtags", columnDefinition = "TEXT")
    private String hashtags; // Comma-separated hashtags

    @Column(name = "target_audience", length = 255)
    private String targetAudience;

    @Column(name = "content_category", length = 100)
    private String contentCategory;

    @Column(name = "language", length = 10)
    @Builder.Default
    private String language = "en";

    @Column(name = "transcript", columnDefinition = "TEXT")
    private String transcript;

    @Column(name = "closed_captions_url", length = 1000)
    private String closedCaptionsUrl;

    @Column(name = "thumbnail_alt_text", length = 500)
    private String thumbnailAltText;

    @Column(name = "schema_markup", columnDefinition = "TEXT")
    private String schemaMarkup; // JSON-LD schema markup

    @Column(name = "canonical_url", length = 1000)
    private String canonicalUrl;

    @Column(name = "seo_score")
    private Double seoScore;

    @Column(name = "readability_score")
    private Double readabilityScore;

    @Column(name = "keyword_density")
    private Double keywordDensity;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
