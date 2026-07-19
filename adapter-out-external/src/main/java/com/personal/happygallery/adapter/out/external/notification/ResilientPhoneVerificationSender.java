package com.personal.happygallery.adapter.out.external.notification;

import com.personal.happygallery.application.customer.port.out.PhoneVerificationSender;
import com.personal.happygallery.domain.notification.NotificationChannel;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.timelimiter.TimeLimiter;
import java.util.concurrent.ExecutorService;

/** 인증 SMS에도 일반 SMS와 같은 장애 격리 정책을 적용한다. */
public class ResilientPhoneVerificationSender implements PhoneVerificationSender {

    private final PhoneVerificationSender delegate;
    private final ResilientNotificationCall resilientCall;

    public ResilientPhoneVerificationSender(PhoneVerificationSender delegate,
                                            CircuitBreaker circuitBreaker,
                                            TimeLimiter timeLimiter,
                                            ExecutorService executor,
                                            long timeoutMillis) {
        this.delegate = delegate;
        this.resilientCall = new ResilientNotificationCall(
                circuitBreaker, timeLimiter, executor, timeoutMillis);
    }

    @Override
    public boolean send(String phone, String verificationCode) {
        return resilientCall.execute(
                NotificationChannel.SMS,
                "PHONE_VERIFICATION",
                () -> delegate.send(phone, verificationCode));
    }
}
