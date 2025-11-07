package ai.content.auto.service;

import ai.content.auto.config.VideoResourceConfig;
import ai.content.auto.entity.VideoGenerationJob.JobStatus;
import ai.content.auto.repository.VideoGenerationJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for monitoring and optimizing video generation resource usage.
 * Tracks CPU, memory, disk usage and provides adaptive scaling recommendations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VideoResourceMonitorService {

    private final VideoResourceConfig config;
    private final VideoGenerationJobRepository jobRepository;
    private final OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();

    // Resource usage metrics
    private volatile double currentCpuLoad = 0.0;
    private volatile long currentMemoryUsageMb = 0;
    private volatile long currentDiskUsageGb = 0;
    private volatile int currentActiveJobs = 0;
    private volatile int recommendedConcurrentJobs = 3;

    /**
     * Monitor system resources (scheduled every 30 seconds by default)
     */
    @Scheduled(fixedDelayString = "${app.video.optimization.monitoring-interval-seconds:30}000")
    public void monitorResources() {
        try {
            // Update resource metrics
            updateCpuLoad();
            updateMemoryUsage();
            updateDiskUsage();
            updateActiveJobCount();

            // Calculate system load
            double systemLoad = calculateSystemLoad();

            // Adjust concurrent job limit if adaptive scaling is enabled
            if (config.getOptimization().isEnableAdaptiveScaling()) {
                adjustConcurrentJobLimit(systemLoad);
            }

            // Log resource status
            if (log.isDebugEnabled()) {
                log.debug(
                        "Video resource status - CPU: {:.2f}%, Memory: {}MB, Disk: {}GB, Active Jobs: {}, Recommended Concurrent: {}",
                        currentCpuLoad * 100, currentMemoryUsageMb, currentDiskUsageGb,
                        currentActiveJobs, recommendedConcurrentJobs);
            }

            // Alert if resources are critically high
            if (systemLoad > 0.9) {
                log.warn("Video generation resources critically high - CPU: {:.2f}%, Memory: {}MB, Active Jobs: {}",
                        currentCpuLoad * 100, currentMemoryUsageMb, currentActiveJobs);
            }

        } catch (Exception e) {
            log.error("Error monitoring video resources", e);
        }
    }

    /**
     * Clean up temporary video files (scheduled every hour)
     */
    @Scheduled(fixedDelay = 3600000) // 1 hour
    public void cleanupTempFiles() {
        try {
            String tempPath = config.getResource().getTempStoragePath();
            File tempDir = new File(tempPath);

            if (!tempDir.exists()) {
                return;
            }

            Instant cutoffTime = Instant.now().minus(
                    config.getResource().getCleanupTempFilesHours(), ChronoUnit.HOURS);

            int deletedCount = 0;
            long freedSpaceMb = 0;

            File[] files = tempDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && file.lastModified() < cutoffTime.toEpochMilli()) {
                        long fileSize = file.length();
                        if (file.delete()) {
                            deletedCount++;
                            freedSpaceMb += fileSize / (1024 * 1024);
                        }
                    }
                }
            }

            if (deletedCount > 0) {
                log.info("Cleaned up {} temporary video files, freed {}MB of disk space",
                        deletedCount, freedSpaceMb);
            }

        } catch (Exception e) {
            log.error("Error cleaning up temporary video files", e);
        }
    }

    /**
     * Get current resource usage metrics
     */
    public Map<String, Object> getResourceMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("cpuLoad", currentCpuLoad);
        metrics.put("cpuLoadPercentage", currentCpuLoad * 100);
        metrics.put("memoryUsageMb", currentMemoryUsageMb);
        metrics.put("memoryLimitMb", config.getResource().getMaxMemoryMb());
        metrics.put("memoryUsagePercentage",
                (currentMemoryUsageMb * 100.0) / config.getResource().getMaxMemoryMb());
        metrics.put("diskUsageGb", currentDiskUsageGb);
        metrics.put("diskLimitGb", config.getResource().getMaxTempStorageGb());
        metrics.put("diskUsagePercentage",
                (currentDiskUsageGb * 100.0) / config.getResource().getMaxTempStorageGb());
        metrics.put("activeJobs", currentActiveJobs);
        metrics.put("maxConcurrentJobs", config.getProcessing().getMaxConcurrentJobs());
        metrics.put("recommendedConcurrentJobs", recommendedConcurrentJobs);
        metrics.put("systemLoad", calculateSystemLoad());
        metrics.put("canAcceptMoreJobs", canAcceptMoreJobs());
        return metrics;
    }

    /**
     * Check if system can accept more video jobs
     */
    public boolean canAcceptMoreJobs() {
        // Check if we're at concurrent job limit
        if (currentActiveJobs >= recommendedConcurrentJobs) {
            return false;
        }

        // Check if resources are critically high
        double systemLoad = calculateSystemLoad();
        if (systemLoad > 0.95) {
            log.warn("System load too high ({:.2f}%), rejecting new video jobs", systemLoad * 100);
            return false;
        }

        // Check disk space
        if (currentDiskUsageGb >= config.getResource().getMaxTempStorageGb()) {
            log.warn("Disk space limit reached ({}GB), rejecting new video jobs", currentDiskUsageGb);
            return false;
        }

        return true;
    }

    /**
     * Check if user can submit more jobs
     */
    public boolean canUserSubmitMoreJobs(Long userId) {
        int userActiveJobs = (int) jobRepository.countByUser_IdAndStatus(userId, JobStatus.PROCESSING);
        return userActiveJobs < config.getProcessing().getMaxUserConcurrentJobs();
    }

    /**
     * Get recommended batch size based on current load
     */
    public int getRecommendedBatchSize() {
        double systemLoad = calculateSystemLoad();
        return config.getRecommendedBatchSize(systemLoad);
    }

    // Private helper methods

    private void updateCpuLoad() {
        try {
            currentCpuLoad = osBean.getSystemLoadAverage() / osBean.getAvailableProcessors();
            // Clamp between 0 and 1
            currentCpuLoad = Math.max(0.0, Math.min(1.0, currentCpuLoad));
        } catch (Exception e) {
            log.debug("Could not get CPU load", e);
            currentCpuLoad = 0.5; // Default to 50% if unavailable
        }
    }

    private void updateMemoryUsage() {
        try {
            Runtime runtime = Runtime.getRuntime();
            long usedMemory = runtime.totalMemory() - runtime.freeMemory();
            currentMemoryUsageMb = usedMemory / (1024 * 1024);
        } catch (Exception e) {
            log.debug("Could not get memory usage", e);
        }
    }

    private void updateDiskUsage() {
        try {
            String tempPath = config.getResource().getTempStoragePath();
            File tempDir = new File(tempPath);

            if (tempDir.exists()) {
                long totalSize = 0;
                File[] files = tempDir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.isFile()) {
                            totalSize += file.length();
                        }
                    }
                }
                currentDiskUsageGb = totalSize / (1024 * 1024 * 1024);
            }
        } catch (Exception e) {
            log.debug("Could not get disk usage", e);
        }
    }

    private void updateActiveJobCount() {
        try {
            currentActiveJobs = (int) jobRepository.countByStatus(JobStatus.PROCESSING);
        } catch (Exception e) {
            log.debug("Could not get active job count", e);
        }
    }

    private double calculateSystemLoad() {
        // Calculate weighted average of CPU, memory, and disk usage
        double cpuWeight = 0.5;
        double memoryWeight = 0.3;
        double diskWeight = 0.2;

        double memoryLoad = (double) currentMemoryUsageMb / config.getResource().getMaxMemoryMb();
        double diskLoad = (double) currentDiskUsageGb / config.getResource().getMaxTempStorageGb();

        return (currentCpuLoad * cpuWeight) + (memoryLoad * memoryWeight) + (diskLoad * diskWeight);
    }

    private void adjustConcurrentJobLimit(double systemLoad) {
        int maxConcurrent = config.getProcessing().getMaxConcurrentJobs();
        int minConcurrent = 1;

        // Scale up if load is low
        if (systemLoad < config.getOptimization().getScaleDownThreshold()) {
            recommendedConcurrentJobs = Math.min(maxConcurrent, recommendedConcurrentJobs + 1);
            log.debug("Low system load ({:.2f}%), increasing concurrent jobs to {}",
                    systemLoad * 100, recommendedConcurrentJobs);
        }
        // Scale down if load is high
        else if (systemLoad > config.getOptimization().getScaleUpThreshold()) {
            recommendedConcurrentJobs = Math.max(minConcurrent, recommendedConcurrentJobs - 1);
            log.debug("High system load ({:.2f}%), decreasing concurrent jobs to {}",
                    systemLoad * 100, recommendedConcurrentJobs);
        }
    }
}
