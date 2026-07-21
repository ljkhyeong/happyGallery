package com.personal.happygallery.adapter.out.external.payment;

import com.personal.happygallery.application.payment.port.out.PaymentConfirmResult;
import com.personal.happygallery.application.payment.port.out.PaymentLookupResult;
import com.personal.happygallery.application.payment.port.out.RefundLookupResult;
import com.personal.happygallery.application.payment.port.out.RefundResult;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics;
import java.time.Duration;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/** 결제 외부 호출을 보호하는 자원과 데코레이터 빈을 구성한다. */
@Configuration(proxyBeanMethods = false)
class PaymentResilienceConfig {

    @Bean
    CircuitBreaker paymentCircuitBreaker(ExternalPaymentProperties properties) {
        ExternalPaymentProperties.CircuitBreaker cb = properties.circuitBreaker();
        return CircuitBreaker.of("paymentProvider", CircuitBreakerConfig.custom()
                .failureRateThreshold(cb.failureRateThreshold())
                .slidingWindowSize(cb.slidingWindowSize())
                .minimumNumberOfCalls(cb.minimumNumberOfCalls())
                .waitDurationInOpenState(Duration.ofSeconds(cb.waitDurationOpenSeconds()))
                .permittedNumberOfCallsInHalfOpenState(cb.permittedCallsInHalfOpenState())
                .recordResult(PaymentResilienceConfig::isFailureResult)
                .build());
    }

    @Bean
    TimeLimiter paymentTimeLimiter(ExternalPaymentProperties properties) {
        return TimeLimiter.of(TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofMillis(properties.timeoutMillis()))
                .cancelRunningFuture(true)
                .build());
    }

    @Bean(destroyMethod = "close")
    PaymentTimeoutExecutor paymentTimeoutExecutor(ExternalPaymentProperties properties,
                                                   MeterRegistry meterRegistry) {
        ExternalPaymentProperties.ThreadPool threadPool = properties.threadPool();
        Counter rejectedCounter = Counter.builder("happygallery.payment.executor.rejected")
                .description("PG timeout executor rejected task count")
                .register(meterRegistry);
        RejectedExecutionHandler abortPolicy = new ThreadPoolExecutor.AbortPolicy();
        RejectedExecutionHandler countingAbortPolicy = (task, rejectedExecutor) -> {
            rejectedCounter.increment();
            abortPolicy.rejectedExecution(task, rejectedExecutor);
        };
        ThreadPoolExecutor rawExecutor = new ThreadPoolExecutor(
                threadPool.poolSize(),
                threadPool.poolSize(),
                0L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(threadPool.queueCapacity()),
                Thread.ofPlatform()
                        .name("payment-timeout-", 1)
                        .daemon(true)
                        .factory(),
                countingAbortPolicy);
        ExecutorService monitoredExecutor = ExecutorServiceMetrics.monitor(
                meterRegistry,
                rawExecutor,
                "paymentTimeoutExecutor");
        return new PaymentTimeoutExecutor(monitoredExecutor);
    }

    @Bean
    @Primary
    ResilientPaymentProvider resilientPaymentProvider(
            @Qualifier("paymentProviderDelegate") PaymentProvider delegate,
            @Qualifier("paymentCircuitBreaker") CircuitBreaker circuitBreaker,
            @Qualifier("paymentTimeLimiter") TimeLimiter timeLimiter,
            @Qualifier("paymentTimeoutExecutor") PaymentTimeoutExecutor executor,
            ExternalPaymentProperties properties
    ) {
        return new ResilientPaymentProvider(
                delegate,
                circuitBreaker,
                timeLimiter,
                executor,
                properties.timeoutMillis());
    }

    private static boolean isFailureResult(Object result) {
        if (result instanceof PaymentConfirmResult confirmResult) {
            return confirmResult.retryable();
        }
        if (result instanceof RefundResult refundResult) {
            return refundResult.retryable() || refundResult.reconciliationRequired();
        }
        if (result instanceof PaymentLookupResult lookupResult) {
            return lookupResult.status() == PaymentLookupResult.Status.UNAVAILABLE;
        }
        if (result instanceof RefundLookupResult lookupResult) {
            return lookupResult.status() == RefundLookupResult.Status.UNAVAILABLE;
        }
        return false;
    }
}
