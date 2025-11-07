package ai.content.auto.repository;

import ai.content.auto.entity.UserPreset;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserPresetRepository extends JpaRepository<UserPreset, Long> {

        /**
         * Find all presets for a user
         */
        @Query("SELECT up FROM UserPreset up WHERE up.user.id = :userId ORDER BY up.isDefault DESC, up.lastUsedAt DESC")
        List<UserPreset> findByUserId(@Param("userId") Long userId);

        /**
         * Find presets by user with pagination
         */
        @Query("SELECT up FROM UserPreset up WHERE up.user.id = :userId ORDER BY up.isDefault DESC, up.lastUsedAt DESC")
        Page<UserPreset> findByUserId(@Param("userId") Long userId, Pageable pageable);

        /**
         * Find user's default preset
         */
        @Query("SELECT up FROM UserPreset up WHERE up.user.id = :userId AND up.isDefault = true")
        Optional<UserPreset> findDefaultByUserId(@Param("userId") Long userId);

        /**
         * Find presets by category for a user
         */
        @Query("SELECT up FROM UserPreset up WHERE " +
                        "up.user.id = :userId AND " +
                        "(:category IS NULL OR up.category = :category) " +
                        "ORDER BY up.isDefault DESC, up.usageCount DESC")
        List<UserPreset> findByUserIdAndCategory(@Param("userId") Long userId, @Param("category") String category);

        /**
         * Find presets by content type for a user
         */
        @Query("SELECT up FROM UserPreset up WHERE " +
                        "up.user.id = :userId AND " +
                        "(:contentType IS NULL OR up.contentType = :contentType) " +
                        "ORDER BY up.usageCount DESC")
        List<UserPreset> findByUserIdAndContentType(@Param("userId") Long userId,
                        @Param("contentType") String contentType);

        /**
         * Find favorite presets for a user
         */
        @Query("SELECT up FROM UserPreset up WHERE " +
                        "up.user.id = :userId AND " +
                        "up.isFavorite = true " +
                        "ORDER BY up.lastUsedAt DESC")
        List<UserPreset> findFavoritesByUserId(@Param("userId") Long userId);

        /**
         * Find shared presets in workspace
         */
        @Query("SELECT up FROM UserPreset up WHERE " +
                        "up.workspaceId = :workspaceId AND " +
                        "up.sharedWithWorkspace = true " +
                        "ORDER BY up.usageCount DESC")
        List<UserPreset> findSharedInWorkspace(@Param("workspaceId") Long workspaceId);

        /**
         * Search presets by name or description
         */
        @Query("SELECT up FROM UserPreset up WHERE " +
                        "up.user.id = :userId AND " +
                        "(LOWER(up.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
                        "LOWER(up.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
                        "ORDER BY up.usageCount DESC")
        List<UserPreset> searchPresets(@Param("userId") Long userId, @Param("searchTerm") String searchTerm);

        /**
         * Find preset by name and user (for uniqueness check)
         */
        Optional<UserPreset> findByNameAndUser_Id(String name, Long userId);

        /**
         * Find most used presets for a user
         */
        @Query("SELECT up FROM UserPreset up WHERE " +
                        "up.user.id = :userId " +
                        "ORDER BY up.usageCount DESC")
        Page<UserPreset> findMostUsedByUserId(@Param("userId") Long userId, Pageable pageable);

        /**
         * Find recently used presets for a user
         */
        @Query("SELECT up FROM UserPreset up WHERE " +
                        "up.user.id = :userId AND " +
                        "up.lastUsedAt IS NOT NULL " +
                        "ORDER BY up.lastUsedAt DESC")
        List<UserPreset> findRecentlyUsedByUserId(@Param("userId") Long userId, Pageable pageable);

        /**
         * Unset all default presets for a user (used when setting a new default)
         */
        @Modifying
        @Query("UPDATE UserPreset up SET up.isDefault = false WHERE up.user.id = :userId AND up.isDefault = true")
        void unsetDefaultPresets(@Param("userId") Long userId);

        /**
         * Count presets by user
         */
        @Query("SELECT COUNT(up) FROM UserPreset up WHERE up.user.id = :userId")
        Long countByUserId(@Param("userId") Long userId);

        /**
         * Find presets with tags
         * Using native query because JPQL MEMBER OF doesn't work well with array types
         */
        @Query(value = "SELECT * FROM user_presets up WHERE " +
                        "up.user_id = :userId AND " +
                        ":tag = ANY(up.tags)", nativeQuery = true)
        List<UserPreset> findByUserIdAndTag(@Param("userId") Long userId, @Param("tag") String tag);

        /**
         * Update usage statistics
         */
        @Modifying
        @Query("UPDATE UserPreset up SET " +
                        "up.usageCount = up.usageCount + 1, " +
                        "up.totalUses = up.totalUses + 1, " +
                        "up.lastUsedAt = CURRENT_TIMESTAMP " +
                        "WHERE up.id = :presetId")
        void incrementUsageCount(@Param("presetId") Long presetId);
}