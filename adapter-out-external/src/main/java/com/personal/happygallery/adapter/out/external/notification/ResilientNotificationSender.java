package com.personal.happygallery.adapter.out.external.notification;

import com.personal.happygallery.domain.notification.NotificationChannel;
import com.personal.happygallery.domain.notification.NotificationEventType;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.timelimiter.TimeLimiter;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger log = LoggerFactory.getLogger(ResilientNotificationSender.class);

    private final NotificationSender delegate;
    private final CircuitBreaker circuitBreaker;
    private final TimeLimiter timeLimiter;
    private final ExecutorService executor;
    private final long timeoutMillis;

    public ResilientNotificationSender(NotificationSender delegate,
                                       CircuitBreaker circuitBreaker,
                                       TimeLimiter timeLimiter,
                                       ExecutorService executor,
                                       long timeoutMillis) {
        this.delegate = delegate;
        this.circuitBreaker = circuitBreaker;
        this.timeLimiter = timeLimiter;
        this.executor = executor;
        this.timeoutMillis = timeoutMillis;
    }

    @Override
    public NotificationChannel channel() {
        return delegate.channel();
    }

    @Override
    public boolean send(String phone, String recipientName, NotificationEventType eventType) {
        try {
            return circuitBreaker.executeCallable(() -> sendWithTimeout(phone, recipientName, eventType));
        } catch (CallNotPermittedException e) {
            log.warn("[{}] 발송 차단 (circuit open) event={}", channel(), eventType);
            return false;
        } catch (TimeoutException e) {
            log.warn("[{}] 발송 타임아웃 [timeoutMs={} event={}]", channel(), timeoutMillis, eventType);
            return false;
        } catch (Exception e) {
            Throwable cause = rootCause(e);
            if (cause instanceof TimeoutException) {
                log.warn("[{}] 발송 타임아웃 [timeoutMs={} event={}]", channel(), timeoutMillis, eventType);
                return false;
            }
            log.warn("[{}] 발송 예외 event={}", channel(), eventType, cause);
            return false;
        }
    }

    private boolean sendWithTimeout(String phone, String recipientName, NotificationEventType eventType) throws Exception {
        return timeLimiter.executeFutureSupplier(
                () -> CompletableFuture.supplyAsync(
                        () -> delegate.send(phone, recipientName, eventType), executor));
    }

    private Throwable rootCause(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }
}
