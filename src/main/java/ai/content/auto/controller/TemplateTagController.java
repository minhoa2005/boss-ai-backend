package ai.content.auto.controller;

import ai.content.auto.dtos.*;
import ai.content.auto.service.TemplateCategorizationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/template-tags")
@RequiredArgsConstructor
@Slf4j
public class TemplateTagController {

    private final TemplateCategorizationService categorizationService;

    /**
     * Get all active tags
     */
    @GetMapping
    public ResponseEntity<BaseResponse<List<TemplateTagDto>>> getAllTags() {
        log.info("Getting all active tags");

        List<TemplateTagDto> tags = categorizationService.getAllTags();

        BaseResponse<List<TemplateTagDto>> response = new BaseResponse<List<TemplateTagDto>>()
                .setErrorMessage("Tags retrieved successfully")
                .setData(tags);

        return ResponseEntity.ok(response);
    }

    /**
     * Get popular tags
     */
    @GetMapping("/popular")
    public ResponseEntity<BaseResponse<List<TemplateTagDto>>> getPopularTags() {
        log.info("Getting popular tags");

        List<TemplateTagDto> tags = categorizationService.getPopularTags();

        BaseResponse<List<TemplateTagDto>> response = new BaseResponse<List<TemplateTagDto>>()
                .setErrorMessage("Popular tags retrieved successfully")
                .setData(tags);

        return ResponseEntity.ok(response);
    }

    /**
     * Search tags
     */
    @GetMapping("/search")
    public ResponseEntity<BaseResponse<List<TemplateTagDto>>> searchTags(
            @RequestParam String q) {

        log.info("Searching tags with term: {}", q);

        List<TemplateTagDto> tags = categorizationService.searchTags(q);

        BaseResponse<List<TemplateTagDto>> response = new BaseResponse<List<TemplateTagDto>>()
                .setErrorMessage("Tags search completed successfully")
                .setData(tags);

        return ResponseEntity.ok(response);
    }

    /**
     * Create a new tag
     */
    @PostMapping
    public ResponseEntity<BaseResponse<TemplateTagDto>> createTag(
            @Valid @RequestBody CreateTemplateTagRequest request) {

        log.info("Creating tag: {}", request.getName());

        TemplateTagDto tag = categorizationService.createTag(request);

        BaseResponse<TemplateTagDto> response = new BaseResponse<TemplateTagDto>()
                .setErrorMessage("Tag created successfully")
                .setData(tag);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Update an existing tag
     */
    @PutMapping("/{tagId}")
    public ResponseEntity<BaseResponse<TemplateTagDto>> updateTag(
            @PathVariable Long tagId,
            @Valid @RequestBody CreateTemplateTagRequest request) {

        log.info("Updating tag: {}", tagId);

        TemplateTagDto tag = categorizationService.updateTag(tagId, request);

        BaseResponse<TemplateTagDto> response = new BaseResponse<TemplateTagDto>()
                .setErrorMessage("Tag updated successfully")
                .setData(tag);

        return ResponseEntity.ok(response);
    }

    /**
     * Delete a tag
     */
    @DeleteMapping("/{tagId}")
    public ResponseEntity<BaseResponse<Void>> deleteTag(@PathVariable Long tagId) {
        log.info("Deleting tag: {}", tagId);

        categorizationService.deleteTag(tagId);

        BaseResponse<Void> response = new BaseResponse<Void>()
                .setErrorMessage("Tag deleted successfully");

        return ResponseEntity.ok(response);
    }

    /**
     * Get or create tags by names (for bulk operations)
     */
    @PostMapping("/bulk")
    public ResponseEntity<BaseResponse<List<TemplateTagDto>>> getOrCreateTags(
            @RequestBody List<String> tagNames) {

        log.info("Getting or creating tags: {}", tagNames);

        List<TemplateTagDto> tags = categorizationService.getOrCreateTags(tagNames);

        BaseResponse<List<TemplateTagDto>> response = new BaseResponse<List<TemplateTagDto>>()
                .setErrorMessage("Tags processed successfully")
                .setData(tags);

        return ResponseEntity.ok(response);
    }
}