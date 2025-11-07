package ai.content.auto.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * Status information for an A/B test
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ABTestStatus {

    private Long testId;
    private String testName;
    private String status;
    private String variantATemplateName;
    private String variantBTemplateName;
    private OffsetDateTime startedAt;
}
