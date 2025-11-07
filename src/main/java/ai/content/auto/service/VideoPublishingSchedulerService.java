package ai.content.auto.service;

import ai.content.auto.dto.VideoPublicationDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service for automated video publishing scheduling
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VideoPublishingSchedulerService {

    private final VideoPublishingService publishingService;

    /**
     * Process scheduled publications every minute
     */
    @Scheduled(fixedRate = 60000) // Run every minute
    public void processScheduledPublications() {
        try {
            List<VideoPublicationDto> readyPublications = publishingService.getScheduledPublicationsReadyToPublish();

            if (!readyPublications.isEmpty()) {
                log.info("Found {} publications ready to publish", readyPublications.size());

                for (VideoPublicationDto publication : readyPublications) {
                    try {
                        processPublication(publication);
                    } catch (Exception e) {
                        log.error("Failed to process publication {}: {}", publication.getId(), e.getMessage());
                        handlePublicationFailure(publication);
                    }
                }
            }

        } catch (Exception e) {
            log.error("Error in scheduled publication processing", e);
        }
    }

    private void processPublication(VideoPublicationDto publication) {
        log.info("Processing publication {} for platform {}", publication.getId(), publication.getPlatform());

        // Update status to PUBLISHING
        publishingService.updatePublicationStatus(
                publication.getId(),
                "PUBLISHING",
                null,
                null);

        // Here you would integrate with actual platform APIs
        // For now, we'll simulate successful publication
        String platformVideoId = "simulated_" + publication.getPlatform() + "_" + System.currentTimeMillis();
        String publicationUrl = "https://" + publication.getPlatform().toLowerCase() + ".com/video/" + platformVideoId;

        // Update status to PUBLISHED
        publishingService.updatePublicationStatus(
                publication.getId(),
                "PUBLISHED",
                platformVideoId,
                publicationUrl);

        log.info("Successfully published video {} to {}", publication.getId(), publication.getPlatform());
    }

    private void handlePublicationFailure(VideoPublicationDto publication) {
        try {
            // Increment retry count and update status
            if (publication.getRetryCount() < publication.getMaxRetries()) {
                log.info("Scheduling retry for publication {}", publication.getId());
                // Status will remain SCHEDULED for retry
            } else {
                log.warn("Max retries reached for publication {}", publication.getId());
                publishingService.updatePublicationStatus(
                        publication.getId(),
                        "FAILED",
                        null,
                        null);
            }
        } catch (Exception e) {
            log.error("Failed to handle publication failure for {}", publication.getId(), e);
        }
    }
}
