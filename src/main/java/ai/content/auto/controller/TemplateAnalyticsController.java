package ai.content.auto.controller;

import ai.content.auto.dtos.*;
import ai.content.auto.service.TemplateABTestingService;
import ai.content.auto.service.TemplateAnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * Controller for template analytics and optimization
 */
@RestController
@RequestMapping("/api/v1/templates/analytics")
@RequiredArgsConstructor
@Slf4j
public class TemplateAnalyticsController {

    private final TemplateAnalyticsService analyticsService;
    private final TemplateABTestingService abTestingService;

    /**
     * Get comprehensive analytics for a template
     */
    @GetMapping("/{templateId}")
    public ResponseEntity<BaseResponse<TemplateAnalyticsReport>> getTemplateAnalytics(
            @PathVariable Long templateId) {

        log.info("Getting analytics for template: {}", templateId);

        TemplateAnalyticsReport report = analyticsService.getTemplateAnalytics(templateId);

        BaseResponse<TemplateAnalyticsReport> response = new BaseResponse<TemplateAnalyticsReport>()
                .setErrorMessage("Template analytics retrieved successfully")
                .setData(report);

        return ResponseEntity.ok(response);
    }

    /**
     * Get comparative analytics for multiple templates
     */
    @PostMapping("/compare")
    public ResponseEntity<BaseResponse<List<TemplateAnalyticsReport>>> getComparativeAnalytics(
            @RequestBody List<Long> templateIds) {

        log.info("Getting comparative analytics for {} templates", templateIds.size());

        List<TemplateAnalyticsReport> reports = analyticsService.getComparativeAnalytics(templateIds);

        BaseResponse<List<TemplateAnalyticsReport>> response = new BaseResponse<List<TemplateAnalyticsReport>>()
                .setErrorMessage("Comparative analytics retrieved successfully")
                .setData(reports);

        return ResponseEntity.ok(response);
    }

    /**
     * Create a new A/B test
     */
    @PostMapping("/ab-tests")
    public ResponseEntity<BaseResponse<ABTestConfiguration>> createABTest(
            @Valid @RequestBody ABTestConfiguration config) {

        log.info("Creating A/B test: {}", config.getTestName());

        ABTestConfiguration createdTest = abTestingService.createABTest(config);

        BaseResponse<ABTestConfiguration> response = new BaseResponse<ABTestConfiguration>()
                .setErrorMessage("A/B test created successfully")
                .setData(createdTest);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get A/B test results
     */
    @GetMapping("/ab-tests/{testId}/results")
    public ResponseEntity<BaseResponse<ABTestResult>> getABTestResults(
            @PathVariable Long testId) {

        log.info("Getting results for A/B test: {}", testId);

        ABTestResult result = abTestingService.getABTestResults(testId);

        BaseResponse<ABTestResult> response = new BaseResponse<ABTestResult>()
                .setErrorMessage("A/B test results retrieved successfully")
                .setData(result);

        return ResponseEntity.ok(response);
    }

    /**
     * Stop an active A/B test
     */
    @PostMapping("/ab-tests/{testId}/stop")
    public ResponseEntity<BaseResponse<Void>> stopABTest(@PathVariable Long testId) {

        log.info("Stopping A/B test: {}", testId);

        abTestingService.stopABTest(testId);

        BaseResponse<Void> response = new BaseResponse<Void>()
                .setErrorMessage("A/B test stopped successfully");

        return ResponseEntity.ok(response);
    }

    /**
     * Get all active A/B tests
     */
    @GetMapping("/ab-tests/active")
    public ResponseEntity<BaseResponse<List<ABTestStatus>>> getActiveABTests() {

        log.info("Getting all active A/B tests");

        List<ABTestStatus> activeTests = abTestingService.getActiveABTests();

        BaseResponse<List<ABTestStatus>> response = new BaseResponse<List<ABTestStatus>>()
                .setErrorMessage("Active A/B tests retrieved successfully")
                .setData(activeTests);

        return ResponseEntity.ok(response);
    }

    /**
     * Get template variant for A/B test
     */
    @GetMapping("/ab-tests/{testId}/variant")
    public ResponseEntity<BaseResponse<Long>> getTemplateVariant(
            @PathVariable Long testId,
            @RequestParam Long userId) {

        log.info("Getting template variant for test: {} and user: {}", testId, userId);

        Long templateId = abTestingService.getTemplateVariantForTest(testId, userId);

        BaseResponse<Long> response = new BaseResponse<Long>()
                .setErrorMessage("Template variant retrieved successfully")
                .setData(templateId);

        return ResponseEntity.ok(response);
    }
}
