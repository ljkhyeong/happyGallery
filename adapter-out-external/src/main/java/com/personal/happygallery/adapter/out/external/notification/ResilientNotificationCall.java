package com.personal.happygallery.adapter.out.external.notification;

import com.personal.happygallery.domain.notification.NotificationChannel;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.timelimiter.TimeLimiter;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.NestedExceptionUtils;

/** 알림 채널 외부 호출에 공통 서킷 브레이커·타임아웃·제한 큐 정책을 적용한다. */
final class ResilientNotificationCall {

    private static final Logger log = LoggerFactory.getLogger(ResilientNotificationCall.class);

    private final CircuitBreaker circuitBreaker;
    private final TimeLimiter timeLimiter;
    private final ExecutorService executor;
    private final long timeoutMillis;

    ResilientNotificationCall(CircuitBreaker circuitBreaker,
                              TimeLimiter timeLimiter,
                              ExecutorService executor,
                              long timeoutMillis) {
        this.circuitBreaker = circuitBreaker;
        this.timeLimiter = timeLimiter;
        this.executor = executor;
        this.timeoutMillis = timeoutMillis;
    }

    <T> T execute(NotificationChannel channel,
                  String operation,
                  Supplier<T> call,
                  T unavailableResult,
                  T unknownResult) {
        try {
            return circuitBreaker.executeCallable(() -> timeLimiter.executeFutureSupplier(
                    () -> CompletableFuture.supplyAsync(call, executor)));
        } catch (CallNotPermittedException e) {
            log.warn("[{}] 발송 차단 (circuit open) operation={}", channel, operation);
            return unavailableResult;
        } catch (TimeoutException e) {
            log.warn("[{}] 발송 타임아웃 [timeoutMs={} operation={}]",
                    channel, timeoutMillis, operation);
            return unknownResult;
        } catch (RejectedExecutionException e) {
            log.warn("[{}] 발송 대기열 포화 [operation={}]", channel, operation);
            return unavailableResult;
        } catch (Exception e) {
            Throwable cause = NestedExceptionUtils.getMostSpecificCause(e);
            if (cause instanceof TimeoutException) {
                log.warn("[{}] 발송 타임아웃 [timeoutMs={} operation={}]",
                        channel, timeoutMillis, operation);
                return unknownResult;
            }
            if (cause instanceof RejectedExecutionException) {
                log.warn("[{}] 발송 대기열 포화 [operation={}]", channel, operation);
                return unavailableResult;
            }
            log.warn("[{}] 발송 예외 [operation={} type={}]",
                    channel, operation, cause.getClass().getSimpleName());
            return unknownResult;
        }
    }
}
