package com.personal.happygallery.adapter.out.external.payment;

import com.personal.happygallery.adapter.out.external.resilience.BoundedExecutorFactory;
import com.personal.happygallery.application.payment.port.out.PaymentConfirmResult;
import com.personal.happygallery.application.payment.port.out.PaymentLookupResult;
import com.personal.happygallery.application.payment.port.out.RefundLookupResult;
import com.personal.happygallery.application.payment.port.out.RefundResult;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import java.time.Duration;
import java.util.concurrent.Executor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.Assert;

/** 결제 외부 호출을 보호하는 자원과 데코레이터 빈을 구성한다. */
@Configuration(proxyBeanMethods = false)
class PaymentResilienceConfig {

    @Bean
    CircuitBreaker paymentCircuitBreaker(ExternalPaymentProperties properties,
                                         CircuitBreakerRegistry circuitBreakerRegistry) {
        ExternalPaymentProperties.CircuitBreaker cb = properties.circuitBreaker();
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(cb.failureRateThreshold())
                .slidingWindowSize(cb.slidingWindowSize())
                .minimumNumberOfCalls(cb.minimumNumberOfCalls())
                .waitDurationInOpenState(Duration.ofSeconds(cb.waitDurationOpenSeconds()))
                .permittedNumberOfCallsInHalfOpenState(cb.permittedCallsInHalfOpenState())
                .recordResult(PaymentResilienceConfig::isFailureResult)
                .build();
        return circuitBreakerRegistry.circuitBreaker("paymentProvider", circuitBreakerConfig);
    }

    @Bean
    TimeLimiter paymentTimeLimiter(ExternalPaymentProperties properties,
                                   TossPaymentsProperties tossProperties) {
        validateTimeoutHierarchy(properties, tossProperties);
        return TimeLimiter.of(TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofMillis(properties.timeoutMillis()))
                .cancelRunningFuture(true)
                .build());
    }

    @Bean
    ThreadPoolTaskExecutor paymentTimeoutExecutor(ExternalPaymentProperties properties,
                                                  BoundedExecutorFactory executorFactory) {
        ExternalPaymentProperties.ThreadPool threadPool = properties.threadPool();
        return executorFactory.create(
                threadPool.poolSize(),
                threadPool.queueCapacity(),
                "payment-timeout-",
                "happygallery.payment.executor.rejected",
                "PG timeout executor rejected task count");
    }

    @Bean
    @Primary
    ResilientPaymentProvider resilientPaymentProvider(
            @Qualifier("paymentProviderDelegate") PaymentProvider delegate,
            @Qualifier("paymentCircuitBreaker") CircuitBreaker circuitBreaker,
            @Qualifier("paymentTimeLimiter") TimeLimiter timeLimiter,
            @Qualifier("paymentTimeoutExecutor") Executor executor,
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
            return confirmResult.retryable() || confirmResult.reconciliationRequired();
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

    private static void validateTimeoutHierarchy(ExternalPaymentProperties resilience,
                                                 TossPaymentsProperties transport) {
        long transportBudgetMillis = Math.addExact(
                Math.addExact(transport.acquireTimeoutMillis(), transport.connectTimeoutMillis()),
                transport.timeoutMillis());
        Assert.isTrue(resilience.timeoutMillis() > transportBudgetMillis,
                "결제 TimeLimiter는 Toss acquire + connect + response timeout 합보다 커야 합니다.");
    }
}
