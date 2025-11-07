package ai.content.auto.repository;

import ai.content.auto.entity.TemplateCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TemplateCategoryRepository extends JpaRepository<TemplateCategory, Long> {

    /**
     * Find all root categories (no parent) ordered by sort order
     */
    @Query("SELECT tc FROM TemplateCategory tc WHERE tc.parent IS NULL AND tc.status = 'ACTIVE' ORDER BY tc.sortOrder, tc.name")
    List<TemplateCategory> findRootCategories();

    /**
     * Find categories by parent ID
     */
    @Query("SELECT tc FROM TemplateCategory tc WHERE tc.parent.id = :parentId AND tc.status = 'ACTIVE' ORDER BY tc.sortOrder, tc.name")
    List<TemplateCategory> findByParentId(@Param("parentId") Long parentId);

    /**
     * Find category by slug
     */
    Optional<TemplateCategory> findBySlugAndStatus(String slug, String status);

    /**
     * Find category by name
     */
    Optional<TemplateCategory> findByNameAndStatus(String name, String status);

    /**
     * Find categories with template count greater than zero
     */
    @Query("SELECT tc FROM TemplateCategory tc WHERE tc.templateCount > 0 AND tc.status = 'ACTIVE' ORDER BY tc.templateCount DESC")
    List<TemplateCategory> findCategoriesWithTemplates();

    /**
     * Find popular categories (high template count)
     */
    @Query("SELECT tc FROM TemplateCategory tc WHERE tc.status = 'ACTIVE' ORDER BY tc.templateCount DESC")
    List<TemplateCategory> findPopularCategories();

    /**
     * Search categories by name or description
     */
    @Query("SELECT tc FROM TemplateCategory tc WHERE " +
            "(LOWER(tc.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(tc.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) AND " +
            "tc.status = 'ACTIVE' " +
            "ORDER BY tc.templateCount DESC")
    List<TemplateCategory> searchCategories(@Param("searchTerm") String searchTerm);

    /**
     * Find all categories in hierarchy (parent and children)
     */
    @Query("SELECT tc FROM TemplateCategory tc WHERE " +
            "(tc.id = :categoryId OR tc.parent.id = :categoryId) AND " +
            "tc.status = 'ACTIVE' " +
            "ORDER BY tc.parent.id NULLS FIRST, tc.sortOrder")
    List<TemplateCategory> findCategoryHierarchy(@Param("categoryId") Long categoryId);
}