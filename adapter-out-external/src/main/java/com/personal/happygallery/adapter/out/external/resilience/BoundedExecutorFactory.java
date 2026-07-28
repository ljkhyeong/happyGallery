package com.personal.happygallery.adapter.out.external.resilience;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskDecorator;
import org.springframework.stereotype.Component;

@Component
public final class BoundedExecutorFactory {

    private final MeterRegistry meterRegistry;
    private final TaskDecorator taskDecorator;

    public BoundedExecutorFactory(
            MeterRegistry meterRegistry,
            @Qualifier("asyncContextTaskDecorator") TaskDecorator taskDecorator) {
        this.meterRegistry = meterRegistry;
        this.taskDecorator = taskDecorator;
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
        ThreadPoolExecutor executor = new ContextDecoratingThreadPoolExecutor(
                poolSize,
                poolSize,
                0L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(queueCapacity),
                Thread.ofPlatform()
                        .name(threadNamePrefix, 1)
                        .daemon(true)
                        .factory(),
                countingAbortPolicy,
                taskDecorator);
        return ExecutorServiceMetrics.monitor(meterRegistry, executor, monitorName);
    }

    private static final class ContextDecoratingThreadPoolExecutor extends ThreadPoolExecutor {

        private final TaskDecorator taskDecorator;

        private ContextDecoratingThreadPoolExecutor(
                int corePoolSize,
                int maximumPoolSize,
                long keepAliveTime,
                TimeUnit unit,
                BlockingQueue<Runnable> workQueue,
                ThreadFactory threadFactory,
                RejectedExecutionHandler handler,
                TaskDecorator taskDecorator) {
            super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
            this.taskDecorator = taskDecorator;
        }

        @Override
        public void execute(Runnable command) {
            super.execute(taskDecorator.decorate(command));
        }
    }
}
