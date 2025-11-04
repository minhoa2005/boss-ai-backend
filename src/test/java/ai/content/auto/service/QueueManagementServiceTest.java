package ai.content.auto.service;

import ai.content.auto.dtos.QueueJobRequest;
import ai.content.auto.entity.GenerationJob.JobPriority;
import ai.content.auto.entity.GenerationJob.JobStatus;
import ai.content.auto.repository.GenerationJobRepository;
import ai.content.auto.util.SecurityUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for QueueManagementService
 */
@ExtendWith(MockitoExtension.class)
class QueueManagementServiceTest {

    @Mock
    private GenerationJobRepository jobRepository;

    @Mock
    private SecurityUtil securityUtil;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @InjectMocks
    private QueueManagementService queueManagementService;

    @Test
    void testQueueJobRequest_ShouldCreateValidRequest() {
        // Given
        Map<String, Object> requestParams = new HashMap<>();
        requestParams.put("content", "Test content");
        requestParams.put("industry", "Technology");

        QueueJobRequest request = QueueJobRequest.builder()
                .requestParams(requestParams)
                .contentType("blog-post")
                .priority(JobPriority.STANDARD)
                .build();

        // Then
        assertNotNull(request);
        assertEquals("blog-post", request.getContentType());
        assertEquals(JobPriority.STANDARD, request.getPriority());
        assertEquals(24, request.getExpirationHours()); // Default value
        assertEquals(3, request.getMaxRetries()); // Default value
    }

    @Test
    void testJobPriorityLevels() {
        // Test priority levels
        assertEquals(1, JobPriority.PREMIUM.getLevel());
        assertEquals(2, JobPriority.STANDARD.getLevel());
        assertEquals(3, JobPriority.BATCH.getLevel());
    }

    @Test
    void testJobStatusValues() {
        // Test that all expected job statuses exist
        JobStatus[] statuses = JobStatus.values();
        assertTrue(statuses.length >= 6);

        // Check specific statuses
        assertNotNull(JobStatus.QUEUED);
        assertNotNull(JobStatus.PROCESSING);
        assertNotNull(JobStatus.COMPLETED);
        assertNotNull(JobStatus.FAILED);
        assertNotNull(JobStatus.CANCELLED);
        assertNotNull(JobStatus.EXPIRED);
    }
}