package ai.content.auto.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoAnalyticsDto {
    private Long id;
    private Long publicationId;
    private Long views;
    private Long likes;
    private Long dislikes;
    private Long comments;
    private Long shares;
    private Long watchTimeSeconds;
    private Double averageViewDurationSeconds;
    private Double engagementRate;
    private Double clickThroughRate;
    private Double conversionRate;
    private Double revenue;
    private Long impressions;
    private Long reach;
    private Instant snapshotAt;
    private Instant createdAt;
}
