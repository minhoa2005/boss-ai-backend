package ai.content.auto.repository;

import ai.content.auto.entity.TemplateTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TemplateTagRepository extends JpaRepository<TemplateTag, Long> {

    /**
     * Find tag by name
     */
    Optional<TemplateTag> findByNameAndStatus(String name, String status);

    /**
     * Find tag by slug
     */
    Optional<TemplateTag> findBySlugAndStatus(String slug, String status);

    /**
     * Find all active tags ordered by usage count
     */
    @Query("SELECT tt FROM TemplateTag tt WHERE tt.status = 'ACTIVE' ORDER BY tt.usageCount DESC")
    List<TemplateTag> findAllActiveOrderByUsage();

    /**
     * Find popular tags (high usage count)
     */
    @Query("SELECT tt FROM TemplateTag tt WHERE tt.status = 'ACTIVE' AND tt.usageCount > 0 ORDER BY tt.usageCount DESC")
    List<TemplateTag> findPopularTags();

    /**
     * Search tags by name
     */
    @Query("SELECT tt FROM TemplateTag tt WHERE " +
            "LOWER(tt.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) AND " +
            "tt.status = 'ACTIVE' " +
            "ORDER BY tt.usageCount DESC")
    List<TemplateTag> searchTags(@Param("searchTerm") String searchTerm);

    /**
     * Find tags by names (for bulk operations)
     */
    @Query("SELECT tt FROM TemplateTag tt WHERE tt.name IN :names AND tt.status = :status")
    List<TemplateTag> findByNamesAndStatus(@Param("names") List<String> names, @Param("status") String status);

    /**
     * Find unused tags (usage count = 0)
     */
    @Query("SELECT tt FROM TemplateTag tt WHERE tt.usageCount = 0 AND tt.status = 'ACTIVE'")
    List<TemplateTag> findUnusedTags();

    /**
     * Find tags with usage count greater than threshold
     */
    @Query("SELECT tt FROM TemplateTag tt WHERE tt.usageCount >= :threshold AND tt.status = 'ACTIVE' ORDER BY tt.usageCount DESC")
    List<TemplateTag> findTagsWithUsageAbove(@Param("threshold") Integer threshold);
}