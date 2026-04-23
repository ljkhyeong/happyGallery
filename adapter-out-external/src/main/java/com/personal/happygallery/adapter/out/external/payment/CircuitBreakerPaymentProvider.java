package com.personal.happygallery.adapter.out.external.payment;

import com.personal.happygallery.application.payment.port.out.PaymentConfirmResult;
import com.personal.happygallery.application.payment.port.out.RefundResult;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * 외부 PG 호출 보호용 데코레이터.
 *
 * <p>서킷 브레이커 + 타임아웃을 외부 호출 경계에 적용해
 * 장애 전파(cascading failure)를 줄인다.
 */
@Primary
@Component
public class CircuitBreakerPaymentProvider implements PaymentProvider {

    private static final Logger log = LoggerFactory.getLogger(CircuitBreakerPaymentProvider.class);
    private static final AtomicInteger THREAD_SEQ = new AtomicInteger(0);

    private final PaymentProvider delegate;
    private final CircuitBreaker circuitBreaker;
    private final TimeLimiter timeLimiter;
    private final ExecutorService executor;
    private final long timeoutMillis;

    public CircuitBreakerPaymentProvider(
            @Qualifier("paymentProviderDelegate") PaymentProvider delegate,
            ExternalPaymentProperties properties,
            MeterRegistry meterRegistry
    ) {
        ExternalPaymentProperties.CircuitBreaker cb = properties.circuitBreaker();
        this.delegate = delegate;
        this.timeoutMillis = properties.timeoutMillis();
        this.circuitBreaker = CircuitBreaker.of("paymentProvider", CircuitBreakerConfig.custom()
                .failureRateThreshold(cb.failureRateThreshold())
                .slidingWindowSize(cb.slidingWindowSize())
                .minimumNumberOfCalls(cb.minimumNumberOfCalls())
                .waitDurationInOpenState(Duration.ofSeconds(cb.waitDurationOpenSeconds()))
                .permittedNumberOfCallsInHalfOpenState(cb.permittedCallsInHalfOpenState())
                .build());
        this.timeLimiter = TimeLimiter.of(TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofMillis(this.timeoutMillis))
                .cancelRunningFuture(true)
                .build());
        ExecutorService rawExecutor = Executors.newFixedThreadPool(
                Math.max(2, cb.permittedCallsInHalfOpenState()),
                runnable -> {
                    Thread thread = new Thread(runnable);
                    thread.setName("payment-timeout-" + THREAD_SEQ.incrementAndGet());
                    thread.setDaemon(true);
                    return thread;
                });
        this.executor = ExecutorServiceMetrics.monitor(
                meterRegistry,
                rawExecutor,
                "executor",
                Tags.of("name", "paymentTimeoutExecutor"));
    }

    @Override
    public PaymentConfirmResult confirm(String paymentKey, String orderId, long amount) {
        try {
            return circuitBreaker.executeSupplier(() -> executeConfirmWithTimeout(paymentKey, orderId, amount));
        } catch (CallNotPermittedException e) {
            log.warn("PG 확정 호출 차단 (circuit open) [state={}]", circuitBreaker.getState());
            return PaymentConfirmResult.failure("PG 장애로 결제 확정이 일시 차단되었습니다. 잠시 후 재시도해주세요.");
        } catch (RuntimeException e) {
            if (containsCause(e, TimeoutException.class)) {
                log.warn("PG 확정 호출 타임아웃 [timeoutMs={}]", timeoutMillis);
                return PaymentConfirmResult.failure("PG 응답 지연으로 결제 확정에 실패했습니다.");
            }
            Throwable cause = rootCause(e);
            log.error("PG 확정 호출 예외", cause);
            return PaymentConfirmResult.failure(cause.getMessage() != null ? cause.getMessage() : "PG 호출 중 오류가 발생했습니다.");
        }
    }

    @Override
    public RefundResult refund(String pgRef, long amount) {
        try {
            return circuitBreaker.executeSupplier(() -> executeRefundWithTimeout(pgRef, amount));
        } catch (CallNotPermittedException e) {
            log.warn("PG 환불 호출 차단 (circuit open) [state={}]", circuitBreaker.getState());
            return RefundResult.failure("PG 장애로 환불 처리가 일시 차단되었습니다. 잠시 후 재시도해주세요.");
        } catch (RuntimeException e) {
            if (containsCause(e, TimeoutException.class)) {
                log.warn("PG 환불 호출 타임아웃 [timeoutMs={}]", timeoutMillis);
                return RefundResult.failure("PG 응답 지연으로 환불 처리에 실패했습니다.");
            }
            Throwable cause = rootCause(e);
            log.error("PG 환불 호출 예외", cause);
            return RefundResult.failure(cause.getMessage() != null ? cause.getMessage() : "PG 호출 중 오류가 발생했습니다.");
        }
    }

    private PaymentConfirmResult executeConfirmWithTimeout(String paymentKey, String orderId, long amount) {
        try {
            return timeLimiter.executeFutureSupplier(
                    () -> CompletableFuture.supplyAsync(() -> delegate.confirm(paymentKey, orderId, amount), executor));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private RefundResult executeRefundWithTimeout(String pgRef, long amount) {
        try {
            return timeLimiter.executeFutureSupplier(
                    () -> CompletableFuture.supplyAsync(() -> delegate.refund(pgRef, amount), executor));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private boolean containsCause(Throwable throwable, Class<? extends Throwable> causeType) {
        Throwable current = throwable;
        while (current != null) {
            if (causeType.isInstance(current)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private Throwable rootCause(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }

    @PreDestroy
    void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            executor.shutdownNow();
        }
    }
}
