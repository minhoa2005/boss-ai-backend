package ai.content.auto.service;

import ai.content.auto.constants.ContentConstants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class to verify OpenAI incomplete response handling
 */
@ExtendWith(MockitoExtension.class)
class OpenAiServiceIncompleteResponseTest {

    @InjectMocks
    private OpenAiService openAiService;

    @Test
    void testProcessIncompleteResponseWithValidContent() throws Exception {
        // Create test data matching the real OpenAI incomplete response format
        Map<String, Object> responseBody = createIncompleteResponseWithContent();

        // Use reflection to access the private method
        Method processMethod = OpenAiService.class.getDeclaredMethod(
                "processOpenAiResponse", Map.class, long.class);
        processMethod.setAccessible(true);

        // Execute the method
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) processMethod.invoke(
                openAiService, responseBody, System.currentTimeMillis());

        // Verify the response is processed successfully
        assertEquals(ContentConstants.STATUS_COMPLETED, result.get("status"));
        assertTrue((Boolean) result.get("isIncomplete"));
        assertNotNull(result.get("generatedContent"));
        assertNotNull(result.get("incompleteDetails"));

        // Verify content metrics are calculated
        assertNotNull(result.get("wordCount"));
        assertNotNull(result.get("characterCount"));
        assertTrue((Integer) result.get("wordCount") > 0);
        assertTrue((Integer) result.get("characterCount") > 0);

        System.out.println("✅ Incomplete response processed successfully:");
        System.out.println("Status: " + result.get("status"));
        System.out.println("Is Incomplete: " + result.get("isIncomplete"));
        System.out.println("Content Length: " + result.get("characterCount"));
        System.out.println("Word Count: " + result.get("wordCount"));
    }

    @Test
    void testProcessCompletedResponseStillWorks() throws Exception {
        // Create test data for completed response
        Map<String, Object> responseBody = createCompletedResponse();

        // Use reflection to access the private method
        Method processMethod = OpenAiService.class.getDeclaredMethod(
                "processOpenAiResponse", Map.class, long.class);
        processMethod.setAccessible(true);

        // Execute the method
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) processMethod.invoke(
                openAiService, responseBody, System.currentTimeMillis());

        // Verify completed response works as before
        assertEquals(ContentConstants.STATUS_COMPLETED, result.get("status"));
        assertNull(result.get("isIncomplete")); // Should not have incomplete flag
        assertNotNull(result.get("generatedContent"));

        System.out.println("✅ Completed response processed successfully:");
        System.out.println("Status: " + result.get("status"));
        System.out.println("Content Length: " + result.get("characterCount"));
    }

    @Test
    void testRejectInvalidStatus() throws Exception {
        // Create test data with invalid status
        Map<String, Object> responseBody = createInvalidStatusResponse();

        // Use reflection to access the private method
        Method processMethod = OpenAiService.class.getDeclaredMethod(
                "processOpenAiResponse", Map.class, long.class);
        processMethod.setAccessible(true);

        // Execute the method
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) processMethod.invoke(
                openAiService, responseBody, System.currentTimeMillis());

        // Verify invalid status is rejected
        assertEquals(ContentConstants.STATUS_FAILED, result.get("status"));
        assertNotNull(result.get("errorMessage"));
        assertTrue(result.get("errorMessage").toString().contains("not valid"));

        System.out.println("✅ Invalid status properly rejected:");
        System.out.println("Status: " + result.get("status"));
        System.out.println("Error: " + result.get("errorMessage"));
    }

    private Map<String, Object> createIncompleteResponseWithContent() {
        Map<String, Object> response = new HashMap<>();
        response.put("id", "resp_test_incomplete");
        response.put("status", "incomplete");

        // Create incomplete details
        Map<String, Object> incompleteDetails = new HashMap<>();
        incompleteDetails.put("reason", "max_output_tokens");
        response.put("incomplete_details", incompleteDetails);

        // Create output array with content
        Map<String, Object> contentItem = new HashMap<>();
        contentItem.put("type", "output_text");
        contentItem.put("text",
                "### 🐉🎶 Chuyện về Rồng Yêu Âm Nhạc\n\nBạn có biết rằng không phải chỉ có người mới thích âm nhạc? Một chú rồng tên là Kira đã sống trong một hang động đầy màu sắc, nơi mà âm thanh của nhạc cụ vang lên mỗi ngày!");

        Map<String, Object> output = new HashMap<>();
        output.put("id", "msg_test");
        output.put("role", "assistant");
        output.put("type", "message");
        output.put("status", "incomplete");
        output.put("content", List.of(contentItem));

        response.put("output", List.of(output));

        // Add usage information
        Map<String, Object> usage = new HashMap<>();
        usage.put("total_tokens", 300);
        usage.put("input_tokens", 100);
        usage.put("output_tokens", 200);
        response.put("usage", usage);

        return response;
    }

    private Map<String, Object> createCompletedResponse() {
        Map<String, Object> response = new HashMap<>();
        response.put("id", "resp_test_completed");
        response.put("status", "completed");

        // Create output array with content
        Map<String, Object> contentItem = new HashMap<>();
        contentItem.put("type", "output_text");
        contentItem.put("text", "This is a completed response with full content generated successfully.");

        Map<String, Object> output = new HashMap<>();
        output.put("id", "msg_test");
        output.put("role", "assistant");
        output.put("type", "message");
        output.put("status", "completed");
        output.put("content", List.of(contentItem));

        response.put("output", List.of(output));

        // Add usage information
        Map<String, Object> usage = new HashMap<>();
        usage.put("total_tokens", 150);
        usage.put("input_tokens", 50);
        usage.put("output_tokens", 100);
        response.put("usage", usage);

        return response;
    }

    private Map<String, Object> createInvalidStatusResponse() {
        Map<String, Object> response = new HashMap<>();
        response.put("id", "resp_test_invalid");
        response.put("status", "failed"); // Invalid status

        return response;
    }
}