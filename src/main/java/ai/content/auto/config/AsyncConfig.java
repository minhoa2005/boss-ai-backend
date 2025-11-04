package ai.content.auto.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Configuration for asynchronous processing and scheduling
 */
@Configuration
@EnableAsync
@EnableScheduling
@Slf4j
public class AsyncConfig {

    @Value("${app.async.core-pool-size:10}")
    private int corePoolSize;

    @Value("${app.async.max-pool-size:50}")
    private int maxPoolSize;

    @Value("${app.async.queue-capacity:1000}")
    private int queueCapacity;

    @Value("${app.async.keep-alive-seconds:60}")
    private int keepAliveSeconds;

    /**
     * Task executor for asynchronous content processing
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // Core pool size - minimum number of threads
        executor.setCorePoolSize(corePoolSize);

        // Maximum pool size - maximum number of threads
        executor.setMaxPoolSize(maxPoolSize);

        // Queue capacity - number of tasks that can be queued
        executor.setQueueCapacity(queueCapacity);

        // Keep alive time for idle threads
        executor.setKeepAliveSeconds(keepAliveSeconds);

        // Thread name prefix for easier debugging
        executor.setThreadNamePrefix("ContentProcessor-");

        // Rejection policy when queue is full
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // Wait for tasks to complete on shutdown
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);

        // Initialize the executor
        executor.initialize();

        log.info("Configured async task executor - Core: {}, Max: {}, Queue: {}",
                corePoolSize, maxPoolSize, queueCapacity);

        return executor;
    }

    /**
     * Task executor for scheduled tasks (separate from main processing)
     */
    @Bean(name = "scheduledTaskExecutor")
    public Executor scheduledTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // Smaller pool for scheduled tasks
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("ScheduledTask-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(10);

        executor.initialize();

        log.info("Configured scheduled task executor");

        return executor;
    }
}