package com.personal.happygallery.application.customer;

import com.personal.happygallery.application.customer.port.in.PhoneOwnershipVerificationUseCase;
import com.personal.happygallery.application.customer.port.out.PhoneVerificationAttemptGuard;
import com.personal.happygallery.application.customer.port.out.PhoneVerificationReaderPort;
import com.personal.happygallery.domain.booking.PhoneVerification;
import com.personal.happygallery.domain.booking.PhoneVerificationPurpose;
import com.personal.happygallery.domain.error.PhoneVerificationFailedException;
import com.personal.happygallery.domain.user.KoreanPhoneNumber;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DefaultPhoneOwnershipVerificationService implements PhoneOwnershipVerificationUseCase {

    private final PhoneVerificationReaderPort phoneVerificationReader;
    private final PhoneVerificationAttemptGuard attemptGuard;
    private final Clock clock;

    public DefaultPhoneOwnershipVerificationService(PhoneVerificationReaderPort phoneVerificationReader,
                                                    PhoneVerificationAttemptGuard attemptGuard,
                                                    Clock clock) {
        this.phoneVerificationReader = phoneVerificationReader;
        this.attemptGuard = attemptGuard;
        this.clock = clock;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void verify(String phone,
                       String verificationCode,
                       PhoneVerificationPurpose purpose) {
        String normalizedPhone = KoreanPhoneNumber.required(phone);
        attemptGuard.check(normalizedPhone);
        PhoneVerification verification = phoneVerificationReader
                .findValidVerification(
                        normalizedPhone, verificationCode, purpose, LocalDateTime.now(clock))
                .orElseThrow(PhoneVerificationFailedException::new);
        verification.markVerified();
    }
}
