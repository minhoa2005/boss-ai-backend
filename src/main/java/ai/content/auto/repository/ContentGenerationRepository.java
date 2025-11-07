package ai.content.auto.repository;

import ai.content.auto.entity.ContentGeneration;
import ai.content.auto.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface ContentGenerationRepository extends JpaRepository<ContentGeneration, Long> {
    List<ContentGeneration> findByUserOrderByCreatedAtDesc(User user);

    /**
     * Find content generations by user ID and date range
     */
    @Query("SELECT cg FROM ContentGeneration cg WHERE " +
            "cg.user.id = :userId AND " +
            "cg.createdAt >= :since " +
            "ORDER BY cg.createdAt DESC")
    List<ContentGeneration> findByUserIdAndCreatedAtAfter(
            @Param("userId") Long userId,
            @Param("since") Instant since);
}
