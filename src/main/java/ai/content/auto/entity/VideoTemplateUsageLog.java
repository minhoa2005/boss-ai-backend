package ai.content.auto.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Entity for tracking video template usage analytics.
 * Records when and how templates are used for video generation.
 */
@Entity
@Table(name = "video_template_usage_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoTemplateUsageLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private VideoTemplate template;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "content_id")
    private ContentGeneration content;

    @Column(name = "video_duration")
    private Integer videoDuration; // actual duration in seconds

    @Column(name = "generation_status", length = 50)
    private String generationStatus; // SUCCESS, FAILED, CANCELLED

    @Column(name = "processing_time_ms")
    private Long processingTimeMs;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "used_at", nullable = false)
    private Instant usedAt;

    @PrePersist
    protected void onCreate() {
        if (usedAt == null) {
            usedAt = Instant.now();
        }
    }
}
