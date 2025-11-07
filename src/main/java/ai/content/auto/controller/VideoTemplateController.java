package ai.content.auto.controller;

import ai.content.auto.dto.request.CreateVideoTemplateRequest;
import ai.content.auto.dto.response.VideoTemplateResponse;
import ai.content.auto.dtos.BaseResponse;
import ai.content.auto.service.VideoTemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for video template management.
 * Provides endpoints for CRUD operations, template discovery, and
 * recommendations.
 */
@RestController
@RequestMapping("/api/v1/video-templates")
@RequiredArgsConstructor
@Slf4j
public class VideoTemplateController {

        private final VideoTemplateService videoTemplateService;

        /**
         * Create a new video template.
         */
        @PostMapping
        public ResponseEntity<BaseResponse<VideoTemplateResponse>> createTemplate(
                        @Valid @RequestBody CreateVideoTemplateRequest request) {

                log.info("Creating video template: {}", request.getName());

                VideoTemplateResponse template = videoTemplateService.createTemplate(request);

                BaseResponse<VideoTemplateResponse> response = new BaseResponse<VideoTemplateResponse>()
                                .setErrorMessage("Video template created successfully")
                                .setData(template);

                return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }

        /**
         * Get template by ID.
         */
        @GetMapping("/{id}")
        public ResponseEntity<BaseResponse<VideoTemplateResponse>> getTemplate(@PathVariable Long id) {

                log.info("Retrieving video template: {}", id);

                VideoTemplateResponse template = videoTemplateService.getTemplateById(id);

                BaseResponse<VideoTemplateResponse> response = new BaseResponse<VideoTemplateResponse>()
                                .setErrorMessage("Video template retrieved successfully")
                                .setData(template);

                return ResponseEntity.ok(response);
        }

        /**
         * Get all public templates.
         */
        @GetMapping("/public")
        public ResponseEntity<BaseResponse<List<VideoTemplateResponse>>> getPublicTemplates() {

                log.info("Retrieving public video templates");

                List<VideoTemplateResponse> templates = videoTemplateService.getPublicTemplates();

                BaseResponse<List<VideoTemplateResponse>> response = new BaseResponse<List<VideoTemplateResponse>>()
                                .setErrorMessage("Public templates retrieved successfully")
                                .setData(templates);

                return ResponseEntity.ok(response);
        }

        /**
         * Get templates by category.
         */
        @GetMapping("/category/{category}")
        public ResponseEntity<BaseResponse<List<VideoTemplateResponse>>> getTemplatesByCategory(
                        @PathVariable String category) {

                log.info("Retrieving video templates for category: {}", category);

                List<VideoTemplateResponse> templates = videoTemplateService.getTemplatesByCategory(category);

                BaseResponse<List<VideoTemplateResponse>> response = new BaseResponse<List<VideoTemplateResponse>>()
                                .setErrorMessage("Templates retrieved successfully")
                                .setData(templates);

                return ResponseEntity.ok(response);
        }

        /**
         * Get user's templates.
         */
        @GetMapping("/my-templates")
        public ResponseEntity<BaseResponse<List<VideoTemplateResponse>>> getUserTemplates() {

                log.info("Retrieving user's video templates");

                List<VideoTemplateResponse> templates = videoTemplateService.getUserTemplates();

                BaseResponse<List<VideoTemplateResponse>> response = new BaseResponse<List<VideoTemplateResponse>>()
                                .setErrorMessage("User templates retrieved successfully")
                                .setData(templates);

                return ResponseEntity.ok(response);
        }

        /**
         * Get popular templates.
         */
        @GetMapping("/popular")
        public ResponseEntity<BaseResponse<List<VideoTemplateResponse>>> getPopularTemplates(
                        @RequestParam(defaultValue = "10") int limit) {

                log.info("Retrieving popular video templates, limit: {}", limit);

                List<VideoTemplateResponse> templates = videoTemplateService.getPopularTemplates(limit);

                BaseResponse<List<VideoTemplateResponse>> response = new BaseResponse<List<VideoTemplateResponse>>()
                                .setErrorMessage("Popular templates retrieved successfully")
                                .setData(templates);

                return ResponseEntity.ok(response);
        }

        /**
         * Get recommended templates for current user.
         */
        @GetMapping("/recommended")
        public ResponseEntity<BaseResponse<List<VideoTemplateResponse>>> getRecommendedTemplates(
                        @RequestParam(defaultValue = "10") int limit) {

                log.info("Retrieving recommended video templates, limit: {}", limit);

                List<VideoTemplateResponse> templates = videoTemplateService.getRecommendedTemplates(limit);

                BaseResponse<List<VideoTemplateResponse>> response = new BaseResponse<List<VideoTemplateResponse>>()
                                .setErrorMessage("Recommended templates retrieved successfully")
                                .setData(templates);

                return ResponseEntity.ok(response);
        }

        /**
         * Search templates.
         */
        @GetMapping("/search")
        public ResponseEntity<BaseResponse<List<VideoTemplateResponse>>> searchTemplates(
                        @RequestParam String query) {

                log.info("Searching video templates with query: {}", query);

                List<VideoTemplateResponse> templates = videoTemplateService.searchTemplates(query);

                BaseResponse<List<VideoTemplateResponse>> response = new BaseResponse<List<VideoTemplateResponse>>()
                                .setErrorMessage("Search completed successfully")
                                .setData(templates);

                return ResponseEntity.ok(response);
        }

        /**
         * Update template.
         */
        @PutMapping("/{id}")
        public ResponseEntity<BaseResponse<VideoTemplateResponse>> updateTemplate(
                        @PathVariable Long id,
                        @Valid @RequestBody CreateVideoTemplateRequest request) {

                log.info("Updating video template: {}", id);

                VideoTemplateResponse template = videoTemplateService.updateTemplate(id, request);

                BaseResponse<VideoTemplateResponse> response = new BaseResponse<VideoTemplateResponse>()
                                .setErrorMessage("Video template updated successfully")
                                .setData(template);

                return ResponseEntity.ok(response);
        }

        /**
         * Delete template.
         */
        @DeleteMapping("/{id}")
        public ResponseEntity<BaseResponse<Void>> deleteTemplate(@PathVariable Long id) {

                log.info("Deleting video template: {}", id);

                videoTemplateService.deleteTemplate(id);

                BaseResponse<Void> response = new BaseResponse<Void>()
                                .setErrorMessage("Video template deleted successfully");

                return ResponseEntity.ok(response);
        }
}
