package ai.content.auto.repository;

import ai.content.auto.entity.VideoGenerationJob;
import ai.content.auto.entity.VideoGenerationJob.JobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface VideoGenerationJobRepository extends JpaRepository<VideoGenerationJob, Long> {

        List<VideoGenerationJob> findByStatus(String status);

        List<VideoGenerationJob> findByUser_Id(Long userId);

        // Find by jobId (unique identifier)
        Optional<VideoGenerationJob> findByJobId(String jobId);

        // Find by user with pagination and ordering
        Page<VideoGenerationJob> findByUser_IdOrderByCreatedAtDesc(Long userId, Pageable pageable);

        // Find by batch ID with ordering
        List<VideoGenerationJob> findByBatchIdOrderByBatchPositionAsc(String batchId);

        // Count by user and status
        long countByUser_IdAndStatus(Long userId, JobStatus status);

        // Count by status
        long countByStatus(JobStatus status);

        // Find scheduled jobs by user
        @Query("SELECT j FROM VideoGenerationJob j WHERE j.user.id = :userId " +
                        "AND j.scheduledAt <= :now AND j.status = :status " +
                        "ORDER BY j.scheduledAt ASC")
        Page<VideoGenerationJob> findScheduledJobsByUser(
                        @Param("userId") Long userId,
                        @Param("now") Instant now,
                        @Param("status") JobStatus status,
                        Pageable pageable);

        // Find next jobs to process
        @Query("SELECT j FROM VideoGenerationJob j WHERE j.status = :status " +
                        "AND (j.scheduledAt IS NULL OR j.scheduledAt <= :now) " +
                        "ORDER BY j.priority DESC, j.createdAt ASC")
        List<VideoGenerationJob> findNextJobsToProcess(
                        @Param("status") JobStatus status,
                        @Param("now") Instant now,
                        Pageable pageable);

        // Find scheduled jobs ready to queue
        @Query("SELECT j FROM VideoGenerationJob j WHERE j.status = :status " +
                        "AND j.scheduledAt IS NOT NULL AND j.scheduledAt <= :now " +
                        "ORDER BY j.scheduledAt ASC")
        List<VideoGenerationJob> findScheduledJobsReadyToQueue(
                        @Param("status") JobStatus status,
                        @Param("now") Instant now,
                        Pageable pageable);

        // Find jobs ready for retry
        @Query("SELECT j FROM VideoGenerationJob j WHERE j.status = :status " +
                        "AND j.nextRetryAt IS NOT NULL AND j.nextRetryAt <= :now " +
                        "ORDER BY j.nextRetryAt ASC")
        List<VideoGenerationJob> findJobsReadyForRetry(
                        @Param("status") JobStatus status,
                        @Param("now") Instant now,
                        Pageable pageable);

        // Find stale processing jobs
        @Query("SELECT j FROM VideoGenerationJob j WHERE j.status = :status " +
                        "AND j.startedAt IS NOT NULL AND j.startedAt < :staleThreshold")
        List<VideoGenerationJob> findStaleProcessingJobs(
                        @Param("status") JobStatus status,
                        @Param("staleThreshold") Instant staleThreshold);

        // Find old completed jobs for cleanup
        @Query("SELECT j FROM VideoGenerationJob j WHERE j.status IN :statuses " +
                        "AND j.completedAt IS NOT NULL AND j.completedAt < :threshold " +
                        "ORDER BY j.completedAt ASC")
        List<VideoGenerationJob> findOldCompletedJobs(
                        @Param("statuses") List<JobStatus> statuses,
                        @Param("threshold") Instant threshold,
                        Pageable pageable);
}
