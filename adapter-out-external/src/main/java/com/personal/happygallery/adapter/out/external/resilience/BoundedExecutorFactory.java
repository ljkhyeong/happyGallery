package com.personal.happygallery.adapter.out.external.resilience;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

@Component
public final class BoundedExecutorFactory {

    private final MeterRegistry meterRegistry;

    public BoundedExecutorFactory(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public ExecutorService create(int poolSize,
                                  int queueCapacity,
                                  String threadNamePrefix,
                                  String monitorName,
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
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                poolSize,
                poolSize,
                0L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(queueCapacity),
                Thread.ofPlatform()
                        .name(threadNamePrefix, 1)
                        .daemon(true)
                        .factory(),
                countingAbortPolicy);
        return ExecutorServiceMetrics.monitor(meterRegistry, executor, monitorName);
    }
}
