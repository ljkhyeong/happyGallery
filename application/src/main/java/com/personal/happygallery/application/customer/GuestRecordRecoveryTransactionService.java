package com.personal.happygallery.application.customer;

import com.personal.happygallery.application.customer.port.in.GuestRecordRecoveryUseCase.RecoveryResult;
import com.personal.happygallery.application.customer.port.out.GuestReaderPort;
import com.personal.happygallery.application.customer.port.out.GuestRecordRecoveryTargetPort;
import com.personal.happygallery.application.token.GuestTokenService;
import com.personal.happygallery.application.token.GuestTokenService.IssuedToken;
import com.personal.happygallery.domain.booking.PhoneVerificationPurpose;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 시도 제한을 통과한 복구 요청의 인증 코드 소비와 주문·예약 접근 토큰 교체를 한 트랜잭션으로 처리한다. */
@Service
class GuestRecordRecoveryTransactionService {

    private final PhoneVerificationConsumptionService phoneVerification;
    private final GuestReaderPort guestReader;
    private final GuestRecordRecoveryTargetPort recoveryTargets;
    private final GuestPersonalDataProtector guestPersonalDataProtector;
    private final GuestTokenService guestTokenService;

    GuestRecordRecoveryTransactionService(
            PhoneVerificationConsumptionService phoneVerification,
            GuestReaderPort guestReader,
            GuestRecordRecoveryTargetPort recoveryTargets,
            GuestPersonalDataProtector guestPersonalDataProtector,
            GuestTokenService guestTokenService
    ) {
        this.phoneVerification = phoneVerification;
        this.guestReader = guestReader;
        this.recoveryTargets = recoveryTargets;
        this.guestPersonalDataProtector = guestPersonalDataProtector;
        this.guestTokenService = guestTokenService;
    }

    @Transactional
    public RecoveryResult recover(String normalizedPhone, String verificationCode) {
        phoneVerification.consume(
                normalizedPhone, verificationCode, PhoneVerificationPurpose.GUEST_RECORD_RECOVERY);

        IssuedToken token = guestTokenService.issueRecoveryToken();
        guestReader.findByPhoneHmac(guestPersonalDataProtector.indexPhone(normalizedPhone))
                .ifPresent(guest -> {
                    recoveryTargets.replaceOrderAccessTokens(guest.getId(), token.tokenHash());
                    recoveryTargets.replaceBookingAccessTokens(guest.getId(), token.tokenHash());
                });
        return new RecoveryResult(token.rawToken(), token.expiresAt());
    }
}
