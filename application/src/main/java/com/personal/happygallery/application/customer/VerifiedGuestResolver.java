package com.personal.happygallery.application.customer;

import com.personal.happygallery.application.customer.port.out.GuestStorePort;
import com.personal.happygallery.application.customer.port.out.PhoneVerificationReaderPort;
import com.personal.happygallery.domain.error.PhoneVerificationFailedException;
import com.personal.happygallery.domain.booking.Guest;
import com.personal.happygallery.domain.booking.PhoneVerification;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 전화번호 인증 코드를 검증하고, Guest를 upsert하는 공통 로직.
 * booking/order/pass 생성 시 동일한 패턴을 반복하지 않도록 추출했다.
 */
@Component
public class VerifiedGuestResolver {

    private final PhoneVerificationReaderPort phoneVerificationReader;
    private final GuestStorePort guestStore;
    private final GuestPhoneProtector guestPhoneProtector;
    private final Clock clock;

    public VerifiedGuestResolver(PhoneVerificationReaderPort phoneVerificationReader,
                                  GuestStorePort guestStore,
                                  GuestPhoneProtector guestPhoneProtector,
                                  Clock clock) {
        this.phoneVerificationReader = phoneVerificationReader;
        this.guestStore = guestStore;
        this.guestPhoneProtector = guestPhoneProtector;
        this.clock = clock;
    }

    /**
     * 인증 코드를 검증·소모하고, 전화번호 기준으로 Guest를 upsert 한다.
     *
     * @return phoneVerified 상태의 Guest
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public Guest resolveVerifiedGuest(String phone, String verificationCode, String name) {
        PhoneVerification pv = phoneVerificationReader
                .findValidVerification(phone, verificationCode, LocalDateTime.now(clock))
                .orElseThrow(PhoneVerificationFailedException::new);
        pv.markVerified();

        Guest guest = guestStore.getOrCreateByPhoneHmac(guestPhoneProtector.newGuest(name, phone));
        guest.markPhoneVerified();

        return guest;
    }
}
