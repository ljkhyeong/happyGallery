package com.personal.happygallery.adapter.out.external.notification;

import com.personal.happygallery.domain.notification.NotificationChannel;
import com.personal.happygallery.domain.notification.NotificationEventType;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.timelimiter.TimeLimiter;
import java.util.concurrent.ExecutorService;

/**
 * 알림 발송 어댑터에 서킷 브레이커 + 타임아웃을 씌우는 데코레이터.
 *
 * <p>소켓 타임아웃은 {@code PooledHttpClientFactory}에서 이미 적용되지만,
 * 부분 장애가 누적될 때 호출 스레드를 fail-fast로 회수하려면 CircuitBreaker가 필요하다.
 * TimeLimiter는 PG 보호와 동일한 이중 안전장치 의미.
 *
 * <p>장애 상황(차단/타임아웃/예외)에서는 {@code false}를 반환해
 * {@link com.personal.happygallery.application.notification.NotificationService}의
 * 채널 fallback 체인이 그대로 동작하도록 한다.
 */
public class ResilientNotificationSender implements NotificationSender {

    private final NotificationSender delegate;
    private final ResilientNotificationCall resilientCall;

    public ResilientNotificationSender(NotificationSender delegate,
                                       CircuitBreaker circuitBreaker,
                                       TimeLimiter timeLimiter,
                                       ExecutorService executor,
                                       long timeoutMillis) {
        this.delegate = delegate;
        this.resilientCall = new ResilientNotificationCall(
                circuitBreaker, timeLimiter, executor, timeoutMillis);
    }

    @Override
    public NotificationChannel channel() {
        return delegate.channel();
    }

    @Override
    public boolean send(String phone, String recipientName, NotificationEventType eventType) {
        return resilientCall.execute(
                channel(),
                eventType.name(),
                () -> delegate.send(phone, recipientName, eventType));
    }
}
