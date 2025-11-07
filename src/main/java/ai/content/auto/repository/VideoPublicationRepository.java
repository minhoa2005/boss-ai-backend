package ai.content.auto.repository;

import ai.content.auto.entity.VideoPublication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface VideoPublicationRepository extends JpaRepository<VideoPublication, Long> {

    List<VideoPublication> findByVideoJobId(Long videoJobId);

    List<VideoPublication> findByStatus(String status);

    List<VideoPublication> findByPlatform(String platform);

    @Query("SELECT vp FROM VideoPublication vp WHERE vp.status = 'SCHEDULED' AND vp.scheduledAt <= :now")
    List<VideoPublication> findScheduledPublicationsReadyToPublish(@Param("now") Instant now);

    @Query("SELECT vp FROM VideoPublication vp WHERE vp.status = 'FAILED' AND vp.retryCount < vp.maxRetries")
    List<VideoPublication> findFailedPublicationsForRetry();

    Optional<VideoPublication> findByPlatformVideoId(String platformVideoId);

    @Query("SELECT vp FROM VideoPublication vp WHERE vp.createdBy = :userId ORDER BY vp.createdAt DESC")
    List<VideoPublication> findByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(vp) FROM VideoPublication vp WHERE vp.status = 'PUBLISHED' AND vp.platform = :platform")
    Long countPublishedByPlatform(@Param("platform") String platform);
}
