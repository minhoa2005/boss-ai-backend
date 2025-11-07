package ai.content.auto.controller;

import ai.content.auto.dtos.*;
import ai.content.auto.service.TemplateManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/templates")
@RequiredArgsConstructor
@Slf4j
public class TemplateController {

        private final TemplateManagementService templateManagementService;

        /**
         * Get templates by category and industry
         */
        @GetMapping
        public ResponseEntity<BaseResponse<List<ContentTemplateDto>>> getTemplates(
                        @RequestParam(required = false) String category,
                        @RequestParam(required = false) String industry) {

                log.info("Getting templates by category: {} and industry: {}", category, industry);

                List<ContentTemplateDto> templates = templateManagementService.getTemplatesByCategory(category,
                                industry);

                BaseResponse<List<ContentTemplateDto>> response = new BaseResponse<List<ContentTemplateDto>>()
                                .setErrorMessage("Templates retrieved successfully")
                                .setData(templates);

                return ResponseEntity.ok(response);
        }

        /**
         * Get recommended templates
         */
        @GetMapping("/recommended")
        public ResponseEntity<BaseResponse<List<ContentTemplateDto>>> getRecommendedTemplates() {

                log.info("Getting recommended templates");

                List<ContentTemplateDto> templates = templateManagementService.getRecommendedTemplates();

                BaseResponse<List<ContentTemplateDto>> response = new BaseResponse<List<ContentTemplateDto>>()
                                .setErrorMessage("Recommended templates retrieved successfully")
                                .setData(templates);

                return ResponseEntity.ok(response);
        }

        /**
         * Get featured templates
         */
        @GetMapping("/featured")
        public ResponseEntity<BaseResponse<List<ContentTemplateDto>>> getFeaturedTemplates() {

                log.info("Getting featured templates");

                List<ContentTemplateDto> templates = templateManagementService.getFeaturedTemplates();

                BaseResponse<List<ContentTemplateDto>> response = new BaseResponse<List<ContentTemplateDto>>()
                                .setErrorMessage("Featured templates retrieved successfully")
                                .setData(templates);

                return ResponseEntity.ok(response);
        }

        /**
         * Get popular templates with pagination
         */
        @GetMapping("/popular")
        public ResponseEntity<BaseResponse<Page<ContentTemplateDto>>> getPopularTemplates(
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size) {

                log.info("Getting popular templates (page: {}, size: {})", page, size);

                Page<ContentTemplateDto> templates = templateManagementService.getPopularTemplates(page, size);

                BaseResponse<Page<ContentTemplateDto>> response = new BaseResponse<Page<ContentTemplateDto>>()
                                .setErrorMessage("Popular templates retrieved successfully")
                                .setData(templates);

                return ResponseEntity.ok(response);
        }

        /**
         * Get template by ID
         */
        @GetMapping("/{id}")
        public ResponseEntity<BaseResponse<ContentTemplateDto>> getTemplateById(@PathVariable Long id) {

                log.info("Getting template by ID: {}", id);

                ContentTemplateDto template = templateManagementService.getTemplateById(id);

                BaseResponse<ContentTemplateDto> response = new BaseResponse<ContentTemplateDto>()
                                .setErrorMessage("Template retrieved successfully")
                                .setData(template);

                return ResponseEntity.ok(response);
        }

        /**
         * Create a new template
         */
        @PostMapping
        public ResponseEntity<BaseResponse<ContentTemplateDto>> createTemplate(
                        @Valid @RequestBody CreateTemplateRequest request) {

                log.info("Creating template: {}", request.getName());

                ContentTemplateDto template = templateManagementService.createTemplate(request);

                BaseResponse<ContentTemplateDto> response = new BaseResponse<ContentTemplateDto>()
                                .setErrorMessage("Template created successfully")
                                .setData(template);

                return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }

        /**
         * Update an existing template
         */
        @PutMapping("/{id}")
        public ResponseEntity<BaseResponse<ContentTemplateDto>> updateTemplate(
                        @PathVariable Long id,
                        @Valid @RequestBody UpdateTemplateRequest request) {

                log.info("Updating template: {}", id);

                ContentTemplateDto template = templateManagementService.updateTemplate(id, request);

                BaseResponse<ContentTemplateDto> response = new BaseResponse<ContentTemplateDto>()
                                .setErrorMessage("Template updated successfully")
                                .setData(template);

                return ResponseEntity.ok(response);
        }

        /**
         * Delete a template
         */
        @DeleteMapping("/{id}")
        public ResponseEntity<BaseResponse<Void>> deleteTemplate(@PathVariable Long id) {

                log.info("Deleting template: {}", id);

                templateManagementService.deleteTemplate(id);

                BaseResponse<Void> response = new BaseResponse<Void>()
                                .setErrorMessage("Template deleted successfully");

                return ResponseEntity.ok(response);
        }

        /**
         * Apply template to generate content request
         */
        @PostMapping("/{id}/apply")
        public ResponseEntity<BaseResponse<ContentGenerateRequest>> applyTemplate(
                        @PathVariable Long id,
                        @RequestBody(required = false) Map<String, Object> customParams) {

                log.info("Applying template: {} with custom params", id);

                ContentGenerateRequest request = templateManagementService.applyTemplate(id, customParams);

                BaseResponse<ContentGenerateRequest> response = new BaseResponse<ContentGenerateRequest>()
                                .setErrorMessage("Template applied successfully")
                                .setData(request);

                return ResponseEntity.ok(response);
        }

        /**
         * Get templates by tag
         */
        @GetMapping("/tag/{tag}")
        public ResponseEntity<BaseResponse<List<ContentTemplateDto>>> getTemplatesByTag(
                        @PathVariable String tag) {

                log.info("Getting templates by tag: {}", tag);

                List<ContentTemplateDto> templates = templateManagementService.getTemplatesByTag(tag);

                BaseResponse<List<ContentTemplateDto>> response = new BaseResponse<List<ContentTemplateDto>>()
                                .setErrorMessage("Templates by tag retrieved successfully")
                                .setData(templates);

                return ResponseEntity.ok(response);
        }
}