package ai.content.auto.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Template Recommendation DTO
 * 
 * Represents a recommended template with its score and reason
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TemplateRecommendation {

    /**
     * Template ID
     */
    private Long templateId;

    /**
     * Recommendation score (0-100)
     */
    private Double score;

    /**
     * Reason for recommendation
     */
    private String reason;
}
