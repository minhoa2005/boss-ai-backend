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
@RequestMapping("/api/v1/template-categories")
@RequiredArgsConstructor
@Slf4j
public class TemplateCategoryController {

    private final TemplateCategorizationService categorizationService;

    /**
     * Get all root categories
     */
    @GetMapping("/root")
    public ResponseEntity<BaseResponse<List<TemplateCategoryDto>>> getRootCategories() {
        log.info("Getting root categories");

        List<TemplateCategoryDto> categories = categorizationService.getRootCategories();

        BaseResponse<List<TemplateCategoryDto>> response = new BaseResponse<List<TemplateCategoryDto>>()
                .setErrorMessage("Root categories retrieved successfully")
                .setData(categories);

        return ResponseEntity.ok(response);
    }

    /**
     * Get categories by parent ID
     */
    @GetMapping("/parent/{parentId}")
    public ResponseEntity<BaseResponse<List<TemplateCategoryDto>>> getCategoriesByParent(
            @PathVariable Long parentId) {

        log.info("Getting categories by parent ID: {}", parentId);

        List<TemplateCategoryDto> categories = categorizationService.getCategoriesByParent(parentId);

        BaseResponse<List<TemplateCategoryDto>> response = new BaseResponse<List<TemplateCategoryDto>>()
                .setErrorMessage("Categories retrieved successfully")
                .setData(categories);

        return ResponseEntity.ok(response);
    }

    /**
     * Get category hierarchy
     */
    @GetMapping("/{categoryId}/hierarchy")
    public ResponseEntity<BaseResponse<List<TemplateCategoryDto>>> getCategoryHierarchy(
            @PathVariable Long categoryId) {

        log.info("Getting category hierarchy for: {}", categoryId);

        List<TemplateCategoryDto> categories = categorizationService.getCategoryHierarchy(categoryId);

        BaseResponse<List<TemplateCategoryDto>> response = new BaseResponse<List<TemplateCategoryDto>>()
                .setErrorMessage("Category hierarchy retrieved successfully")
                .setData(categories);

        return ResponseEntity.ok(response);
    }

    /**
     * Get popular categories
     */
    @GetMapping("/popular")
    public ResponseEntity<BaseResponse<List<TemplateCategoryDto>>> getPopularCategories() {
        log.info("Getting popular categories");

        List<TemplateCategoryDto> categories = categorizationService.getPopularCategories();

        BaseResponse<List<TemplateCategoryDto>> response = new BaseResponse<List<TemplateCategoryDto>>()
                .setErrorMessage("Popular categories retrieved successfully")
                .setData(categories);

        return ResponseEntity.ok(response);
    }

    /**
     * Search categories
     */
    @GetMapping("/search")
    public ResponseEntity<BaseResponse<List<TemplateCategoryDto>>> searchCategories(
            @RequestParam String q) {

        log.info("Searching categories with term: {}", q);

        List<TemplateCategoryDto> categories = categorizationService.searchCategories(q);

        BaseResponse<List<TemplateCategoryDto>> response = new BaseResponse<List<TemplateCategoryDto>>()
                .setErrorMessage("Categories search completed successfully")
                .setData(categories);

        return ResponseEntity.ok(response);
    }

    /**
     * Create a new category
     */
    @PostMapping
    public ResponseEntity<BaseResponse<TemplateCategoryDto>> createCategory(
            @Valid @RequestBody CreateTemplateCategoryRequest request) {

        log.info("Creating category: {}", request.getName());

        TemplateCategoryDto category = categorizationService.createCategory(request);

        BaseResponse<TemplateCategoryDto> response = new BaseResponse<TemplateCategoryDto>()
                .setErrorMessage("Category created successfully")
                .setData(category);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Update an existing category
     */
    @PutMapping("/{categoryId}")
    public ResponseEntity<BaseResponse<TemplateCategoryDto>> updateCategory(
            @PathVariable Long categoryId,
            @Valid @RequestBody CreateTemplateCategoryRequest request) {

        log.info("Updating category: {}", categoryId);

        TemplateCategoryDto category = categorizationService.updateCategory(categoryId, request);

        BaseResponse<TemplateCategoryDto> response = new BaseResponse<TemplateCategoryDto>()
                .setErrorMessage("Category updated successfully")
                .setData(category);

        return ResponseEntity.ok(response);
    }

    /**
     * Delete a category
     */
    @DeleteMapping("/{categoryId}")
    public ResponseEntity<BaseResponse<Void>> deleteCategory(@PathVariable Long categoryId) {
        log.info("Deleting category: {}", categoryId);

        categorizationService.deleteCategory(categoryId);

        BaseResponse<Void> response = new BaseResponse<Void>()
                .setErrorMessage("Category deleted successfully");

        return ResponseEntity.ok(response);
    }
}