package ai.content.auto.repository;

import ai.content.auto.entity.VideoAnalytics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface VideoAnalyticsRepository extends JpaRepository<VideoAnalytics, Long> {

    List<VideoAnalytics> findByPublicationId(Long publicationId);

    @Query("SELECT va FROM VideoAnalytics va WHERE va.publication.id = :publicationId ORDER BY va.snapshotAt DESC")
    List<VideoAnalytics> findByPublicationIdOrderBySnapshotAtDesc(@Param("publicationId") Long publicationId);

    @Query("SELECT va FROM VideoAnalytics va WHERE va.publication.id = :publicationId ORDER BY va.snapshotAt DESC LIMIT 1")
    Optional<VideoAnalytics> findLatestByPublicationId(@Param("publicationId") Long publicationId);

    @Query("SELECT va FROM VideoAnalytics va WHERE va.snapshotAt BETWEEN :startDate AND :endDate")
    List<VideoAnalytics> findByDateRange(@Param("startDate") Instant startDate, @Param("endDate") Instant endDate);

    @Query("SELECT SUM(va.views) FROM VideoAnalytics va WHERE va.publication.platform = :platform")
    Long getTotalViewsByPlatform(@Param("platform") String platform);

    @Query("SELECT AVG(va.engagementRate) FROM VideoAnalytics va WHERE va.publication.platform = :platform")
    Double getAverageEngagementRateByPlatform(@Param("platform") String platform);
}
