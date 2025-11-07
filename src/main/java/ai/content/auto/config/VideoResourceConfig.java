package ai.content.auto.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for video generation resource management.
 * Provides centralized configuration for resource optimization, queue
 * management,
 * and processing limits.
 */
@Configuration
@ConfigurationProperties(prefix = "app.video")
@Data
public class VideoResourceConfig {

    private QueueConfig queue = new QueueConfig();
    private ProcessingConfig processing = new ProcessingConfig();
    private ResourceConfig resource = new ResourceConfig();
    private OptimizationConfig optimization = new OptimizationConfig();

    @Data
    public static class QueueConfig {
        /**
         * Number of jobs to process in each batch cycle
         */
        private int processingBatchSize = 5;

        /**
         * Delay in minutes before retrying a failed job
         */
        private int retryDelayMinutes = 10;

        /**
         * Maximum time in minutes a job can be in processing state before being marked
         * as stale
         */
        private int maxProcessingTimeMinutes = 30;

        /**
         * Number of days to keep completed jobs before cleanup
         */
        private int cleanupDays = 7;
    }

    @Data
    public static class ProcessingConfig {
        /**
         * Maximum number of concurrent video generation jobs across all users
         */
        private int maxConcurrentJobs = 3;

        /**
         * Maximum number of concurrent jobs per user
         */
        private int maxUserConcurrentJobs = 2;

        /**
         * Size of the thread pool for async video processing
         */
        private int threadPoolSize = 5;

        /**
         * Capacity of the processing queue
         */
        private int queueCapacity = 100;
    }

    @Data
    public static class ResourceConfig {
        /**
         * Maximum memory allocation in MB for video processing
         */
        private int maxMemoryMb = 2048;

        /**
         * Maximum CPU cores to use for video processing
         */
        private int maxCpuCores = 2;

        /**
         * Path for temporary video file storage
         */
        private String tempStoragePath = "/tmp/video-generation";

        /**
         * Maximum temporary storage in GB
         */
        private int maxTempStorageGb = 10;

        /**
         * Hours to keep temporary files before cleanup
         */
        private int cleanupTempFilesHours = 24;
    }

    @Data
    public static class OptimizationConfig {
        /**
         * Enable adaptive scaling based on system load
         */
        private boolean enableAdaptiveScaling = true;

        /**
         * CPU/Memory threshold (0.0-1.0) to trigger scale-up
         */
        private double scaleUpThreshold = 0.8;

        /**
         * CPU/Memory threshold (0.0-1.0) to trigger scale-down
         */
        private double scaleDownThreshold = 0.3;

        /**
         * Interval in seconds for monitoring resource usage
         */
        private int monitoringIntervalSeconds = 30;
    }

    /**
     * Get current processing capacity based on configuration
     */
    public int getCurrentProcessingCapacity() {
        return processing.getMaxConcurrentJobs();
    }

    /**
     * Check if system can accept more jobs
     */
    public boolean canAcceptMoreJobs(int currentActiveJobs) {
        return currentActiveJobs < processing.getMaxConcurrentJobs();
    }

    /**
     * Check if user can submit more jobs
     */
    public boolean canUserSubmitMoreJobs(int userActiveJobs) {
        return userActiveJobs < processing.getMaxUserConcurrentJobs();
    }

    /**
     * Calculate recommended batch size based on current load
     */
    public int getRecommendedBatchSize(double currentLoad) {
        if (!optimization.isEnableAdaptiveScaling()) {
            return queue.getProcessingBatchSize();
        }

        // Reduce batch size under high load
        if (currentLoad > optimization.getScaleUpThreshold()) {
            return Math.max(1, queue.getProcessingBatchSize() / 2);
        }

        // Increase batch size under low load
        if (currentLoad < optimization.getScaleDownThreshold()) {
            return queue.getProcessingBatchSize() * 2;
        }

        return queue.getProcessingBatchSize();
    }
}
