package com.personal.happygallery.adapter.out.external.payment;

import com.personal.happygallery.application.payment.port.out.PaymentConfirmResult;
import com.personal.happygallery.application.payment.port.out.PaymentLookupResult;
import com.personal.happygallery.application.payment.port.out.PaymentPort;
import com.personal.happygallery.application.payment.port.out.RefundLookupResult;
import com.personal.happygallery.application.payment.port.out.RefundResult;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.timelimiter.TimeLimiter;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.NestedExceptionUtils;

/**
 * 외부 PG 호출 보호용 데코레이터.
 *
 * <p>서킷 브레이커 + 타임아웃을 외부 호출 경계에 적용해
 * 장애 전파(cascading failure)를 줄인다.
 */
public class ResilientPaymentProvider implements PaymentPort {

    private static final Logger log = LoggerFactory.getLogger(ResilientPaymentProvider.class);

    private final PaymentPort delegate;
    private final CircuitBreaker circuitBreaker;
    private final TimeLimiter timeLimiter;
    private final Executor executor;
    private final Duration timeout;

    ResilientPaymentProvider(PaymentPort delegate,
                             CircuitBreaker circuitBreaker,
                             TimeLimiter timeLimiter,
                             Executor executor,
                             Duration timeout) {
        this.delegate = delegate;
        this.circuitBreaker = circuitBreaker;
        this.timeLimiter = timeLimiter;
        this.executor = executor;
        this.timeout = timeout;
    }

    @Override
    public PaymentConfirmResult confirm(String paymentKey, String orderId, long amount, String idempotencyKey) {
        try {
            return circuitBreaker.executeCallable(
                    () -> executeWithTimeout(
                            () -> delegate.confirm(paymentKey, orderId, amount, idempotencyKey)));
        } catch (CallNotPermittedException e) {
            log.warn("PG 확정 호출 차단 (circuit open) [state={}]", circuitBreaker.getState());
            return PaymentConfirmResult.retryableFailure(
                    "PG 장애로 결제 확정이 일시 차단되었습니다. 잠시 후 재시도해주세요.");
        } catch (Exception e) {
            Throwable cause = NestedExceptionUtils.getMostSpecificCause(e);
            if (cause instanceof TimeoutException) {
                log.warn("PG 확정 호출 타임아웃 [timeoutMs={}]", timeout.toMillis());
                return PaymentConfirmResult.retryableFailure("PG 응답 지연으로 결제 확정에 실패했습니다.");
            }
            if (cause instanceof RejectedExecutionException) {
                log.warn("PG 확정 호출 대기열 포화");
                return PaymentConfirmResult.retryableFailure("PG 호출 대기열이 가득 차 결제 확정을 재시도해야 합니다.");
            }
            log.error("PG 확정 호출 예외 [orderId={} type={}]",
                    orderId, cause.getClass().getSimpleName());
            return PaymentConfirmResult.retryableFailure("PG 호출 중 오류가 발생했습니다.");
        }
    }

    @Override
    public RefundResult refund(String paymentKey, long amount, String idempotencyKey) {
        try {
            return circuitBreaker.executeCallable(
                    () -> executeWithTimeout(
                            () -> delegate.refund(paymentKey, amount, idempotencyKey)));
        } catch (CallNotPermittedException e) {
            log.warn("PG 환불 호출 차단 (circuit open) [state={}]", circuitBreaker.getState());
            return RefundResult.retryableFailure("PG 장애로 환불 처리가 일시 차단되었습니다. 잠시 후 재시도해주세요.");
        } catch (Exception e) {
            Throwable cause = NestedExceptionUtils.getMostSpecificCause(e);
            if (cause instanceof TimeoutException) {
                log.warn("PG 환불 호출 타임아웃 [timeoutMs={}]", timeout.toMillis());
                return RefundResult.reconciliationRequired("PG 응답 지연으로 환불 상태 확인이 필요합니다.");
            }
            if (cause instanceof RejectedExecutionException) {
                log.warn("PG 환불 호출 대기열 포화");
                return RefundResult.retryableFailure("PG 호출 대기열이 가득 차 환불을 재시도해야 합니다.");
            }
            log.error("PG 환불 호출 예외 [type={}]", cause.getClass().getSimpleName());
            return RefundResult.reconciliationRequired("PG 호출 결과를 확인할 수 없습니다.");
        }
    }

    @Override
    public PaymentLookupResult lookupByOrderId(String orderId) {
        try {
            return circuitBreaker.executeCallable(
                    () -> executeWithTimeout(() -> delegate.lookupByOrderId(orderId)));
        } catch (CallNotPermittedException e) {
            log.warn("PG 조회 호출 차단 (circuit open) [state={}]", circuitBreaker.getState());
            return PaymentLookupResult.unavailable(orderId, "PG 장애로 결제 조회가 일시 차단되었습니다.");
        } catch (Exception e) {
            Throwable cause = NestedExceptionUtils.getMostSpecificCause(e);
            if (cause instanceof TimeoutException) {
                log.warn("PG 조회 호출 타임아웃 [timeoutMs={}]", timeout.toMillis());
            } else if (cause instanceof RejectedExecutionException) {
                log.warn("PG 조회 호출 대기열 포화");
            } else {
                log.error("PG 조회 호출 예외 [orderId={} type={}]",
                        orderId, cause.getClass().getSimpleName());
            }
            return PaymentLookupResult.unavailable(orderId, "PG 결제 조회 결과를 확인할 수 없습니다.");
        }
    }

    @Override
    public RefundLookupResult lookupRefund(String paymentKey, long amount, String idempotencyKey) {
        try {
            return circuitBreaker.executeCallable(
                    () -> executeWithTimeout(
                            () -> delegate.lookupRefund(paymentKey, amount, idempotencyKey)));
        } catch (CallNotPermittedException e) {
            log.warn("PG 환불 조회 호출 차단 (circuit open) [state={}]", circuitBreaker.getState());
            return RefundLookupResult.unavailable(paymentKey, "PG 장애로 환불 조회가 일시 차단되었습니다.");
        } catch (Exception e) {
            Throwable cause = NestedExceptionUtils.getMostSpecificCause(e);
            if (cause instanceof TimeoutException) {
                log.warn("PG 환불 조회 호출 타임아웃 [timeoutMs={}]", timeout.toMillis());
            } else if (cause instanceof RejectedExecutionException) {
                log.warn("PG 환불 조회 호출 대기열 포화");
            } else {
                log.error("PG 환불 조회 호출 예외 [type={}]",
                        cause.getClass().getSimpleName());
            }
            return RefundLookupResult.unavailable(paymentKey, "PG 환불 조회 결과를 확인할 수 없습니다.");
        }
    }

    private <T> T executeWithTimeout(Supplier<T> operation) throws Exception {
        return timeLimiter.executeFutureSupplier(
                () -> CompletableFuture.supplyAsync(operation, executor));
    }
}
