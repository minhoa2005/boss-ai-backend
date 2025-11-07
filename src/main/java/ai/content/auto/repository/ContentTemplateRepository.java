package ai.content.auto.repository;

import ai.content.auto.entity.ContentTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContentTemplateRepository extends JpaRepository<ContentTemplate, Long> {

        /**
         * Find templates by category and industry, ordered by usage count descending
         */
        @Query("SELECT ct FROM ContentTemplate ct WHERE " +
                        "(:category IS NULL OR ct.category = :category) AND " +
                        "(:industry IS NULL OR ct.industry = :industry) AND " +
                        "ct.status = 'ACTIVE' AND " +
                        "(ct.visibility = 'PUBLIC' OR ct.createdBy.id = :userId) " +
                        "ORDER BY ct.usageCount DESC")
        List<ContentTemplate> findByCategoryAndIndustryOrderByUsageCountDesc(
                        @Param("category") String category,
                        @Param("industry") String industry,
                        @Param("userId") Long userId);

        /**
         * Find recommended templates based on user preferences
         */
        @Query("SELECT ct FROM ContentTemplate ct WHERE " +
                        "ct.status = 'ACTIVE' AND " +
                        "(ct.visibility = 'PUBLIC' OR ct.createdBy.id = :userId) AND " +
                        "(:industries IS NULL OR ct.industry IN :industries) AND " +
                        "(:contentTypes IS NULL OR ct.contentType IN :contentTypes) AND " +
                        "(:tones IS NULL OR ct.tone IN :tones) " +
                        "ORDER BY ct.averageRating DESC, ct.usageCount DESC")
        List<ContentTemplate> findRecommendedTemplates(
                        @Param("userId") Long userId,
                        @Param("industries") List<String> industries,
                        @Param("contentTypes") List<String> contentTypes,
                        @Param("tones") List<String> tones);

        /**
         * Find templates by category with pagination
         */
        @Query("SELECT ct FROM ContentTemplate ct WHERE " +
                        "(:category IS NULL OR ct.category = :category) AND " +
                        "ct.status = 'ACTIVE' AND " +
                        "(ct.visibility = 'PUBLIC' OR ct.createdBy.id = :userId)")
        Page<ContentTemplate> findByCategory(
                        @Param("category") String category,
                        @Param("userId") Long userId,
                        Pageable pageable);

        /**
         * Find featured templates
         */
        @Query("SELECT ct FROM ContentTemplate ct WHERE " +
                        "ct.status = 'ACTIVE' AND " +
                        "ct.isFeatured = true AND " +
                        "(ct.visibility = 'PUBLIC' OR ct.createdBy.id = :userId) " +
                        "ORDER BY ct.averageRating DESC")
        List<ContentTemplate> findFeaturedTemplates(@Param("userId") Long userId);

        /**
         * Find popular templates (high usage count)
         */
        @Query("SELECT ct FROM ContentTemplate ct WHERE " +
                        "ct.status = 'ACTIVE' AND " +
                        "(ct.visibility = 'PUBLIC' OR ct.createdBy.id = :userId) " +
                        "ORDER BY ct.usageCount DESC")
        Page<ContentTemplate> findPopularTemplates(@Param("userId") Long userId, Pageable pageable);

        /**
         * Find templates by content type
         */
        @Query("SELECT ct FROM ContentTemplate ct WHERE " +
                        "ct.contentType = :contentType AND " +
                        "ct.status = 'ACTIVE' AND " +
                        "(ct.visibility = 'PUBLIC' OR ct.createdBy.id = :userId) " +
                        "ORDER BY ct.averageRating DESC")
        List<ContentTemplate> findByContentType(
                        @Param("contentType") String contentType,
                        @Param("userId") Long userId);

        /**
         * Search templates by name or description
         */
        @Query("SELECT ct FROM ContentTemplate ct WHERE " +
                        "(LOWER(ct.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
                        "LOWER(ct.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) AND " +
                        "ct.status = 'ACTIVE' AND " +
                        "(ct.visibility = 'PUBLIC' OR ct.createdBy.id = :userId)")
        Page<ContentTemplate> searchTemplates(
                        @Param("searchTerm") String searchTerm,
                        @Param("userId") Long userId,
                        Pageable pageable);

        /**
         * Find templates created by user
         */
        @Query("SELECT ct FROM ContentTemplate ct WHERE " +
                        "ct.createdBy.id = :userId " +
                        "ORDER BY ct.createdAt DESC")
        Page<ContentTemplate> findByCreatedBy(@Param("userId") Long userId, Pageable pageable);

        /**
         * Find system templates
         */
        @Query("SELECT ct FROM ContentTemplate ct WHERE " +
                        "ct.isSystemTemplate = true AND " +
                        "ct.status = 'ACTIVE' " +
                        "ORDER BY ct.category, ct.name")
        List<ContentTemplate> findSystemTemplates();

        /**
         * Find template by name and user (for uniqueness check)
         */
        Optional<ContentTemplate> findByNameAndCreatedBy_Id(String name, Long userId);

        /**
         * Count templates by category
         */
        @Query("SELECT COUNT(ct) FROM ContentTemplate ct WHERE " +
                        "ct.category = :category AND " +
                        "ct.status = 'ACTIVE' AND " +
                        "(ct.visibility = 'PUBLIC' OR ct.createdBy.id = :userId)")
        Long countByCategory(@Param("category") String category, @Param("userId") Long userId);

        /**
         * Find templates with tags
         * Using native query because JPQL MEMBER OF doesn't work well with array types
         */
        @Query(value = "SELECT * FROM content_templates ct WHERE " +
                        ":tag = ANY(ct.tags) AND " +
                        "ct.status = 'ACTIVE' AND " +
                        "(ct.visibility = 'PUBLIC' OR ct.created_by = :userId)", nativeQuery = true)
        List<ContentTemplate> findByTag(@Param("tag") String tag, @Param("userId") Long userId);

        /**
         * Find templates by status
         */
        List<ContentTemplate> findByStatus(String status);
}