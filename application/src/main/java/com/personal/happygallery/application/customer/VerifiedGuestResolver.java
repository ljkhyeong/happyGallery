package com.personal.happygallery.application.customer;

import com.personal.happygallery.application.customer.port.in.PhoneOwnershipVerificationUseCase;
import com.personal.happygallery.application.customer.port.out.GuestStorePort;
import com.personal.happygallery.application.payment.GuestPaymentVerificationService;
import com.personal.happygallery.domain.booking.Guest;
import com.personal.happygallery.domain.payment.PaymentContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** 휴대폰 인증 코드 또는 결제 귀속 증거를 검증하고 Guest를 upsert한다. */
@Component
public class VerifiedGuestResolver {

    private final PhoneOwnershipVerificationUseCase phoneOwnershipVerification;
    private final GuestStorePort guestStore;
    private final GuestPersonalDataProtector guestPersonalDataProtector;
    private final GuestPaymentVerificationService guestPaymentVerification;

    public VerifiedGuestResolver(PhoneOwnershipVerificationUseCase phoneOwnershipVerification,
                                  GuestStorePort guestStore,
                                  GuestPersonalDataProtector guestPersonalDataProtector,
                                  GuestPaymentVerificationService guestPaymentVerification) {
        this.phoneOwnershipVerification = phoneOwnershipVerification;
        this.guestStore = guestStore;
        this.guestPersonalDataProtector = guestPersonalDataProtector;
        this.guestPaymentVerification = guestPaymentVerification;
    }

    /**
     * 인증 코드를 검증·소모하고, 전화번호 기준으로 Guest를 upsert 한다.
     *
     * @return phoneVerified 상태의 Guest
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public Guest resolveWithVerificationCode(String phone, String verificationCode, String name) {
        phoneOwnershipVerification.verify(phone, verificationCode);
        return getOrCreateVerifiedGuest(phone, name);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public Guest resolveWithPaymentProof(
            PaymentContext context,
            String paymentOrderId,
            String phone,
            String verificationProof,
            String name) {
        guestPaymentVerification.requireValid(context, paymentOrderId, phone, verificationProof);
        return getOrCreateVerifiedGuest(phone, name);
    }

    private Guest getOrCreateVerifiedGuest(String phone, String name) {
        Guest guest = guestStore.getOrCreateByPhoneHmac(guestPersonalDataProtector.newGuest(name, phone));
        guest.markPhoneVerified();
        return guest;
    }
}
