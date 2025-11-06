package ai.content.auto.service;

import ai.content.auto.dtos.ContentGenerateRequest;
import ai.content.auto.entity.OpenaiResponseLog;
import ai.content.auto.entity.User;
import ai.content.auto.repository.OpenaiResponseLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Test class to verify OpenAI response ID is properly extracted and saved
 */
@ExtendWith(MockitoExtension.class)
class OpenAiResponseIdTest {

    @Mock
    private OpenaiResponseLogRepository openaiResponseLogRepository;

    @InjectMocks
    private OpenAiService openAiService;

    @Test
    void testOpenAiResponseIdExtraction() {
        // Given
        User user = new User();
        user.setId(1L);

        Map<String, Object> request = new HashMap<>();
        request.put("model", "gpt-4o-mini");

        Map<String, Object> response = new HashMap<>();
        response.put("id", "chatcmpl-123456789");
        response.put("model", "gpt-4o-mini");
        response.put("created_at", String.valueOf(System.currentTimeMillis()));

        // When
        openAiService.saveResponseLogInTransaction(user, request, response, "gpt-4o-mini");

        // Then
        verify(openaiResponseLogRepository).save(argThat(log -> {
            OpenaiResponseLog responseLog = (OpenaiResponseLog) log;
            return "chatcmpl-123456789".equals(responseLog.getOpenaiResponseId());
        }));
    }

    @Test
    void testOpenAiResponseIdExtractionWithSystemFingerprint() {
        // Given
        User user = new User();
        user.setId(1L);

        Map<String, Object> request = new HashMap<>();
        request.put("model", "gpt-4o-mini");

        Map<String, Object> response = new HashMap<>();
        response.put("system_fingerprint", "fp_123456789");
        response.put("model", "gpt-4o-mini");
        response.put("created_at", String.valueOf(System.currentTimeMillis()));

        // When
        openAiService.saveResponseLogInTransaction(user, request, response, "gpt-4o-mini");

        // Then
        verify(openaiResponseLogRepository).save(argThat(log -> {
            OpenaiResponseLog responseLog = (OpenaiResponseLog) log;
            return "fp_123456789".equals(responseLog.getOpenaiResponseId());
        }));
    }

    @Test
    void testOpenAiResponseIdExtractionWithNoId() {
        // Given
        User user = new User();
        user.setId(1L);

        Map<String, Object> request = new HashMap<>();
        request.put("model", "gpt-4o-mini");

        Map<String, Object> response = new HashMap<>();
        response.put("model", "gpt-4o-mini");
        response.put("created_at", String.valueOf(System.currentTimeMillis()));

        // When
        openAiService.saveResponseLogInTransaction(user, request, response, "gpt-4o-mini");

        // Then
        verify(openaiResponseLogRepository).save(argThat(log -> {
            OpenaiResponseLog responseLog = (OpenaiResponseLog) log;
            return responseLog.getOpenaiResponseId() == null;
        }));
    }
}