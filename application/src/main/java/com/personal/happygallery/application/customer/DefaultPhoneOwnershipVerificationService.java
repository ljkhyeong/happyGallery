package com.personal.happygallery.application.customer;

import com.personal.happygallery.application.customer.port.in.PhoneOwnershipVerificationUseCase;
import com.personal.happygallery.application.customer.port.out.PhoneVerificationReaderPort;
import com.personal.happygallery.domain.booking.PhoneVerification;
import com.personal.happygallery.domain.error.PhoneVerificationFailedException;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DefaultPhoneOwnershipVerificationService implements PhoneOwnershipVerificationUseCase {

    private final PhoneVerificationReaderPort phoneVerificationReader;
    private final Clock clock;

    public DefaultPhoneOwnershipVerificationService(PhoneVerificationReaderPort phoneVerificationReader,
                                                    Clock clock) {
        this.phoneVerificationReader = phoneVerificationReader;
        this.clock = clock;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void verify(String phone, String verificationCode) {
        PhoneVerification verification = phoneVerificationReader
                .findValidVerification(phone, verificationCode, LocalDateTime.now(clock))
                .orElseThrow(PhoneVerificationFailedException::new);
        verification.markVerified();
    }
}
