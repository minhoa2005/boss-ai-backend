package ai.content.auto.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Optimization suggestion for improving template performance
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateOptimizationSuggestion {

    private String type; // SUCCESS_RATE, USER_SATISFACTION, PERFORMANCE, RELIABILITY, etc.
    private String priority; // HIGH, MEDIUM, LOW
    private String title;
    private String description;
    private String impact; // HIGH, MEDIUM, LOW, POSITIVE, INFORMATIONAL
    private String recommendation;
}
