package ai.content.auto.config;

import ai.content.auto.entity.N8nConfig;
import ai.content.auto.repository.N8nConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Initialize AI provider configurations
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(2) // Run after DataInitializer
public class AIProviderDataInitializer implements CommandLineRunner {

    private final N8nConfigRepository n8nConfigRepository;

    @Value("${spring.ai.openai.api-key:}")
    private String openaiApiKey;

    @Value("${spring.ai.gemini.api-key:}")
    private String geminiApiKey;

    @Value("${spring.ai.gemini.api-url:https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent}")
    private String geminiApiUrl;

    @Override
    public void run(String... args) throws Exception {
        initializeAIProviderConfigs();
    }

    private void initializeAIProviderConfigs() {
        // Initialize OpenAI configuration if not exists
        if (n8nConfigRepository.findN8nConfigByAgentName("openai").isEmpty()) {
            createOpenAIConfig();
        } else {
            log.info("OpenAI configuration already exists");
        }

        // Initialize Gemini configuration if not exists
        if (n8nConfigRepository.findN8nConfigByAgentName("gemini").isEmpty()) {
            createGeminiConfig();
        } else {
            log.info("Gemini configuration already exists");
        }
    }

    private void createOpenAIConfig() {
        N8nConfig openaiConfig = new N8nConfig();
        openaiConfig.setAgentName("openai");
        openaiConfig.setAgentUrl("https://api.openai.com/v1/chat/completions");
        openaiConfig.setXApiKey(openaiApiKey);
        openaiConfig.setModel("gpt-4o-mini");
        openaiConfig.setTemperature(0.7);

        n8nConfigRepository.save(openaiConfig);
        log.info("Created OpenAI configuration with model: {}", openaiConfig.getModel());
    }

    private void createGeminiConfig() {
        N8nConfig geminiConfig = new N8nConfig();
        geminiConfig.setAgentName("gemini");
        geminiConfig.setAgentUrl(geminiApiUrl);
        geminiConfig.setXApiKey(geminiApiKey);
        geminiConfig.setModel("gemini-pro");
        geminiConfig.setTemperature(0.7);

        n8nConfigRepository.save(geminiConfig);
        log.info("Created Gemini configuration with model: {} and URL: {}",
                geminiConfig.getModel(), geminiConfig.getAgentUrl());
    }
}