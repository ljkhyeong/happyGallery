package com.personal.happygallery.application.customer;

import com.personal.happygallery.application.customer.port.in.PhoneOwnershipVerificationUseCase;
import com.personal.happygallery.application.customer.port.out.GuestStorePort;
import com.personal.happygallery.domain.booking.Guest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 전화번호 인증 코드를 검증하고, Guest를 upsert하는 공통 로직.
 * booking/order/pass 생성 시 동일한 패턴을 반복하지 않도록 추출했다.
 */
@Component
public class VerifiedGuestResolver {

    private final PhoneOwnershipVerificationUseCase phoneOwnershipVerification;
    private final GuestStorePort guestStore;
    private final GuestPersonalDataProtector guestPersonalDataProtector;

    public VerifiedGuestResolver(PhoneOwnershipVerificationUseCase phoneOwnershipVerification,
                                  GuestStorePort guestStore,
                                  GuestPersonalDataProtector guestPersonalDataProtector) {
        this.phoneOwnershipVerification = phoneOwnershipVerification;
        this.guestStore = guestStore;
        this.guestPersonalDataProtector = guestPersonalDataProtector;
    }

    /**
     * 인증 코드를 검증·소모하고, 전화번호 기준으로 Guest를 upsert 한다.
     *
     * @return phoneVerified 상태의 Guest
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public Guest resolveVerifiedGuest(String phone, String verificationCode, String name) {
        phoneOwnershipVerification.verify(phone, verificationCode);

        Guest guest = guestStore.getOrCreateByPhoneHmac(guestPersonalDataProtector.newGuest(name, phone));
        guest.markPhoneVerified();

        return guest;
    }
}
