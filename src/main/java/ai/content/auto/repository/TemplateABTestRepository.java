package ai.content.auto.repository;

import ai.content.auto.entity.TemplateABTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for template A/B test operations
 */
@Repository
public interface TemplateABTestRepository extends JpaRepository<TemplateABTest, Long> {

    /**
     * Find all A/B tests by status
     */
    List<TemplateABTest> findByStatus(String status);

    /**
     * Find A/B tests by template ID (either variant A or B)
     */
    List<TemplateABTest> findByVariantATemplate_IdOrVariantBTemplate_Id(Long variantAId, Long variantBId);

    /**
     * Find active A/B tests for a specific template
     */
    List<TemplateABTest> findByStatusAndVariantATemplate_IdOrStatusAndVariantBTemplate_Id(
            String status1, Long variantAId, String status2, Long variantBId);
}
