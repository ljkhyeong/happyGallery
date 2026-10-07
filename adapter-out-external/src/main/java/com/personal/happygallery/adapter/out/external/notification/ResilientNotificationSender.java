package com.personal.happygallery.adapter.out.external.notification;

import com.personal.happygallery.adapter.out.external.resilience.ResilientExternalCall;
import com.personal.happygallery.adapter.out.external.resilience.ResilientExternalCall.Failure;
import com.personal.happygallery.application.notification.port.out.NotificationSendResult;
import com.personal.happygallery.application.notification.port.out.NotificationSendOutcome;
import com.personal.happygallery.application.notification.port.out.NotificationSenderPort;
import com.personal.happygallery.domain.notification.NotificationChannel;
import com.personal.happygallery.domain.notification.NotificationEventType;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.timelimiter.TimeLimiter;
import java.time.Duration;
import java.util.concurrent.Executor;
import java.util.function.BiFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 알림 발송 어댑터에 서킷 브레이커 + 타임아웃을 씌우는 데코레이터.
 *
 * <p>소켓 타임아웃은 {@code PooledHttpClientFactory}에서 이미 적용되지만,
 * 부분 장애가 누적될 때 호출 스레드를 fail-fast로 회수하려면 CircuitBreaker가 필요하다.
 * TimeLimiter는 PG 보호와 동일한 이중 안전장치 의미.
 *
 * <p>호출 전 차단과 대기열 거절은 재시도 가능한 실패로, 호출 시작 뒤 타임아웃과
 * 예상하지 못한 예외는 실제 발송 여부를 알 수 없는 결과로 구분한다.
 */
public class ResilientNotificationSender implements NotificationSenderPort {

    private static final Logger log = LoggerFactory.getLogger(ResilientNotificationSender.class);

    private final NotificationSenderPort delegate;
    private final ResilientExternalCall resilientCall;

    public ResilientNotificationSender(NotificationSenderPort delegate,
                                       CircuitBreaker circuitBreaker,
                                       TimeLimiter timeLimiter,
                                       Executor executor,
                                       Duration timeout) {
        this.delegate = delegate;
        this.resilientCall = new ResilientExternalCall(circuitBreaker, timeLimiter, executor, timeout);
    }

    /** 호출 전 차단·대기열 거절은 재시도 가능한 실패로, 호출 시작 뒤 실패는 결과 미확인으로 나눈다. */
    static <T> BiFunction<Failure, Throwable, T> fallback(String operation, T unavailable, T unknown) {
        return (failure, cause) -> {
            if (failure == Failure.ERROR) {
                log.warn("알림 외부 호출 예외 [operation={} type={}]",
                        operation, cause.getClass().getSimpleName());
            }
            return failure == Failure.CIRCUIT_OPEN || failure == Failure.QUEUE_REJECTED
                    ? unavailable
                    : unknown;
        };
    }

    @Override
    public NotificationChannel channel() {
        return delegate.channel();
    }

    @Override
    public NotificationSendOutcome send(
            String idempotencyKey,
            String phone,
            String recipientName,
            NotificationEventType eventType) {
        String operation = channel() + ":" + eventType.name();
        return resilientCall.execute(
                operation,
                () -> delegate.send(idempotencyKey, phone, recipientName, eventType),
                fallback(operation,
                        NotificationSendOutcome.immediate(NotificationSendResult.TRANSIENT_FAILURE),
                        NotificationSendOutcome.immediate(NotificationSendResult.DELIVERY_UNKNOWN)));
    }
}
