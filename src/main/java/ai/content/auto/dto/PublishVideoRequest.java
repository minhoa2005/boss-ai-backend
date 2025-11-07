package ai.content.auto.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublishVideoRequest {

    @NotNull(message = "Video job ID is required")
    private Long videoJobId;

    @NotNull(message = "At least one platform is required")
    private List<String> platforms; // YOUTUBE, FACEBOOK, INSTAGRAM, TIKTOK, LINKEDIN, TWITTER

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    private List<String> tags;

    private String visibility; // PUBLIC, PRIVATE, UNLISTED

    private String category;

    private Instant scheduledAt; // Optional: for scheduled publishing

    private String thumbnailUrl;
}
