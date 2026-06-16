package com.czx.wenshu.infrastructure.config;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 启用 Spring @Async 并配置 AI 任务线程池（P5-04+）。
 * Bean 名 "aiTaskExecutor" 供 @Async("aiTaskExecutor") 使用。
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean("aiTaskExecutor")
    public Executor aiTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("ai-task-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

    /** SSE 首字超时定时器（P5-07）。 */
    @Bean("sseTimeoutScheduler")
    public ScheduledExecutorService sseTimeoutScheduler() {
        return Executors.newScheduledThreadPool(4,
                r -> {
                    Thread t = new Thread(r, "sse-timeout");
                    t.setDaemon(true);
                    return t;
                });
    }
}
