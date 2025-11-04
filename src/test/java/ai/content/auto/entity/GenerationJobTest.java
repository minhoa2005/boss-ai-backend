package ai.content.auto.entity;

import ai.content.auto.entity.GenerationJob.JobStatus;
import ai.content.auto.entity.GenerationJob.JobPriority;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for GenerationJob entity
 */
class GenerationJobTest {

    @Test
    void testGenerationJobCreation() {
        // Given
        Map<String, Object> requestParams = new HashMap<>();
        requestParams.put("content", "Test content");
        requestParams.put("industry", "Technology");

        Instant now = Instant.now();
        Instant expiresAt = now.plus(24, ChronoUnit.HOURS);

        // When
        GenerationJob job = GenerationJob.builder()
                .jobId("test-job-123")
                .userId(1L)
                .requestParams(requestParams)
                .status(JobStatus.QUEUED)
                .priority(JobPriority.STANDARD)
                .contentType("blog-post")
                .maxRetries(3)
                .expiresAt(expiresAt)
                .createdAt(now)
                .updatedAt(now)
                .build();

        // Then
        assertNotNull(job);
        assertEquals("test-job-123", job.getJobId());
        assertEquals(1L, job.getUserId());
        assertEquals(JobStatus.QUEUED, job.getStatus());
        assertEquals(JobPriority.STANDARD, job.getPriority());
        assertEquals("blog-post", job.getContentType());
        assertEquals(3, job.getMaxRetries());
        assertEquals(0, job.getRetryCount());
        assertTrue(job.canRetry());
        assertFalse(job.isFinalState());
    }

    @Test
    void testJobStatusTransitions() {
        // Given
        GenerationJob job = GenerationJob.builder()
                .jobId("test-job-456")
                .status(JobStatus.QUEUED)
                .retryCount(0)
                .maxRetries(3)
                .build();

        // Test QUEUED state
        assertTrue(job.canRetry());
        assertFalse(job.isFinalState());

        // Test PROCESSING state
        job.setStatus(JobStatus.PROCESSING);
        assertFalse(job.canRetry());
        assertFalse(job.isFinalState());

        // Test COMPLETED state
        job.setStatus(JobStatus.COMPLETED);
        assertFalse(job.canRetry());
        assertTrue(job.isFinalState());

        // Test FAILED state
        job.setStatus(JobStatus.FAILED);
        assertTrue(job.canRetry());
        assertTrue(job.isFinalState());
    }

    @Test
    void testJobPriorityOrdering() {
        // Test priority levels for queue ordering
        assertTrue(JobPriority.PREMIUM.getLevel() < JobPriority.STANDARD.getLevel());
        assertTrue(JobPriority.STANDARD.getLevel() < JobPriority.BATCH.getLevel());

        assertEquals(1, JobPriority.PREMIUM.getLevel());
        assertEquals(2, JobPriority.STANDARD.getLevel());
        assertEquals(3, JobPriority.BATCH.getLevel());
    }

    @Test
    void testJobExpiration() {
        // Given
        Instant pastTime = Instant.now().minus(1, ChronoUnit.HOURS);
        Instant futureTime = Instant.now().plus(1, ChronoUnit.HOURS);

        GenerationJob expiredJob = GenerationJob.builder()
                .jobId("expired-job")
                .expiresAt(pastTime)
                .build();

        GenerationJob validJob = GenerationJob.builder()
                .jobId("valid-job")
                .expiresAt(futureTime)
                .build();

        // Then
        assertTrue(expiredJob.isExpired());
        assertFalse(validJob.isExpired());
    }

    @Test
    void testProcessingDurationCalculation() {
        // Given
        Instant startTime = Instant.now().minus(5, ChronoUnit.MINUTES);
        Instant endTime = Instant.now();

        GenerationJob job = GenerationJob.builder()
                .jobId("duration-test")
                .startedAt(startTime)
                .completedAt(endTime)
                .build();

        // When
        Long duration = job.getProcessingDuration();

        // Then
        assertNotNull(duration);
        assertTrue(duration > 0);
        assertTrue(duration >= 300000); // At least 5 minutes in milliseconds
    }

    @Test
    void testRetryLogic() {
        // Given
        GenerationJob job = GenerationJob.builder()
                .jobId("retry-test")
                .status(JobStatus.FAILED)
                .retryCount(2)
                .maxRetries(3)
                .build();

        // Test can retry
        assertTrue(job.canRetry());

        // Test max retries reached
        job.setRetryCount(3);
        assertFalse(job.canRetry());

        // Test completed job cannot retry
        job.setStatus(JobStatus.COMPLETED);
        job.setRetryCount(1);
        assertFalse(job.canRetry());
    }
}