package ai.content.auto.service;

import ai.content.auto.mapper.GenerationJobMapper;
import ai.content.auto.repository.GenerationJobRepository;
import ai.content.auto.repository.UserRepository;
import ai.content.auto.service.ai.AIProviderManager;
import ai.content.auto.websocket.JobStatusWebSocketHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ContentProcessor service
 */
@ExtendWith(MockitoExtension.class)
class ContentProcessorTest {

    @Mock
    private GenerationJobRepository jobRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private GenerationJobMapper jobMapper;

    @Mock
    private AIProviderManager aiProviderManager;

    @Mock
    private JobStatusWebSocketHandler webSocketHandler;

    @Mock
    private ObjectMapper objectMapper;

    private ContentProcessor contentProcessor;

    @BeforeEach
    void setUp() {
        contentProcessor = new ContentProcessor(
                jobRepository,
                userRepository,
                jobMapper,
                aiProviderManager,
                webSocketHandler,
                objectMapper);

        // Set configuration values using reflection
        ReflectionTestUtils.setField(contentProcessor, "maxConcurrentJobs", 10);
        ReflectionTestUtils.setField(contentProcessor, "jobTimeoutSeconds", 120);
        ReflectionTestUtils.setField(contentProcessor, "maxRetryAttempts", 3);
        ReflectionTestUtils.setField(contentProcessor, "retryBaseDelayMs", 1000L);
        ReflectionTestUtils.setField(contentProcessor, "processingBatchSize", 20);
    }

    @Test
    void testGetProcessingStatistics() {
        // When
        Map<String, Object> statistics = contentProcessor.getProcessingStatistics();

        // Then
        assertNotNull(statistics);
        assertTrue(statistics.containsKey("active_jobs"));
        assertTrue(statistics.containsKey("processed_jobs"));
        assertTrue(statistics.containsKey("failed_jobs"));
        assertTrue(statistics.containsKey("available_slots"));
        assertTrue(statistics.containsKey("max_concurrent_jobs"));
        assertTrue(statistics.containsKey("job_timeout_seconds"));
        assertTrue(statistics.containsKey("max_retry_attempts"));

        assertEquals(10, statistics.get("max_concurrent_jobs"));
        assertEquals(120, statistics.get("job_timeout_seconds"));
        assertEquals(3, statistics.get("max_retry_attempts"));
    }

    @Test
    void testProcessingStatisticsInitialValues() {
        // When
        Map<String, Object> statistics = contentProcessor.getProcessingStatistics();

        // Then
        assertEquals(0, statistics.get("active_jobs"));
        assertEquals(0, statistics.get("processed_jobs"));
        assertEquals(0, statistics.get("failed_jobs"));
        assertEquals(10, statistics.get("available_slots")); // Should equal max_concurrent_jobs initially
    }
}