package ai.content.auto.scheduler;

import ai.content.auto.service.TemplatePopularityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled job to update template metrics and popularity scores
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TemplateMetricsScheduler {

    private final TemplatePopularityService popularityService;

    /**
     * Update template metrics every hour
     * Runs at the start of every hour
     */
    @Scheduled(cron = "0 0 * * * *")
    public void updateTemplateMetrics() {
        try {
            log.info("Starting scheduled template metrics update");
            popularityService.updateAllTemplateMetrics();
            log.info("Completed scheduled template metrics update");
        } catch (Exception e) {
            log.error("Error during scheduled template metrics update", e);
        }
    }

    /**
     * Update template metrics every 6 hours (alternative schedule)
     * Uncomment if hourly updates are too frequent
     */
    // @Scheduled(cron = "0 0 */6 * * *")
    // public void updateTemplateMetricsSixHourly() {
    // try {
    // log.info("Starting scheduled template metrics update (6-hourly)");
    // popularityService.updateAllTemplateMetrics();
    // log.info("Completed scheduled template metrics update (6-hourly)");
    // } catch (Exception e) {
    // log.error("Error during scheduled template metrics update", e);
    // }
    // }
}
