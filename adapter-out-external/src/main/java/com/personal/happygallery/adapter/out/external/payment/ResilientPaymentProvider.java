package com.personal.happygallery.adapter.out.external.payment;

import com.personal.happygallery.application.payment.port.out.PaymentConfirmResult;
import com.personal.happygallery.application.payment.port.out.RefundResult;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
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
public class ResilientPaymentProvider implements PaymentProvider {

    private static final Logger log = LoggerFactory.getLogger(ResilientPaymentProvider.class);
    private static final AtomicInteger THREAD_SEQ = new AtomicInteger(0);

    private final PaymentProvider delegate;
    private final CircuitBreaker circuitBreaker;
    private final TimeLimiter timeLimiter;
    private final ExecutorService executor;
    private final long timeoutMillis;

    public ResilientPaymentProvider(
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
                .recordResult(ResilientPaymentProvider::isFailureResult)
                .build());
        this.timeLimiter = TimeLimiter.of(TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofMillis(this.timeoutMillis))
                .cancelRunningFuture(true)
                .build());
        ExternalPaymentProperties.ThreadPool threadPool = properties.threadPool();
        Counter rejectedCounter = Counter.builder("happygallery.payment.executor.rejected")
                .description("PG timeout executor rejected task count")
                .register(meterRegistry);
        RejectedExecutionHandler abortPolicy = new ThreadPoolExecutor.AbortPolicy();
        RejectedExecutionHandler rejectionHandler = (task, executor) -> {
            rejectedCounter.increment();
            abortPolicy.rejectedExecution(task, executor);
        };
        ThreadPoolExecutor rawExecutor = new ThreadPoolExecutor(
                threadPool.poolSize(),
                threadPool.poolSize(),
                0L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(threadPool.queueCapacity()),
                runnable -> {
                    Thread thread = new Thread(runnable);
                    thread.setName("payment-timeout-" + THREAD_SEQ.incrementAndGet());
                    thread.setDaemon(true);
                    return thread;
                },
                rejectionHandler);
        this.executor = ExecutorServiceMetrics.monitor(
                meterRegistry,
                rawExecutor,
                "paymentTimeoutExecutor");
    }

    @Override
    public PaymentConfirmResult confirm(String paymentKey, String orderId, long amount, String idempotencyKey) {
        try {
            return circuitBreaker.executeCallable(
                    () -> executeConfirmWithTimeout(paymentKey, orderId, amount, idempotencyKey));
        } catch (CallNotPermittedException e) {
            log.warn("PG 확정 호출 차단 (circuit open) [state={}]", circuitBreaker.getState());
            return PaymentConfirmResult.retryableFailure(
                    "PG 장애로 결제 확정이 일시 차단되었습니다. 잠시 후 재시도해주세요.");
        } catch (TimeoutException e) {
            log.warn("PG 확정 호출 타임아웃 [timeoutMs={}]", timeoutMillis);
            return PaymentConfirmResult.retryableFailure("PG 응답 지연으로 결제 확정에 실패했습니다.");
        } catch (Exception e) {
            Throwable cause = rootCause(e);
            if (cause instanceof TimeoutException) {
                log.warn("PG 확정 호출 타임아웃 [timeoutMs={}]", timeoutMillis);
                return PaymentConfirmResult.retryableFailure("PG 응답 지연으로 결제 확정에 실패했습니다.");
            }
            if (cause instanceof RejectedExecutionException) {
                log.warn("PG 확정 호출 대기열 포화");
                return PaymentConfirmResult.retryableFailure("PG 호출 대기열이 가득 차 결제 확정을 재시도해야 합니다.");
            }
            log.error("PG 확정 호출 예외", cause);
            return PaymentConfirmResult.retryableFailure(
                    cause.getMessage() != null ? cause.getMessage() : "PG 호출 중 오류가 발생했습니다.");
        }
    }

    @Override
    public RefundResult refund(String paymentKey, long amount, String idempotencyKey) {
        try {
            return circuitBreaker.executeCallable(
                    () -> executeRefundWithTimeout(paymentKey, amount, idempotencyKey));
        } catch (CallNotPermittedException e) {
            log.warn("PG 환불 호출 차단 (circuit open) [state={}]", circuitBreaker.getState());
            return RefundResult.retryableFailure("PG 장애로 환불 처리가 일시 차단되었습니다. 잠시 후 재시도해주세요.");
        } catch (TimeoutException e) {
            log.warn("PG 환불 호출 타임아웃 [timeoutMs={}]", timeoutMillis);
            return RefundResult.reconciliationRequired("PG 응답 지연으로 환불 상태 확인이 필요합니다.");
        } catch (Exception e) {
            Throwable cause = rootCause(e);
            if (cause instanceof TimeoutException) {
                log.warn("PG 환불 호출 타임아웃 [timeoutMs={}]", timeoutMillis);
                return RefundResult.reconciliationRequired("PG 응답 지연으로 환불 상태 확인이 필요합니다.");
            }
            if (cause instanceof RejectedExecutionException) {
                log.warn("PG 환불 호출 대기열 포화");
                return RefundResult.retryableFailure("PG 호출 대기열이 가득 차 환불을 재시도해야 합니다.");
            }
            log.error("PG 환불 호출 예외", cause);
            return RefundResult.reconciliationRequired(
                    cause.getMessage() != null ? cause.getMessage() : "PG 호출 결과를 확인할 수 없습니다.");
        }
    }

    private PaymentConfirmResult executeConfirmWithTimeout(String paymentKey, String orderId, long amount,
                                                           String idempotencyKey) throws Exception {
        return timeLimiter.executeFutureSupplier(
                () -> CompletableFuture.supplyAsync(
                        () -> delegate.confirm(paymentKey, orderId, amount, idempotencyKey), executor));
    }

    private RefundResult executeRefundWithTimeout(String paymentKey, long amount, String idempotencyKey) throws Exception {
        return timeLimiter.executeFutureSupplier(
                () -> CompletableFuture.supplyAsync(
                        () -> delegate.refund(paymentKey, amount, idempotencyKey), executor));
    }

    private Throwable rootCause(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }

    private static boolean isFailureResult(Object result) {
        if (result instanceof PaymentConfirmResult confirmResult) {
            return confirmResult.retryable();
        }
        if (result instanceof RefundResult refundResult) {
            return refundResult.retryable() || refundResult.reconciliationRequired();
        }
        return false;
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
