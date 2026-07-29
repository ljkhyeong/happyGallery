package com.personal.happygallery.adapter.out.external.notification;

import com.personal.happygallery.application.customer.port.out.EmailVerificationSender;
import com.personal.happygallery.application.notification.port.out.NotificationSendResult;
import com.personal.happygallery.domain.notification.NotificationChannel;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.timelimiter.TimeLimiter;
import java.util.concurrent.Executor;

/** 이메일 인증 SMTP 호출에 제한 큐·타임아웃·서킷 브레이커를 적용한다. */
public class ResilientEmailVerificationSender implements EmailVerificationSender {

    private final RealEmailVerificationSender delegate;
    private final ResilientNotificationCall resilientCall;

    public ResilientEmailVerificationSender(
            RealEmailVerificationSender delegate,
            CircuitBreaker circuitBreaker,
            TimeLimiter timeLimiter,
            Executor executor,
            long timeoutMillis
    ) {
        this.delegate = delegate;
        this.resilientCall = new ResilientNotificationCall(
                circuitBreaker, timeLimiter, executor, timeoutMillis);
    }

    @Override
    public boolean send(String email, String verificationCode) {
        NotificationSendResult result = resilientCall.execute(
                NotificationChannel.EMAIL,
                "EMAIL_VERIFICATION",
                () -> delegate.sendResult(email, verificationCode),
                NotificationSendResult.TRANSIENT_FAILURE,
                NotificationSendResult.DELIVERY_UNKNOWN);
        return result.isSuccess();
    }
}
