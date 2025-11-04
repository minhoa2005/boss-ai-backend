package ai.content.auto.repository;

import ai.content.auto.entity.ContentVersion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for ContentVersion entity.
 * Provides data access methods for content versioning functionality.
 * 
 * Requirements: 1.1, 1.2
 */
@Repository
public interface ContentVersionRepository extends JpaRepository<ContentVersion, Long> {

        /**
         * Find all versions for a specific content, ordered by version number
         * descending.
         * 
         * @param contentId The content ID
         * @return List of content versions
         */
        List<ContentVersion> findByContentIdOrderByVersionNumberDesc(Long contentId);

        /**
         * Find all versions for a specific content with pagination.
         * 
         * @param contentId The content ID
         * @param pageable  Pagination information
         * @return Page of content versions
         */
        Page<ContentVersion> findByContentIdOrderByVersionNumberDesc(Long contentId, Pageable pageable);

        /**
         * Find a specific version of content.
         * 
         * @param contentId     The content ID
         * @param versionNumber The version number
         * @return Optional ContentVersion
         */
        Optional<ContentVersion> findByContentIdAndVersionNumber(Long contentId, Integer versionNumber);

        /**
         * Find the latest version for a specific content.
         * 
         * @param contentId The content ID
         * @return Optional ContentVersion
         */
        @Query("SELECT cv FROM ContentVersion cv WHERE cv.contentId = :contentId " +
                        "AND cv.versionNumber = (SELECT MAX(cv2.versionNumber) FROM ContentVersion cv2 WHERE cv2.contentId = :contentId)")
        Optional<ContentVersion> findLatestVersionByContentId(@Param("contentId") Long contentId);

        /**
         * Get the next version number for a content.
         * 
         * @param contentId The content ID
         * @return Next version number
         */
        @Query("SELECT COALESCE(MAX(cv.versionNumber), 0) + 1 FROM ContentVersion cv WHERE cv.contentId = :contentId")
        Integer getNextVersionNumber(@Param("contentId") Long contentId);

        /**
         * Find versions created by a specific user.
         * 
         * @param createdBy User ID
         * @param pageable  Pagination information
         * @return Page of content versions
         */
        Page<ContentVersion> findByCreatedByOrderByCreatedAtDesc(Long createdBy, Pageable pageable);

        /**
         * Find versions by AI provider.
         * 
         * @param aiProvider AI provider name
         * @param pageable   Pagination information
         * @return Page of content versions
         */
        Page<ContentVersion> findByAiProviderOrderByCreatedAtDesc(String aiProvider, Pageable pageable);

        /**
         * Find versions created within a date range.
         * 
         * @param startDate Start date
         * @param endDate   End date
         * @param pageable  Pagination information
         * @return Page of content versions
         */
        Page<ContentVersion> findByCreatedAtBetweenOrderByCreatedAtDesc(
                        Instant startDate, Instant endDate, Pageable pageable);

        /**
         * Find versions with quality score above threshold.
         * 
         * @param qualityThreshold Minimum quality score
         * @param pageable         Pagination information
         * @return Page of content versions
         */
        @Query("SELECT cv FROM ContentVersion cv WHERE cv.qualityScore >= :qualityThreshold " +
                        "ORDER BY cv.qualityScore DESC, cv.createdAt DESC")
        Page<ContentVersion> findByQualityScoreGreaterThanEqual(
                        @Param("qualityThreshold") java.math.BigDecimal qualityThreshold, Pageable pageable);

        /**
         * Count total versions for a content.
         * 
         * @param contentId The content ID
         * @return Total version count
         */
        long countByContentId(Long contentId);

        /**
         * Count versions created by a user.
         * 
         * @param createdBy User ID
         * @return Version count
         */
        long countByCreatedBy(Long createdBy);

        /**
         * Find top performing versions across all content.
         * 
         * @param pageable Pagination information
         * @return Page of top performing versions
         */
        @Query("SELECT cv FROM ContentVersion cv WHERE cv.qualityScore IS NOT NULL " +
                        "ORDER BY cv.qualityScore DESC, cv.readabilityScore DESC, cv.seoScore DESC")
        Page<ContentVersion> findTopPerformingVersions(Pageable pageable);

        /**
         * Find versions that need performance analysis (missing quality scores).
         * 
         * @param pageable Pagination information
         * @return Page of versions needing analysis
         */
        @Query("SELECT cv FROM ContentVersion cv WHERE cv.qualityScore IS NULL " +
                        "ORDER BY cv.createdAt DESC")
        Page<ContentVersion> findVersionsNeedingAnalysis(Pageable pageable);

        /**
         * Get version statistics for a content.
         * 
         * @param contentId The content ID
         * @return Version statistics
         */
        @Query("SELECT new map(" +
                        "COUNT(cv) as totalVersions, " +
                        "AVG(cv.qualityScore) as avgQualityScore, " +
                        "MAX(cv.qualityScore) as maxQualityScore, " +
                        "MIN(cv.qualityScore) as minQualityScore, " +
                        "AVG(cv.processingTimeMs) as avgProcessingTime, " +
                        "SUM(cv.generationCost) as totalCost" +
                        ") FROM ContentVersion cv WHERE cv.contentId = :contentId")
        java.util.Map<String, Object> getVersionStatistics(@Param("contentId") Long contentId);

