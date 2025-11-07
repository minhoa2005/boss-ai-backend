package ai.content.auto.controller;

import ai.content.auto.dtos.BaseResponse;
import ai.content.auto.dtos.TemplatePerformanceSummary;
import ai.content.auto.service.TemplatePopularityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for template popularity and performance metrics
 */
@RestController
@RequestMapping("/api/v1/templates/popularity")
@RequiredArgsConstructor
@Slf4j
public class TemplatePopularityController {

    private final TemplatePopularityService popularityService;

    /**
     * Get template performance summary
     */
    @GetMapping("/{templateId}/performance")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<BaseResponse<TemplatePerformanceSummary>> getTemplatePerformance(
            @PathVariable Long templateId) {

        log.info("Getting performance summary for template: {}", templateId);

        TemplatePerformanceSummary performance = popularityService.getTemplatePerformance(templateId);

        BaseResponse<TemplatePerformanceSummary> response = new BaseResponse<TemplatePerformanceSummary>()
                .setErrorMessage("Template performance retrieved successfully")
                .setData(performance);

        return ResponseEntity.ok(response);
    }

    /**
     * Get trending template IDs
     */
    @GetMapping("/trending")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<BaseResponse<List<Long>>> getTrendingTemplates(
            @RequestParam(defaultValue = "10") int limit) {

        log.info("Getting trending templates (limit: {})", limit);

        List<Long> trendingTemplateIds = popularityService.getTrendingTemplateIds(limit);

        BaseResponse<List<Long>> response = new BaseResponse<List<Long>>()
                .setErrorMessage("Trending templates retrieved successfully")
                .setData(trendingTemplateIds);

        return ResponseEntity.ok(response);
    }

    /**
     * Manually trigger metrics update for a specific template (admin only)
     */
    @PostMapping("/{templateId}/update-metrics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BaseResponse<Void>> updateTemplateMetrics(
            @PathVariable Long templateId) {

        log.info("Manually updating metrics for template: {}", templateId);

        popularityService.updateTemplateMetrics(templateId);

        BaseResponse<Void> response = new BaseResponse<Void>()
                .setErrorMessage("Template metrics updated successfully");

        return ResponseEntity.ok(response);
    }

    /**
     * Manually trigger metrics update for all templates (admin only)
     */
    @PostMapping("/update-all-metrics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BaseResponse<Void>> updateAllTemplateMetrics() {

        log.info("Manually updating metrics for all templates");

        popularityService.updateAllTemplateMetrics();

        BaseResponse<Void> response = new BaseResponse<Void>()
                .setErrorMessage("All template metrics updated successfully");

        return ResponseEntity.ok(response);
    }
}
