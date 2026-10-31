package com.personal.happygallery.application.customer;

import com.personal.happygallery.application.customer.port.in.PhoneOwnershipVerificationUseCase;
import com.personal.happygallery.application.customer.port.out.PhoneVerificationAttemptGuard;
import com.personal.happygallery.domain.booking.PhoneVerificationPurpose;
import com.personal.happygallery.domain.user.KoreanPhoneNumber;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 시도 제한과 인증 코드 소비를 현재 DB 트랜잭션 안에서 함께 수행한다.
 *
 * <p>잠금 아래 업무 조건을 먼저 확인한 뒤에야 시도 제한을 적용해야 하는 흐름(비회원 빈자리 알림의 만석 회차 확인)만 사용한다.
 * 그 외 인증 코드 소비 흐름은 facade에서 {@link PhoneVerificationAttemptGuard}를 트랜잭션 밖에서 먼저 호출하고
 * {@link PhoneVerificationConsumptionService}를 짧은 트랜잭션 안에서 호출한다.
 */
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
