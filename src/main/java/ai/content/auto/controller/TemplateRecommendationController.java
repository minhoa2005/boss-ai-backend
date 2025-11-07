package ai.content.auto.controller;

import ai.content.auto.dtos.BaseResponse;
import ai.content.auto.dtos.TemplateRecommendation;
import ai.content.auto.service.TemplateRecommendationEngine;
import ai.content.auto.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Template Recommendation Controller
 * 
 * Provides endpoints for personalized template recommendations
 */
@RestController
@RequestMapping("/api/v1/templates/recommendations")
@RequiredArgsConstructor
@Slf4j
public class TemplateRecommendationController {

    private final TemplateRecommendationEngine recommendationEngine;
    private final SecurityUtil securityUtil;

    /**
     * Get personalized template recommendations for the current user
     * 
     * @param limit Maximum number of recommendations (default: 10)
     * @return List of recommended templates with scores and reasons
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<BaseResponse<List<TemplateRecommendation>>> getRecommendations(
            @RequestParam(defaultValue = "10") int limit) {

        Long userId = securityUtil.getCurrentUser().getId();
        log.info("Fetching {} template recommendations for user: {}", limit, userId);

        List<TemplateRecommendation> recommendations = recommendationEngine.getRecommendationsForUser(userId, limit);

        BaseResponse<List<TemplateRecommendation>> response = new BaseResponse<List<TemplateRecommendation>>()
                .setErrorCode("SUCCESS")
                .setErrorMessage("Template recommendations retrieved successfully")
                .setData(recommendations);

        return ResponseEntity.ok(response);
    }

    /**
     * Get personalized template recommendations for a specific user (admin only)
     * 
     * @param userId User ID
     * @param limit  Maximum number of recommendations (default: 10)
     * @return List of recommended templates with scores and reasons
     */
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BaseResponse<List<TemplateRecommendation>>> getRecommendationsForUser(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "10") int limit) {

        log.info("Admin fetching {} template recommendations for user: {}", limit, userId);

        List<TemplateRecommendation> recommendations = recommendationEngine.getRecommendationsForUser(userId, limit);

        BaseResponse<List<TemplateRecommendation>> response = new BaseResponse<List<TemplateRecommendation>>()
                .setErrorCode("SUCCESS")
                .setErrorMessage("Template recommendations retrieved successfully")
                .setData(recommendations);

        return ResponseEntity.ok(response);
    }
}
