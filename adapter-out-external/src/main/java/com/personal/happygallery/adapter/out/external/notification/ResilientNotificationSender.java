package com.personal.happygallery.adapter.out.external.notification;

import com.personal.happygallery.application.notification.port.out.NotificationSendResult;
import com.personal.happygallery.domain.notification.NotificationChannel;
import com.personal.happygallery.domain.notification.NotificationEventType;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.timelimiter.TimeLimiter;
import java.time.Duration;
import java.util.concurrent.Executor;

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
public class ResilientNotificationSender implements NotificationSender {

    private final NotificationSender delegate;
    private final ResilientNotificationCall resilientCall;

    public ResilientNotificationSender(NotificationSender delegate,
                                       CircuitBreaker circuitBreaker,
                                       TimeLimiter timeLimiter,
                                       Executor executor,
                                       Duration timeout) {
        this.delegate = delegate;
        this.resilientCall = new ResilientNotificationCall(
                circuitBreaker, timeLimiter, executor, timeout);
    }

    @Override
    public NotificationChannel channel() {
        return delegate.channel();
    }

    @Override
    public NotificationSendResult send(String idempotencyKey,
                                       String phone,
                                       String recipientName,
                                       NotificationEventType eventType) {
        return resilientCall.execute(
                channel(),
                eventType.name(),
                () -> delegate.send(idempotencyKey, phone, recipientName, eventType),
                NotificationSendResult.TRANSIENT_FAILURE,
                NotificationSendResult.DELIVERY_UNKNOWN);
    }
}
