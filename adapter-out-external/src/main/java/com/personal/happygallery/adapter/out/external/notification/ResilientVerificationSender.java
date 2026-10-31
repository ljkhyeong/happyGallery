package com.personal.happygallery.adapter.out.external.notification;

import com.personal.happygallery.adapter.out.external.resilience.ResilientExternalCall;
import com.personal.happygallery.application.notification.port.out.NotificationSendResult;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.timelimiter.TimeLimiter;
import java.time.Duration;
import java.util.concurrent.Executor;

/**
 * 인증 코드 발송에 제한 큐·타임아웃·서킷 브레이커를 적용한다.
 *
 * <p>휴대폰·이메일 인증 포트는 {@code NotificationResilienceConfig}가 채널별 빈에서 {@link #send}로 연결한다.
 * 한 클래스가 두 포트를 함께 구현하면 포트 타입별 빈이 둘이 되어 주입이 모호해진다.
 */
final class ResilientVerificationSender {

    private final String operation;
    private final VerificationTransport delegate;
    private final ResilientExternalCall resilientCall;

    ResilientVerificationSender(String operation,
                                VerificationTransport delegate,
                                CircuitBreaker circuitBreaker,
                                TimeLimiter timeLimiter,
                                Executor executor,
                                Duration timeout) {
        this.operation = operation;
        this.delegate = delegate;
        this.resilientCall = new ResilientExternalCall(circuitBreaker, timeLimiter, executor, timeout);
    }

    boolean send(String target, String verificationCode) {
        NotificationSendResult result = resilientCall.execute(
                operation,
                () -> delegate.sendResult(target, verificationCode),
                ResilientNotificationSender.fallback(operation,
                        NotificationSendResult.TRANSIENT_FAILURE,
                        NotificationSendResult.DELIVERY_UNKNOWN));
        return result.isSuccess();
    }
}
