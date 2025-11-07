package ai.content.auto.service;

import ai.content.auto.dtos.*;
import ai.content.auto.entity.TemplateCategory;
import ai.content.auto.entity.TemplateTag;
import ai.content.auto.exception.BusinessException;
import ai.content.auto.mapper.TemplateCategoryMapper;
import ai.content.auto.mapper.TemplateTagMapper;
import ai.content.auto.repository.TemplateCategoryRepository;
import ai.content.auto.repository.TemplateTagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TemplateCategorizationService {

    private final TemplateCategoryRepository categoryRepository;
    private final TemplateTagRepository tagRepository;
    private final TemplateCategoryMapper categoryMapper;
    private final TemplateTagMapper tagMapper;

    // ================================
    // CATEGORY MANAGEMENT METHODS
    // ================================

    /**
     * Get all root categories (categories without parent)
     */
    public List<TemplateCategoryDto> getRootCategories() {
        try {
            log.info("Fetching root categories");
            return getRootCategoriesInTransaction();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error fetching root categories", e);
            throw new BusinessException("Failed to fetch categories");
        }
    }

    @Transactional(readOnly = true)
    private List<TemplateCategoryDto> getRootCategoriesInTransaction() {
        List<TemplateCategory> categories = categoryRepository.findRootCategories();
        return categoryMapper.toDtoList(categories);
    }

    /**
     * Get categories by parent ID
     */
    public List<TemplateCategoryDto> getCategoriesByParent(Long parentId) {
        try {
            log.info("Fetching categories by parent ID: {}", parentId);
            return getCategoriesByParentInTransaction(parentId);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error fetching categories by parent: {}", parentId, e);
            throw new BusinessException("Failed to fetch categories");
        }
    }

    @Transactional(readOnly = true)
    private List<TemplateCategoryDto> getCategoriesByParentInTransaction(Long parentId) {
        List<TemplateCategory> categories = categoryRepository.findByParentId(parentId);
        return categoryMapper.toDtoList(categories);
    }

    /**
     * Get category hierarchy (parent and children)
     */
    public List<TemplateCategoryDto> getCategoryHierarchy(Long categoryId) {
        try {
            log.info("Fetching category hierarchy for: {}", categoryId);
            return getCategoryHierarchyInTransaction(categoryId);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error fetching category hierarchy: {}", categoryId, e);
            throw new BusinessException("Failed to fetch category hierarchy");
        }
    }

    @Transactional(readOnly = true)
    private List<TemplateCategoryDto> getCategoryHierarchyInTransaction(Long categoryId) {
        List<TemplateCategory> categories = categoryRepository.findCategoryHierarchy(categoryId);
        return categoryMapper.toDtoList(categories);
    }

    /**
     * Get popular categories (with high template count)
     */
    public List<TemplateCategoryDto> getPopularCategories() {
        try {
            log.info("Fetching popular categories");
            return getPopularCategoriesInTransaction();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error fetching popular categories", e);
            throw new BusinessException("Failed to fetch popular categories");
        }
    }

    @Transactional(readOnly = true)
    private List<TemplateCategoryDto> getPopularCategoriesInTransaction() {
        List<TemplateCategory> categories = categoryRepository.findPopularCategories();
        return categoryMapper.toDtoList(categories);
    }

    /**
     * Search categories by name or description
     */
    public List<TemplateCategoryDto> searchCategories(String searchTerm) {
        try {
            log.info("Searching categories with term: {}", searchTerm);
            return searchCategoriesInTransaction(searchTerm);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error searching categories with term: {}", searchTerm, e);
            throw new BusinessException("Failed to search categories");
        }
    }

    @Transactional(readOnly = true)
    private List<TemplateCategoryDto> searchCategoriesInTransaction(String searchTerm) {
        List<TemplateCategory> categories = categoryRepository.searchCategories(searchTerm);
        return categoryMapper.toDtoList(categories);
    }

    /**
     * Create a new category
     */
    public TemplateCategoryDto createCategory(CreateTemplateCategoryRequest request) {
        try {
            log.info("Creating category: {}", request.getName());

            // Validate category name uniqueness
            validateCategoryNameUniqueness(request.getName());

            return createCategoryInTransaction(request);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error creating category: {}", request.getName(), e);
            throw new BusinessException("Failed to create category");
        }
    }

    @Transactional
    private TemplateCategoryDto createCategoryInTransaction(CreateTemplateCategoryRequest request) {
        // Get parent category if specified
        TemplateCategory parent = null;
        if (request.getParentId() != null) {
            parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new BusinessException("Parent category not found"));
        }

        TemplateCategory category = categoryMapper.toEntity(request, parent);
        TemplateCategory savedCategory = categoryRepository.save(category);

        log.info("Category created successfully: {} with ID: {}", savedCategory.getName(), savedCategory.getId());
        return categoryMapper.toDto(savedCategory);
    }

    /**
     * Update an existing category
     */
    public TemplateCategoryDto updateCategory(Long categoryId, CreateTemplateCategoryRequest request) {
        try {
            log.info("Updating category: {}", categoryId);
            return updateCategoryInTransaction(categoryId, request);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error updating category: {}", categoryId, e);
            throw new BusinessException("Failed to update category");
        }
    }

    @Transactional
    private TemplateCategoryDto updateCategoryInTransaction(Long categoryId, CreateTemplateCategoryRequest request) {
        TemplateCategory category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new BusinessException("Category not found"));

        // Validate name uniqueness if name is being changed
        if (!request.getName().equals(category.getName())) {
            validateCategoryNameUniqueness(request.getName());
        }

        // Get parent category if specified
        TemplateCategory parent = null;
        if (request.getParentId() != null) {
            parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new BusinessException("Parent category not found"));

            // Prevent circular references
            if (isCircularReference(categoryId, request.getParentId())) {
                throw new BusinessException("Cannot set parent category - would create circular reference");
            }
        }

        categoryMapper.updateEntityFromRequest(category, request, parent);
        TemplateCategory savedCategory = categoryRepository.save(category);

        log.info("Category updated successfully: {}", savedCategory.getId());
        return categoryMapper.toDto(savedCategory);
    }

    /**
     * Delete a category
     */
    public void deleteCategory(Long categoryId) {
        try {
            log.info("Deleting category: {}", categoryId);
            deleteCategoryInTransaction(categoryId);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error deleting category: {}", categoryId, e);
            throw new BusinessException("Failed to delete category");
        }
    }

    @Transactional
    private void deleteCategoryInTransaction(Long categoryId) {
        TemplateCategory category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new BusinessException("Category not found"));

        // Check if category has templates
        if (category.getTemplateCount() > 0) {
            throw new BusinessException("Cannot delete category with existing templates");
        }

        // Check if category has children
        List<TemplateCategory> children = categoryRepository.findByParentId(categoryId);
        if (!children.isEmpty()) {
            throw new BusinessException("Cannot delete category with subcategories");
        }

        // Soft delete by setting status to DELETED
        category.setStatus("DELETED");
        category.setUpdatedAt(OffsetDateTime.now());
        categoryRepository.save(category);

        log.info("Category deleted successfully: {}", categoryId);
    }

    // ================================
    // TAG MANAGEMENT METHODS
    // ================================

    /**
     * Get all active tags ordered by usage count
     */
    public List<TemplateTagDto> getAllTags() {
        try {
            log.info("Fetching all active tags");
            return getAllTagsInTransaction();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error fetching all tags", e);
            throw new BusinessException("Failed to fetch tags");
        }
    }

    @Transactional(readOnly = true)
    private List<TemplateTagDto> getAllTagsInTransaction() {
        List<TemplateTag> tags = tagRepository.findAllActiveOrderByUsage();
        return tagMapper.toDtoList(tags);
    }

    /**
     * Get popular tags (high usage count)
     */
    public List<TemplateTagDto> getPopularTags() {
        try {
            log.info("Fetching popular tags");
            return getPopularTagsInTransaction();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error fetching popular tags", e);
            throw new BusinessException("Failed to fetch popular tags");
        }
    }

    @Transactional(readOnly = true)
    private List<TemplateTagDto> getPopularTagsInTransaction() {
        List<TemplateTag> tags = tagRepository.findPopularTags();
        return tagMapper.toDtoList(tags);
    }

    /**
     * Search tags by name
     */
    public List<TemplateTagDto> searchTags(String searchTerm) {
        try {
            log.info("Searching tags with term: {}", searchTerm);
            return searchTagsInTransaction(searchTerm);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error searching tags with term: {}", searchTerm, e);
            throw new BusinessException("Failed to search tags");
        }
    }

    @Transactional(readOnly = true)
    private List<TemplateTagDto> searchTagsInTransaction(String searchTerm) {
        List<TemplateTag> tags = tagRepository.searchTags(searchTerm);
        return tagMapper.toDtoList(tags);
    }

    /**
     * Create a new tag
     */
    public TemplateTagDto createTag(CreateTemplateTagRequest request) {
        try {
            log.info("Creating tag: {}", request.getName());

            // Validate tag name uniqueness
            validateTagNameUniqueness(request.getName());

            return createTagInTransaction(request);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error creating tag: {}", request.getName(), e);
            throw new BusinessException("Failed to create tag");
        }
    }

    @Transactional
    private TemplateTagDto createTagInTransaction(CreateTemplateTagRequest request) {
        TemplateTag tag = tagMapper.toEntity(request);
        TemplateTag savedTag = tagRepository.save(tag);

        log.info("Tag created successfully: {} with ID: {}", savedTag.getName(), savedTag.getId());
        return tagMapper.toDto(savedTag);
    }

    /**
     * Update an existing tag
     */
    public TemplateTagDto updateTag(Long tagId, CreateTemplateTagRequest request) {
        try {
            log.info("Updating tag: {}", tagId);
            return updateTagInTransaction(tagId, request);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error updating tag: {}", tagId, e);
            throw new BusinessException("Failed to update tag");
        }
    }

    @Transactional
    private TemplateTagDto updateTagInTransaction(Long tagId, CreateTemplateTagRequest request) {
        TemplateTag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new BusinessException("Tag not found"));

        // Validate name uniqueness if name is being changed
        if (!request.getName().equals(tag.getName())) {
            validateTagNameUniqueness(request.getName());
        }

        tagMapper.updateEntityFromRequest(tag, request);
        TemplateTag savedTag = tagRepository.save(tag);

        log.info("Tag updated successfully: {}", savedTag.getId());
        return tagMapper.toDto(savedTag);
    }

    /**
     * Delete a tag
     */
    public void deleteTag(Long tagId) {
        try {
            log.info("Deleting tag: {}", tagId);
            deleteTagInTransaction(tagId);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error deleting tag: {}", tagId, e);
            throw new BusinessException("Failed to delete tag");
        }
    }

    @Transactional
    private void deleteTagInTransaction(Long tagId) {
        TemplateTag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new BusinessException("Tag not found"));

        // Soft delete by setting status to DELETED
        tag.setStatus("DELETED");
        tag.setUpdatedAt(OffsetDateTime.now());
        tagRepository.save(tag);

        log.info("Tag deleted successfully: {}", tagId);
    }

    /**
     * Get or create tags by names (for bulk operations)
     */
    public List<TemplateTagDto> getOrCreateTags(List<String> tagNames) {
        try {
            log.info("Getting or creating tags: {}", tagNames);
            return getOrCreateTagsInTransaction(tagNames);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error getting or creating tags: {}", tagNames, e);
            throw new BusinessException("Failed to process tags");
        }
    }

    @Transactional
    private List<TemplateTagDto> getOrCreateTagsInTransaction(List<String> tagNames) {
        // Find existing tags
        List<TemplateTag> existingTags = tagRepository.findByNamesAndStatus(tagNames, "ACTIVE");
        List<String> existingTagNames = existingTags.stream()
                .map(TemplateTag::getName)
                .collect(Collectors.toList());

        // Create missing tags
        List<String> missingTagNames = tagNames.stream()
                .filter(name -> !existingTagNames.contains(name))
                .collect(Collectors.toList());

        for (String tagName : missingTagNames) {
            CreateTemplateTagRequest request = new CreateTemplateTagRequest();
            request.setName(tagName);

            TemplateTag newTag = tagMapper.toEntity(request);
            TemplateTag savedTag = tagRepository.save(newTag);
            existingTags.add(savedTag);
        }

        return tagMapper.toDtoList(existingTags);
    }

    // ================================
    // HELPER METHODS
    // ================================

    private void validateCategoryNameUniqueness(String name) {
        Optional<TemplateCategory> existing = categoryRepository.findByNameAndStatus(name, "ACTIVE");
        if (existing.isPresent()) {
            throw new BusinessException("Category with this name already exists");
        }
    }

    private void validateTagNameUniqueness(String name) {
        Optional<TemplateTag> existing = tagRepository.findByNameAndStatus(name, "ACTIVE");
        if (existing.isPresent()) {
            throw new BusinessException("Tag with this name already exists");
        }
    }

    private boolean isCircularReference(Long categoryId, Long parentId) {
        if (categoryId.equals(parentId)) {
            return true;
        }

        Optional<TemplateCategory> parent = categoryRepository.findById(parentId);
        if (parent.isPresent() && parent.get().getParent() != null) {
            return isCircularReference(categoryId, parent.get().getParent().getId());
        }

        return false;
    }
}