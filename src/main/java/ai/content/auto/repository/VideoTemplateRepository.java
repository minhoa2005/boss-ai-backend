package ai.content.auto.repository;

import ai.content.auto.entity.VideoTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for VideoTemplate entity operations.
 */
@Repository
public interface VideoTemplateRepository extends JpaRepository<VideoTemplate, Long> {

    /**
     * Find all public templates.
     */
    List<VideoTemplate> findByIsPublicTrue();

    /**
     * Find templates by category.
     */
    List<VideoTemplate> findByCategory(String category);

    /**
     * Find templates by category and public status.
     */
    List<VideoTemplate> findByCategoryAndIsPublicTrue(String category);

    /**
     * Find templates created by a specific user.
     */
    List<VideoTemplate> findByCreatedById(Long userId);

    /**
     * Find system templates.
     */
    List<VideoTemplate> findByIsSystemTemplateTrue();

    /**
     * Find templates by style name.
     */
    List<VideoTemplate> findByStyleName(String styleName);

    /**
     * Find popular templates ordered by usage count.
     */
    @Query("SELECT vt FROM VideoTemplate vt WHERE vt.isPublic = true ORDER BY vt.usageCount DESC")
    Page<VideoTemplate> findPopularTemplates(Pageable pageable);

    /**
     * Find highly rated templates.
     */
    @Query("SELECT vt FROM VideoTemplate vt WHERE vt.isPublic = true AND vt.averageRating >= :minRating ORDER BY vt.averageRating DESC")
    List<VideoTemplate> findHighlyRatedTemplates(@Param("minRating") Double minRating);

    /**
     * Find templates by aspect ratio.
     */
    List<VideoTemplate> findByAspectRatio(String aspectRatio);

    /**
     * Search templates by name or description.
     */
    @Query("SELECT vt FROM VideoTemplate vt WHERE vt.isPublic = true AND (LOWER(vt.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR LOWER(vt.description) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    List<VideoTemplate> searchTemplates(@Param("searchTerm") String searchTerm);

    /**
     * Find recommended templates for a user based on their usage patterns.
     */
    @Query("SELECT vt FROM VideoTemplate vt WHERE vt.id IN " +
            "(SELECT vtul.template.id FROM VideoTemplateUsageLog vtul " +
            "WHERE vtul.user.id = :userId AND vtul.generationStatus = 'SUCCESS' " +
            "GROUP BY vtul.template.id ORDER BY COUNT(vtul.id) DESC)")
    List<VideoTemplate> findRecommendedTemplatesForUser(@Param("userId") Long userId, Pageable pageable);
}
