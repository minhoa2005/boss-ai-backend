package ai.content.auto.config;

import ai.content.auto.service.ConfigurationValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Validates configuration synchronization between ContentConstants and database
 * at application startup
 */
@Component
@Order(1000) // Run after other initialization components
@RequiredArgsConstructor
@Slf4j
public class ConfigurationStartupValidator implements ApplicationRunner {

    private final ConfigurationValidationService configurationValidationService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("Starting configuration validation at application startup...");

        try {
            // Validate that constants are synchronized with database
            configurationValidationService.validateConstantsSynchronization();

            log.info("✅ Configuration validation completed successfully");

        } catch (Exception e) {
            log.error("❌ Configuration validation failed at startup", e);

            // Log detailed information for troubleshooting
            log.error("This indicates that ContentConstants values don't match database configurations.");
            log.error("Please ensure that:");
            log.error("1. Database has been properly initialized with configuration data");
            log.error("2. ContentConstants values match the 'value' field in configs_primary table");
            log.error(
                    "3. All required configuration categories exist: content_type, tone, language, industry, target_audience");

            // Don't fail application startup, just log the error
            // In production, you might want to fail startup to ensure data consistency
            log.warn("Application will continue to run, but configuration inconsistencies may cause issues");
        }
    }
}