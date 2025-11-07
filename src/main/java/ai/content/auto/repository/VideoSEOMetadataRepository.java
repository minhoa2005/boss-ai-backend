package ai.content.auto.repository;

import ai.content.auto.entity.VideoSEOMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VideoSEOMetadataRepository extends JpaRepository<VideoSEOMetadata, Long> {

    Optional<VideoSEOMetadata> findByVideoJobId(Long videoJobId);

    @Query("SELECT vsm FROM VideoSEOMetadata vsm WHERE vsm.seoScore >= :minScore ORDER BY vsm.seoScore DESC")
    List<VideoSEOMetadata> findBySeoScoreGreaterThanEqual(@Param("minScore") Double minScore);

    @Query("SELECT vsm FROM VideoSEOMetadata vsm WHERE vsm.contentCategory = :category")
    List<VideoSEOMetadata> findByContentCategory(@Param("category") String category);

    @Query("SELECT AVG(vsm.seoScore) FROM VideoSEOMetadata vsm")
    Double getAverageSeoScore();
}
