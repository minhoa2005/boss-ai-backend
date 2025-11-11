package ai.content.auto.service;

import ai.content.auto.dtos.ContentWorkflowRequest;
import ai.content.auto.entity.N8nConfig;
import ai.content.auto.exception.BusinessException;
import ai.content.auto.repository.N8nConfigRepository;
import ai.content.auto.util.StringUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class N8nService {

    private final RestTemplate restTemplate;

    private final N8nConfigRepository n8nConfigRepository;

    public Map<String, Object> triggerWorkflow(ContentWorkflowRequest request, Long userId) {
        try {
            // 1. Validate input
            validateWorkflowRequest(request, userId);

            log.info("Triggering N8N workflow for user: {}", userId);

            // 2. Build workflow request
            Map<String, Object> workflowRequest = buildWorkflowRequest(request, userId);

            // 3. Call N8N webhook
            Map<String, Object> webhookResponse = callN8nWebhook(workflowRequest,
                    String.valueOf(workflowRequest.get("n8n_url")));

            // 4. Build result
            Map<String, Object> result = new HashMap<>();
            result.put("status", "SUCCESS");
            result.put("message", "Workflow triggered successfully");
            result.put("workflowResponse", webhookResponse);

            log.info("N8N workflow triggered successfully for user: {}", userId);
            return result;

        } catch (BusinessException e) {
            log.error("Business error triggering N8N workflow for user: {}", userId, e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error triggering N8N workflow for user: {}", userId, e);

            Map<String, Object> result = new HashMap<>();
            result.put("status", "FAILED");
            result.put("message", "Failed to trigger workflow: " + e.getMessage());

            return result;
        }
    }

    private void validateWorkflowRequest(ContentWorkflowRequest request, Long userId) {
        if (request == null) {
            throw new BusinessException("Workflow request is required");
        }
        if (userId == null) {
            throw new BusinessException("User ID is required");
        }
        if (StringUtil.isBlank(request.getGeneratedContent())) {
            throw new BusinessException("Generated content is required");
        }
    }

    private Map<String, Object> callN8nWebhook(Map<String, Object> workflowRequest, final String n8nUrl) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(workflowRequest, headers);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                n8nUrl,
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<Map<String, Object>>() {
                });

        Map<String, Object> responseBody = response.getBody();
        if (responseBody == null) {
            responseBody = new HashMap<>();
            responseBody.put("status", "success");
            responseBody.put("message", "Webhook called successfully");
        }

        return responseBody;
    }

    private Map<String, Object> buildWorkflowRequest(ContentWorkflowRequest request, Long userId) {
        String contentId = UUID.randomUUID().toString();
        List<N8nConfig> n8nConfigDtos = n8nConfigRepository.findAll();
        String voiceId = "1ec1fc9a5fd84835a10d42ba28b6ff60";
        String avatarId = "0de024c12ae44ecda0db7bebd1c4bfc5";
        Map<String, Object> payload = new HashMap<>();
        payload.put("content", request.getGeneratedContent());
        payload.put("voice_id", voiceId);
        payload.put("avatar_id", avatarId);
        payload.put("contentId", contentId);
        payload.put("userId", userId);
        payload.put("open_ai_url", n8nConfigDtos.stream().filter(x -> x.getAgentName().equalsIgnoreCase("openai"))
                .findFirst().get().getAgentUrl());
        payload.put("open_ai_key", n8nConfigDtos.stream().filter(x -> x.getAgentName().equalsIgnoreCase("openai"))
                .findFirst().get().getXApiKey());
        payload.put("open_ai_model", n8nConfigDtos.stream().filter(x -> x.getAgentName().equalsIgnoreCase("openai"))
                .findFirst().get().getModel());
        payload.put("heygen_generate", n8nConfigDtos.stream()
                .filter(x -> x.getAgentName().equalsIgnoreCase("heygen-generate")).findFirst().get().getAgentUrl());
        payload.put("heygen_video_status", n8nConfigDtos.stream()
                .filter(x -> x.getAgentName().equalsIgnoreCase("heygen-video_status")).findFirst().get().getAgentUrl());
        payload.put("heygen_api_key", n8nConfigDtos.stream()
                .filter(x -> x.getAgentName().equalsIgnoreCase("heygen-generate")).findFirst().get().getXApiKey());
        payload.put("n8n_url", n8nConfigDtos.stream().filter(x -> x.getAgentName().equalsIgnoreCase("n8n")).findFirst()
                .get().getAgentUrl());

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("industry", request.getIndustry());
        metadata.put("contentType", request.getContentType());
        metadata.put("language", request.getLanguage());
        metadata.put("tone", request.getTone());
        metadata.put("targetAudience", request.getTargetAudience());

        payload.put("metadata",metadata);
        return payload;
    }

    /**
     * Generate video using N8N workflow with template and branding configuration
     */
    public Map<String, Object> generateVideo(String videoScript, String templateName,
            Map<String, Object> brandingConfig,
            Map<String, Object> generationParams) {
        try {
            log.info("Generating video with template: {}", templateName);

            // Build video generation request
            Map<String, Object> videoRequest = buildVideoGenerationRequest(
                    videoScript, templateName, brandingConfig, generationParams);

            // Get N8N configuration
            List<N8nConfig> n8nConfigs = n8nConfigRepository.findAll();
            String n8nUrl = n8nConfigs.stream()
                    .filter(x -> x.getAgentName().equalsIgnoreCase("n8n"))
                    .findFirst()
                    .map(N8nConfig::getAgentUrl)
                    .orElseThrow(() -> new BusinessException("N8N configuration not found"));

            // Call N8N webhook for video generation
            Map<String, Object> webhookResponse = callN8nWebhook(videoRequest, n8nUrl);

            // Extract video information from response
            Map<String, Object> result = new HashMap<>();
            result.put("videoUrl", webhookResponse.getOrDefault("video_url", ""));
            result.put("thumbnailUrl", webhookResponse.getOrDefault("thumbnail_url", ""));
            result.put("videoSize", webhookResponse.getOrDefault("video_size", 0));
            result.put("videoFormat", webhookResponse.getOrDefault("video_format", "mp4"));
            result.put("status", "SUCCESS");

            log.info("Video generation completed successfully");
            return result;

        } catch (BusinessException e) {
            log.error("Business error generating video", e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error generating video", e);
            throw new BusinessException("Failed to generate video: " + e.getMessage());
        }
    }

    private Map<String, Object> buildVideoGenerationRequest(String videoScript, String templateName,
            Map<String, Object> brandingConfig,
            Map<String, Object> generationParams) {
        List<N8nConfig> n8nConfigs = n8nConfigRepository.findAll();

        Map<String, Object> request = new HashMap<>();
        request.put("video_script", videoScript);
        request.put("template_name", templateName);
        request.put("branding_config", brandingConfig != null ? brandingConfig : new HashMap<>());
        request.put("generation_params", generationParams != null ? generationParams : new HashMap<>());

        // Add N8N configuration
        request.put("heygen_generate", n8nConfigs.stream()
                .filter(x -> x.getAgentName().equalsIgnoreCase("heygen-generate"))
                .findFirst()
                .map(N8nConfig::getAgentUrl)
                .orElse(""));
        request.put("heygen_video_status", n8nConfigs.stream()
                .filter(x -> x.getAgentName().equalsIgnoreCase("heygen-video_status"))
                .findFirst()
                .map(N8nConfig::getAgentUrl)
                .orElse(""));
        request.put("heygen_api_key", n8nConfigs.stream()
                .filter(x -> x.getAgentName().equalsIgnoreCase("heygen-generate"))
                .findFirst()
                .map(N8nConfig::getXApiKey)
                .orElse(""));

        return request;
    }
}