package com.personal.happygallery.adapter.out.external.notification;

import com.personal.happygallery.adapter.out.external.resilience.ResilientExternalCall;
import com.personal.happygallery.application.customer.port.out.EmailVerificationSender;
import com.personal.happygallery.application.notification.port.out.NotificationSendResult;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.timelimiter.TimeLimiter;
import java.time.Duration;
import java.util.concurrent.Executor;

/** 이메일 인증 외부 호출에 제한 큐·타임아웃·서킷 브레이커를 적용한다. */
public class ResilientEmailVerificationSender implements EmailVerificationSender {

    private static final String OPERATION = "EMAIL:EMAIL_VERIFICATION";

    private final EmailVerificationTransport delegate;
    private final ResilientExternalCall resilientCall;

    public ResilientEmailVerificationSender(
            EmailVerificationTransport delegate,
            CircuitBreaker circuitBreaker,
            TimeLimiter timeLimiter,
            Executor executor,
            Duration timeout
    ) {
        this.delegate = delegate;
        this.resilientCall = new ResilientExternalCall(circuitBreaker, timeLimiter, executor, timeout);
    }

    @Override
    public boolean send(String email, String verificationCode) {
        NotificationSendResult result = resilientCall.execute(
                OPERATION,
                () -> delegate.sendResult(email, verificationCode),
                ResilientNotificationSender.fallback(OPERATION,
                        NotificationSendResult.TRANSIENT_FAILURE,
                        NotificationSendResult.DELIVERY_UNKNOWN));
        return result.isSuccess();
    }
}
