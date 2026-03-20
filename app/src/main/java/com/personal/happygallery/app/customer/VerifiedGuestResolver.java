package com.personal.happygallery.app.customer;

import com.personal.happygallery.app.customer.port.out.GuestReaderPort;
import com.personal.happygallery.app.customer.port.out.GuestStorePort;
import com.personal.happygallery.app.customer.port.out.PhoneVerificationReaderPort;
import com.personal.happygallery.common.error.PhoneVerificationFailedException;
import com.personal.happygallery.domain.booking.Guest;
import com.personal.happygallery.domain.booking.PhoneVerification;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;

/**
 * 전화번호 인증 코드를 검증하고, Guest를 upsert하는 공통 로직.
 * booking/order/pass 생성 시 동일한 패턴을 반복하지 않도록 추출했다.
 */
@Component
public class VerifiedGuestResolver {

    private final PhoneVerificationReaderPort phoneVerificationReader;
    private final GuestReaderPort guestReader;
    private final GuestStorePort guestStore;
    private final Clock clock;

    public VerifiedGuestResolver(PhoneVerificationReaderPort phoneVerificationReader,
                                  GuestReaderPort guestReader,
                                  GuestStorePort guestStore,
                                  Clock clock) {
        this.phoneVerificationReader = phoneVerificationReader;
        this.guestReader = guestReader;
        this.guestStore = guestStore;
        this.clock = clock;
    }

    /**
     * 인증 코드를 검증·소모하고, 전화번호 기준으로 Guest를 upsert 한다.
     *
     * @return phoneVerified 상태의 Guest
     */
    public Guest resolveVerifiedGuest(String phone, String verificationCode, String name) {
        PhoneVerification pv = phoneVerificationReader
                .findValidVerification(phone, verificationCode, LocalDateTime.now(clock))
                .orElseThrow(PhoneVerificationFailedException::new);
        pv.markVerified();

        Guest guest = guestReader.findByPhone(phone)
                .orElseGet(() -> guestStore.save(new Guest(name, phone)));
        guest.markPhoneVerified();

        return guest;
    }
}