        /**
         * Find versions by multiple criteria for advanced search.
         * 
         * @param contentId       Optional content ID filter
         * @param aiProvider      Optional AI provider filter
         * @param industry        Optional industry filter
         * @param tone            Optional tone filter
         * @param minQualityScore Optional minimum quality score
         * @param startDate       Optional start date filter
         * @param endDate         Optional end date filter
         * @param pageable        Pagination information
         * @return Page of matching versions
         */
        @Query("SELECT cv FROM ContentVersion cv WHERE " +
                        "(:contentId IS NULL OR cv.contentId = :contentId) AND " +
                        "(:aiProvider IS NULL OR cv.aiProvider = :aiProvider) AND " +
                        "(:industry IS NULL OR cv.industry = :industry) AND " +
                        "(:tone IS NULL OR cv.tone = :tone) AND " +
                        "(:minQualityScore IS NULL OR cv.qualityScore >= :minQualityScore) AND " +
                        "(:startDate IS NULL OR cv.createdAt >= :startDate) AND " +
                        "(:endDate IS NULL OR cv.createdAt <= :endDate) " +
                        "ORDER BY cv.createdAt DESC")
        Page<ContentVersion> findByMultipleCriteria(
                        @Param("contentId") Long contentId,
                        @Param("aiProvider") String aiProvider,
                        @Param("industry") String industry,
                        @Param("tone") String tone,
                        @Param("minQualityScore") java.math.BigDecimal minQualityScore,
                        @Param("startDate") Instant startDate,
                        @Param("endDate") Instant endDate,
                        Pageable pageable);

        /**
         * Delete all versions for a content (cascade delete).
         * 
         * @param contentId The content ID
         * @return Number of deleted versions
         */
        long deleteByContentId(Long contentId);

        /**
         * Check if a version exists for content.
         * 
         * @param contentId     The content ID
         * @param versionNumber The version number
         * @return true if version exists
         */
        boolean existsByContentIdAndVersionNumber(Long contentId, Integer versionNumber);

        /**
         * Find versions by branch name.
         * 
         * @param contentId  The content ID
         * @param branchName The branch name
         * @param pageable   Pagination information
         * @return Page of content versions in the branch
         */
        Page<ContentVersion> findByContentIdAndBranchNameOrderByVersionNumberDesc(
                        Long contentId, String branchName, Pageable pageable);

        /**
         * Find experimental versions.
         * 
         * @param contentId The content ID
         * @param pageable  Pagination information
         * @return Page of experimental versions
         */
        Page<ContentVersion> findByContentIdAndIsExperimentalTrueOrderByVersionNumberDesc(
                        Long contentId, Pageable pageable);

        /**
         * Find versions by tag.
         * 
         * @param contentId  The content ID
         * @param versionTag The version tag
         * @param pageable   Pagination information
         * @return Page of tagged versions
         */
        Page<ContentVersion> findByContentIdAndVersionTagOrderByVersionNumberDesc(
                        Long contentId, String versionTag, Pageable pageable);

        /**
         * Find child versions of a parent version.
         * 
         * @param parentVersionId The parent version ID
         * @param pageable        Pagination information
         * @return Page of child versions
         */
        Page<ContentVersion> findByParentVersionIdOrderByVersionNumberDesc(Long parentVersionId, Pageable pageable);

        /**
         * Find all branches for a content.
         * 
         * @param contentId The content ID
         * @return List of distinct branch names
         */
        @Query("SELECT DISTINCT cv.branchName FROM ContentVersion cv WHERE cv.contentId = :contentId AND cv.branchName IS NOT NULL")
        List<String> findDistinctBranchesByContentId(@Param("contentId") Long contentId);

        /**
         * Find all tags for a content.
         * 
         * @param contentId The content ID
         * @return List of distinct version tags
         */
        @Query("SELECT DISTINCT cv.versionTag FROM ContentVersion cv WHERE cv.contentId = :contentId AND cv.versionTag IS NOT NULL")
        List<String> findDistinctTagsByContentId(@Param("contentId") Long contentId);

        /**
         * Find the latest version in a specific branch.
         * 
         * @param contentId  The content ID
         * @param branchName The branch name
         * @return Optional ContentVersion
         */
        @Query("SELECT cv FROM ContentVersion cv WHERE cv.contentId = :contentId AND cv.branchName = :branchName " +
                        "AND cv.versionNumber = (SELECT MAX(cv2.versionNumber) FROM ContentVersion cv2 " +
                        "WHERE cv2.contentId = :contentId AND cv2.branchName = :branchName)")
        Optional<ContentVersion> findLatestVersionInBranch(@Param("contentId") Long contentId,
                        @Param("branchName") String branchName);

        /**
         * Find versions with pagination and sorting by creation date.
         * 
         * @param contentId The content ID
         * @param pageable  Pagination information
         * @return Page of content versions sorted by creation date
         */
        Page<ContentVersion> findByContentIdOrderByCreatedAtDesc(Long contentId, Pageable pageable);

        /**
         * Find versions with pagination and sorting by quality score.
         * 
         * @param contentId The content ID
         * @param pageable  Pagination information
         * @return Page of content versions sorted by quality score
         */
        @Query("SELECT cv FROM ContentVersion cv WHERE cv.contentId = :contentId " +
                        "ORDER BY cv.qualityScore DESC NULLS LAST, cv.createdAt DESC")
        Page<ContentVersion> findByContentIdOrderByQualityScoreDesc(@Param("contentId") Long contentId,
                        Pageable pageable);

        /**
         * Find versions by content ID and filter by AI provider with pagination.
         * 
         * @param contentId  The content ID
         * @param aiProvider The AI provider name
         * @param pageable   Pagination information
         * @return Page of content versions
         */
        Page<ContentVersion> findByContentIdAndAiProviderOrderByVersionNumberDesc(
                        Long contentId, String aiProvider, Pageable pageable);

        /**
         * Count versions in a specific branch.
         * 
         * @param contentId  The content ID
         * @param branchName The branch name
         * @return Count of versions in the branch
         */
        long countByContentIdAndBranchName(Long contentId, String branchName);
}