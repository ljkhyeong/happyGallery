package com.personal.happygallery.adapter.out.external.resilience;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.timelimiter.TimeLimiter;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.NestedExceptionUtils;

/**
 * 외부 호출에 서킷 브레이커·타임아웃·제한 큐 executor를 적용하고 실패를 종류별로 나눈다.
 *
 * <p>차단·대기열 거절·타임아웃은 여기서 경고로 남긴다. 예상하지 못한 예외는 호출부가
 * 업무 문맥에 맞는 수준으로 기록하도록 {@link Failure#ERROR}와 원인 예외를 넘긴다.
 */
public final class ResilientExternalCall {

    private static final Logger log = LoggerFactory.getLogger(ResilientExternalCall.class);

    public enum Failure {
        /** 서킷이 열려 호출 전에 차단했다. */
        CIRCUIT_OPEN,
        /** timeout executor 대기열이 가득 차 호출 전에 거절했다. */
        QUEUE_REJECTED,
        /** 호출을 시작했지만 제한 시간 안에 결과를 받지 못했다. */
        TIMEOUT,
        /** 호출 중 예상하지 못한 예외가 발생했다. */
        ERROR
    }

    private final CircuitBreaker circuitBreaker;
    private final TimeLimiter timeLimiter;
    private final Executor executor;
    private final Duration timeout;

    public ResilientExternalCall(CircuitBreaker circuitBreaker,
                                 TimeLimiter timeLimiter,
                                 Executor executor,
                                 Duration timeout) {
        this.circuitBreaker = circuitBreaker;
        this.timeLimiter = timeLimiter;
        this.executor = executor;
        this.timeout = timeout;
    }

    public <T> T execute(String operation,
                         Supplier<T> call,
                         BiFunction<Failure, Throwable, T> fallback) {
        try {
            return circuitBreaker.executeCallable(() -> timeLimiter.executeFutureSupplier(
                    () -> CompletableFuture.supplyAsync(call, executor)));
        } catch (CallNotPermittedException e) {
            log.warn("외부 호출 차단 (circuit open) [operation={} state={}]",
                    operation, circuitBreaker.getState());
            return fallback.apply(Failure.CIRCUIT_OPEN, e);
        } catch (Exception e) {
            Throwable cause = NestedExceptionUtils.getMostSpecificCause(e);
            if (cause instanceof TimeoutException) {
                log.warn("외부 호출 타임아웃 [operation={} timeoutMs={}]", operation, timeout.toMillis());
                return fallback.apply(Failure.TIMEOUT, cause);
            }
            if (cause instanceof RejectedExecutionException) {
                log.warn("외부 호출 대기열 포화 [operation={}]", operation);
                return fallback.apply(Failure.QUEUE_REJECTED, cause);
            }
            return fallback.apply(Failure.ERROR, cause);
        }
    }
}
