package com.personal.happygallery.adapter.out.external.resilience;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.micrometer.metrics.autoconfigure.task.TaskExecutorMetricsAutoConfiguration;
import org.springframework.boot.task.ThreadPoolTaskExecutorBuilder;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@ExtendWith(OutputCaptureExtension.class)
class BoundedExecutorMetricsTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(TaskExecutorMetricsAutoConfiguration.class))
            .withUserConfiguration(ExecutorMetricsTestConfig.class);

    @DisplayName("Spring Boot가 executor 빈 이름으로 표준 지표를 한 번만 등록한다")
    @Test
    void bootMetricsBinder_registersExecutorMetricsOnce(CapturedOutput output) {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            MeterRegistry meterRegistry = context.getBean(MeterRegistry.class);
            ThreadPoolTaskExecutor executor = context.getBean(
                    "paymentTimeoutExecutor", ThreadPoolTaskExecutor.class);
            CountDownLatch completed = new CountDownLatch(1);

            executor.execute(completed::countDown);

            assertThat(completed.await(1, TimeUnit.SECONDS)).isTrue();
            await().atMost(1, TimeUnit.SECONDS).untilAsserted(() ->
                    assertThat(meterRegistry.get("executor.completed")
                            .tag("name", "paymentTimeoutExecutor")
                            .functionCounter()
                            .count()).isEqualTo(1));
            assertThat(meterRegistry.get("executor.queued")
                    .tag("name", "paymentTimeoutExecutor")
                    .gauge()).isNotNull();
            assertThat(meterRegistry.get("executor.queue.remaining")
                    .tag("name", "paymentTimeoutExecutor")
                    .gauge()).isNotNull();
            assertThat(meterRegistry.get("happygallery.payment.executor.rejected")
                    .counter()).isNotNull();
            assertThat(output).doesNotContain("already registered");
        });
    }

    @Configuration(proxyBeanMethods = false)
    static class ExecutorMetricsTestConfig {

        @Bean
        SimpleMeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }

        @Bean
        ThreadPoolTaskExecutorBuilder threadPoolTaskExecutorBuilder() {
            return new ThreadPoolTaskExecutorBuilder();
        }

        @Bean("asyncContextTaskDecorator")
        TaskDecorator taskDecorator() {
            return task -> task;
        }

        @Bean
        BoundedExecutorFactory boundedExecutorFactory(
                ThreadPoolTaskExecutorBuilder builder,
                MeterRegistry meterRegistry,
                TaskDecorator taskDecorator
        ) {
            return new BoundedExecutorFactory(builder, meterRegistry, taskDecorator);
        }

        @Bean
        ThreadPoolTaskExecutor paymentTimeoutExecutor(BoundedExecutorFactory executorFactory) {
            return executorFactory.create(
                    1,
                    1,
                    "payment-timeout-test-",
                    "happygallery.payment.executor.rejected",
                    "PG timeout executor rejected task count");
        }
    }
}
