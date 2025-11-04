package ai.content.auto.service.monitoring;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * Database backup monitoring status
 */
@Data
@Builder
public class BackupMonitoringStatus {
    private Instant timestamp;
    private Instant lastBackupTime;
    private String backupStatus; // SUCCESS, FAILED, IN_PROGRESS
    private long backupSize;
    private String backupLocation;
    private int retentionDays;
    private Instant nextScheduledBackup;
    private boolean isHealthy;
    private String errorMessage;
}