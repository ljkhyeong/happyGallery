package com.personal.happygallery.application.customer;

import com.personal.happygallery.application.customer.port.out.PhoneVerificationReaderPort;
import com.personal.happygallery.domain.booking.PhoneVerification;
import com.personal.happygallery.domain.booking.PhoneVerificationPurpose;
import com.personal.happygallery.domain.error.PhoneVerificationFailedException;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PhoneVerificationConsumptionService {

    private final PhoneVerificationReaderPort phoneVerificationReader;
    private final Clock clock;

    public PhoneVerificationConsumptionService(
            PhoneVerificationReaderPort phoneVerificationReader,
            Clock clock
    ) {
        this.phoneVerificationReader = phoneVerificationReader;
        this.clock = clock;
    }

    /** 이미 시도 제한을 통과한 인증 코드를 현재 DB 트랜잭션에서 한 번 소비한다. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void consume(
            String normalizedPhone,
            String verificationCode,
            PhoneVerificationPurpose purpose
    ) {
        PhoneVerification verification = phoneVerificationReader
                .findValidVerification(
                        normalizedPhone, verificationCode, purpose, LocalDateTime.now(clock))
                .orElseThrow(PhoneVerificationFailedException::new);
        verification.markVerified();
    }
}
