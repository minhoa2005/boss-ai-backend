package ai.content.auto.service.monitoring;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

/**
 * System alert information
 */
@Data
@Builder
public class SystemAlert {

    private SystemMonitoringService.AlertType alertType;
    private String message;
    private SystemMonitoringService.AlertSeverity severity;
    private Instant timestamp;

    // Alert status
    private boolean resolved;
    private Instant resolvedAt;
    private String resolvedBy;
    private String resolutionNotes;

    // Alert context
    private Map<String, Object> context;
    private String source; // Component that generated the alert

    // Alert handling
    private boolean acknowledged;
    private Instant acknowledgedAt;
    private String acknowledgedBy;

    // Escalation
    private int escalationLevel;
    private boolean escalated;
    private Instant escalatedAt;
}