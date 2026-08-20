package com.personal.happygallery.adapter.out.external.resilience;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.task.ThreadPoolTaskExecutorBuilder;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

@Component
public final class BoundedExecutorFactory {

    /*
     * executor.* standard meters are intentionally owned by Spring Boot's task executor binder.
     * This factory only owns the application-specific rejection counters.
     */

    private static final Duration SHUTDOWN_TIMEOUT = Duration.ofSeconds(2);

    private final ThreadPoolTaskExecutorBuilder builder;
    private final MeterRegistry meterRegistry;
    private final TaskDecorator taskDecorator;

    public BoundedExecutorFactory(
            ThreadPoolTaskExecutorBuilder builder,
            MeterRegistry meterRegistry,
            @Qualifier("asyncContextTaskDecorator") TaskDecorator taskDecorator) {
        this.builder = builder;
        this.meterRegistry = meterRegistry;
        this.taskDecorator = taskDecorator;
    }

    public ThreadPoolTaskExecutor create(int poolSize,
                                         int queueCapacity,
                                         String threadNamePrefix,
                                         String rejectedMetricName,
                                         String rejectedMetricDescription) {
        Counter rejectedCounter = Counter.builder(rejectedMetricName)
                .description(rejectedMetricDescription)
                .register(meterRegistry);
        RejectedExecutionHandler abortPolicy = new ThreadPoolExecutor.AbortPolicy();
        RejectedExecutionHandler countingAbortPolicy = (task, executor) -> {
            rejectedCounter.increment();
            abortPolicy.rejectedExecution(task, executor);
        };

        BoundedThreadPoolTaskExecutor executor = builder
                .corePoolSize(poolSize)
                .maxPoolSize(poolSize)
                .queueCapacity(queueCapacity)
                .threadNamePrefix(threadNamePrefix)
                .taskDecorator(taskDecorator)
                .awaitTermination(true)
                .awaitTerminationPeriod(SHUTDOWN_TIMEOUT)
                .additionalCustomizers(taskExecutor -> {
                    taskExecutor.setDaemon(true);
                    taskExecutor.setRejectedExecutionHandler(countingAbortPolicy);
                })
                .build(BoundedThreadPoolTaskExecutor.class);
        return executor;
    }

    public static final class BoundedThreadPoolTaskExecutor extends ThreadPoolTaskExecutor {

        @Override
        protected BlockingQueue<Runnable> createQueue(int queueCapacity) {
            return new ArrayBlockingQueue<>(queueCapacity);
        }

        @Override
        public void shutdown() {
            super.shutdown();
            ThreadPoolExecutor executor = getThreadPoolExecutor();
            if (!executor.isTerminated()) {
                executor.shutdownNow();
            }
        }
    }
}
