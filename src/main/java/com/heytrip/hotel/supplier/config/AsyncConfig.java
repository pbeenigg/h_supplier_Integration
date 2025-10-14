package com.heytrip.hotel.supplier.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步线程池配置
 * 为API日志记录提供专用的异步线程池
 *
 * @author Pax
 * @since 1.0.0
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Value("${app.async.api-log.core-pool-size:5}")
    private int corePoolSize;

    @Value("${app.async.api-log.max-pool-size:10}")
    private int maxPoolSize;

    @Value("${app.async.api-log.queue-capacity:1000}")
    private int queueCapacity;

    @Value("${app.async.api-log.keep-alive-seconds:60}")
    private int keepAliveSeconds;

    /**
     * API日志专用线程池
     * 用于异步记录API调用日志，避免影响接口性能
     */
    @Bean("apiLogExecutor")
    public Executor apiLogExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 核心线程数
        executor.setCorePoolSize(corePoolSize);

        // 最大线程数
        executor.setMaxPoolSize(maxPoolSize);

        // 队列容量
        executor.setQueueCapacity(queueCapacity);

        // 线程空闲时间
        executor.setKeepAliveSeconds(keepAliveSeconds);

        // 线程名称前缀
        executor.setThreadNamePrefix("api-log-");

        // 拒绝策略：调用者运行策略，确保日志不丢失
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // 允许核心线程超时
        executor.setAllowCoreThreadTimeOut(true);

        // 等待任务完成后再关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);

        // 等待时间
        executor.setAwaitTerminationSeconds(30);

        executor.initialize();
        return executor;
    }
}
