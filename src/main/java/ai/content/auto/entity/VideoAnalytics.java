package ai.content.auto.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Entity for tracking video performance analytics
 */
@Entity
@Table(name = "video_analytics")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoAnalytics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "publication_id", nullable = false)
    private VideoPublication publication;

    @Column(name = "views")
    @Builder.Default
    private Long views = 0L;

    @Column(name = "likes")
    @Builder.Default
    private Long likes = 0L;

    @Column(name = "dislikes")
    @Builder.Default
    private Long dislikes = 0L;

    @Column(name = "comments")
    @Builder.Default
    private Long comments = 0L;

    @Column(name = "shares")
    @Builder.Default
    private Long shares = 0L;

    @Column(name = "watch_time_seconds")
    @Builder.Default
    private Long watchTimeSeconds = 0L;

    @Column(name = "average_view_duration_seconds")
    private Double averageViewDurationSeconds;

    @Column(name = "engagement_rate")
    private Double engagementRate;

    @Column(name = "click_through_rate")
    private Double clickThroughRate;

    @Column(name = "conversion_rate")
    private Double conversionRate;

    @Column(name = "revenue")
    private Double revenue;

    @Column(name = "impressions")
    @Builder.Default
    private Long impressions = 0L;

    @Column(name = "reach")
    @Builder.Default
    private Long reach = 0L;

    @Column(name = "snapshot_at", nullable = false)
    @Builder.Default
    private Instant snapshotAt = Instant.now();

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

    /**
     * Calculate engagement rate based on interactions
     */
    public void calculateEngagementRate() {
        if (views > 0) {
            long totalEngagements = likes + comments + shares;
            this.engagementRate = (double) totalEngagements / views * 100;
        }
    }
}
