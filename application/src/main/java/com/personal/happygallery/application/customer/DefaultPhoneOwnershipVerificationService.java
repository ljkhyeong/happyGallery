package com.personal.happygallery.application.customer;

import com.personal.happygallery.application.customer.port.in.PhoneOwnershipVerificationUseCase;
import com.personal.happygallery.application.customer.port.out.PhoneVerificationAttemptGuard;
import com.personal.happygallery.domain.booking.PhoneVerificationPurpose;
import com.personal.happygallery.domain.user.KoreanPhoneNumber;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DefaultPhoneOwnershipVerificationService implements PhoneOwnershipVerificationUseCase {

    private final PhoneVerificationAttemptGuard attemptGuard;
    private final PhoneVerificationConsumptionService consumptionService;

    public DefaultPhoneOwnershipVerificationService(
            PhoneVerificationAttemptGuard attemptGuard,
            PhoneVerificationConsumptionService consumptionService
    ) {
        this.attemptGuard = attemptGuard;
        this.consumptionService = consumptionService;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void verify(String phone,
                       String verificationCode,
                       PhoneVerificationPurpose purpose) {
        String normalizedPhone = KoreanPhoneNumber.required(phone);
        attemptGuard.check(normalizedPhone);
        consumptionService.consume(normalizedPhone, verificationCode, purpose);
    }
}
