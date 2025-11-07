package ai.content.auto.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * Comprehensive analytics report for a template
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateAnalyticsReport {

    private Long templateId;
    private String templateName;
    private TemplatePerformanceMetrics performanceMetrics;
    private Map<String, Integer> usageTrends;
    private List<TemplateOptimizationSuggestion> optimizationSuggestions;
    private Map<String, Object> satisfactionMetrics;
    private OffsetDateTime generatedAt;
}
