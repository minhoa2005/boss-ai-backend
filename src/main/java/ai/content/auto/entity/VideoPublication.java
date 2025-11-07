package ai.content.auto.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Entity representing a video publication to social media platforms
 */
@Entity
@Table(name = "video_publications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoPublication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_job_id", nullable = false)
    private VideoGenerationJob videoJob;

    @Column(name = "platform", nullable = false, length = 50)
    private String platform; // YOUTUBE, FACEBOOK, INSTAGRAM, TIKTOK, LINKEDIN, TWITTER

    @Column(name = "platform_video_id", length = 255)
    private String platformVideoId;

    @Column(name = "publication_url", length = 1000)
    private String publicationUrl;

    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private String status = "PENDING"; // PENDING, SCHEDULED, PUBLISHING, PUBLISHED, FAILED

    @Column(name = "scheduled_at")
    private Instant scheduledAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "title", length = 500)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "tags", columnDefinition = "TEXT")
    private String tags; // Comma-separated tags

    @Column(name = "visibility", length = 50)
    @Builder.Default
    private String visibility = "PUBLIC"; // PUBLIC, PRIVATE, UNLISTED

    @Column(name = "category", length = 100)
    private String category;

    @Column(name = "thumbnail_url", length = 1000)
    private String thumbnailUrl;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "retry_count")
    @Builder.Default
    private Integer retryCount = 0;

    @Column(name = "max_retries")
    @Builder.Default
    private Integer maxRetries = 3;

    @Column(name = "created_by")
    private Long createdBy;

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
