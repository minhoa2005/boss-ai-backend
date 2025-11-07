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
public class VideoSEOMetadataDto {
    private Long id;
    private Long videoJobId;
    private String optimizedTitle;
    private String optimizedDescription;
    private String keywords;
    private String hashtags;
    private String targetAudience;
    private String contentCategory;
    private String language;
    private String transcript;
    private String closedCaptionsUrl;
    private String thumbnailAltText;
    private String schemaMarkup;
    private String canonicalUrl;
    private Double seoScore;
    private Double readabilityScore;
    private Double keywordDensity;
    private Instant createdAt;
}
