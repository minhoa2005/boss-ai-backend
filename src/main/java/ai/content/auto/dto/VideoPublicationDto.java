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
public class VideoPublicationDto {
    private Long id;
    private Long videoJobId;
    private String platform;
    private String platformVideoId;
    private String publicationUrl;
    private String status;
    private Instant scheduledAt;
    private Instant publishedAt;
    private String title;
    private String description;
    private String tags;
    private String visibility;
    private String category;
    private String thumbnailUrl;
    private String errorMessage;
    private Integer retryCount;
    private Integer maxRetries;
    private Instant createdAt;
    private Instant updatedAt;
}
