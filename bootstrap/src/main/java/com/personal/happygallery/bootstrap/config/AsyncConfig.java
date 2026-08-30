package com.personal.happygallery.bootstrap.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.sentry.spring7.SentryTaskDecorator;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.RejectedExecutionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.task.ThreadPoolTaskExecutorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.core.task.support.CompositeTaskDecorator;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    private static final Logger log = LoggerFactory.getLogger(AsyncConfig.class);

    @Bean(name = "asyncContextTaskDecorator", defaultCandidate = false)
    public TaskDecorator asyncContextTaskDecorator() {
        return new CompositeTaskDecorator(List.of(
                new SentryTaskDecorator(),
                mdcTaskDecorator()));
    }

    @Bean
    public ThreadPoolTaskExecutor notificationExecutor(
            ThreadPoolTaskExecutorBuilder builder,
            @Qualifier("asyncContextTaskDecorator") TaskDecorator taskDecorator,
            MeterRegistry meterRegistry) {
        return buildExecutor(builder, taskDecorator, meterRegistry, "notification", "notify-");
    }

    @Bean
    public ThreadPoolTaskExecutor refundExecutor(
            ThreadPoolTaskExecutorBuilder builder,
            @Qualifier("asyncContextTaskDecorator") TaskDecorator taskDecorator,
            MeterRegistry meterRegistry) {
        return buildExecutor(builder, taskDecorator, meterRegistry, "refund", "refund-");
    }

    private ThreadPoolTaskExecutor buildExecutor(
            ThreadPoolTaskExecutorBuilder builder,
            TaskDecorator taskDecorator,
            MeterRegistry meterRegistry,
            String executorName,
            String threadNamePrefix) {
        return builder
                .threadNamePrefix(threadNamePrefix)
                .taskDecorator(taskDecorator)
                .additionalCustomizers(executor -> executor.setRejectedExecutionHandler(
                        durableSignalRejectionHandler(meterRegistry, executorName)))
                .build();
    }

    private RejectedExecutionHandler durableSignalRejectionHandler(
            MeterRegistry meterRegistry,
            String executorName) {
        Counter rejected = Counter.builder("happygallery.async.executor.rejected")
                .description("커밋 후 후속 처리 실행 신호가 executor 포화 또는 종료로 거절된 횟수")
                .tag("executor", executorName)
                .register(meterRegistry);
        return (task, executor) -> {
            rejected.increment();
            log.warn("[Async] 후속 처리 실행 신호 거절 "
                            + "[executor={} active={} queued={} shutdown={}]",
                    executorName,
                    executor.getActiveCount(),
                    executor.getQueue().size(),
                    executor.isShutdown());
        };
    }

    private TaskDecorator mdcTaskDecorator() {
        return runnable -> {
            Map<String, String> callerContext = MDC.getCopyOfContextMap();
            return () -> {
                Map<String, String> workerContext = MDC.getCopyOfContextMap();
                if (callerContext == null) {
                    MDC.clear();
                } else {
                    MDC.setContextMap(callerContext);
                }
                try {
                    runnable.run();
                } finally {
                    if (workerContext == null) {
                        MDC.clear();
                    } else {
                        MDC.setContextMap(workerContext);
                    }
                }
            };
        };
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (Throwable ex, Method method, Object... params) ->
                log.error("[Async] 비동기 작업 실패 — {}.{}() [type={}]",
                        method.getDeclaringClass().getSimpleName(), method.getName(),
                        ex.getClass().getSimpleName(), ex);
    }
}
