package com.personal.happygallery.adapter.out.external.notification;

import com.personal.happygallery.application.customer.port.out.PhoneVerificationSender;
import com.personal.happygallery.application.notification.port.out.NotificationSendResult;
import com.personal.happygallery.domain.notification.NotificationChannel;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.timelimiter.TimeLimiter;
import java.time.Duration;
import java.util.concurrent.Executor;

/** 인증 SMS에도 일반 SMS와 같은 장애 격리 정책을 적용한다. */
public class ResilientPhoneVerificationSender implements PhoneVerificationSender {

    private final RealPhoneVerificationSender delegate;
    private final ResilientNotificationCall resilientCall;

    public ResilientPhoneVerificationSender(RealPhoneVerificationSender delegate,
                                            CircuitBreaker circuitBreaker,
                                            TimeLimiter timeLimiter,
                                            Executor executor,
                                            Duration timeout) {
        this.delegate = delegate;
        this.resilientCall = new ResilientNotificationCall(
                circuitBreaker, timeLimiter, executor, timeout);
    }

    @Override
    public boolean send(String phone, String verificationCode) {
        NotificationSendResult result = resilientCall.execute(
                NotificationChannel.SMS,
                "PHONE_VERIFICATION",
                () -> delegate.sendResult(phone, verificationCode),
                NotificationSendResult.TRANSIENT_FAILURE,
                NotificationSendResult.DELIVERY_UNKNOWN);
        return result.isSuccess();
    }
}
