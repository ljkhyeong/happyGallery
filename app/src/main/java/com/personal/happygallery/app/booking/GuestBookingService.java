package com.personal.happygallery.app.booking;

import com.personal.happygallery.app.booking.port.in.GuestBookingUseCase;
import com.personal.happygallery.app.booking.port.out.BookingReaderPort;
import com.personal.happygallery.app.customer.VerifiedGuestResolver;
import com.personal.happygallery.app.customer.port.out.PhoneVerificationStorePort;
import com.personal.happygallery.common.error.DuplicateBookingException;
import com.personal.happygallery.common.token.AccessTokenHasher;
import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.booking.DepositPaymentMethod;
import com.personal.happygallery.domain.booking.Guest;
import com.personal.happygallery.domain.booking.PhoneVerification;
import com.personal.happygallery.domain.booking.Slot;
import com.personal.happygallery.domain.pass.PassPurchase;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class GuestBookingService implements GuestBookingUseCase {

    private static final Logger log = LoggerFactory.getLogger(GuestBookingService.class);

    /** 인증 코드 유효 시간 (5분) */
    private static final int VERIFICATION_EXPIRE_MINUTES = 5;

    private final VerifiedGuestResolver verifiedGuestResolver;
    private final PhoneVerificationStorePort phoneVerificationStorePort;
    private final BookingReaderPort bookingReaderPort;
    private final BookingCreationSupport creationSupport;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public GuestBookingService(VerifiedGuestResolver verifiedGuestResolver,
                               PhoneVerificationStorePort phoneVerificationStorePort,
                               BookingReaderPort bookingReaderPort,
                               BookingCreationSupport creationSupport,
                               Clock clock) {
        this.verifiedGuestResolver = verifiedGuestResolver;
        this.phoneVerificationStorePort = phoneVerificationStorePort;
        this.bookingReaderPort = bookingReaderPort;
        this.creationSupport = creationSupport;
        this.clock = clock;
    }

    /**
     * 휴대폰 인증 코드를 생성·저장한다.
     * 실제 SMS 발송은 미구현 — 코드는 서버 로그에서만 확인 가능.
     *
     * @return 저장된 PhoneVerification (id, phone — code는 응답에 포함하지 않음)
     */
    public PhoneVerification sendVerificationCode(String phone) {
        String code = String.format("%06d", random.nextInt(1_000_000));
        LocalDateTime expiresAt = LocalDateTime.now(clock)
                .plusMinutes(VERIFICATION_EXPIRE_MINUTES);
        PhoneVerification pv = new PhoneVerification(phone, code, expiresAt);
        pv = phoneVerificationStorePort.save(pv);
        log.info("[phone-verification] phone={}, verificationId={}, code={}", phone, pv.getId(), code);
        return pv;
    }

    /**
     * 게스트 예약을 생성한다. {@code passId}가 있으면 8회권 결제, 없으면 예약금 결제.
     *
     * @param passId 8회권 ID (null이면 예약금 결제)
     */
    public GuestBookingResult createGuestBooking(String phone, String code, String name,
                                      Long slotId, long depositAmount,
                                      DepositPaymentMethod paymentMethod,
                                      Long passId) {
        // 1. 인증 코드 검증 + Guest upsert
        Guest guest = verifiedGuestResolver.resolveVerifiedGuest(phone, code, name);

        // 2. 슬롯 활성 여부 확인 (락 전 빠른 체크)
        Slot slot = creationSupport.loadActiveSlot(slotId);

        // 3. 중복 예약 확인
        if (bookingReaderPort.existsBySlotIdAndGuestId(slotId, guest.getId())) {
            throw new DuplicateBookingException();
        }

        // 4. 비관적 락 + 정원 증가 + 버퍼 비활성화
        creationSupport.lockSlotCapacity(slotId);

        String rawToken = AccessTokenHasher.generate();
        String accessToken = AccessTokenHasher.hash(rawToken);

        Booking booking;
        if (passId != null) {
            PassPurchase pass = creationSupport.deductPassCredit(passId, null);
            booking = Booking.forGuestPass(guest, slot, pass, accessToken);
        } else {
            creationSupport.requireValidDeposit(paymentMethod);
            long balanceAmount = slot.getBookingClass().getPrice() - depositAmount;
            booking = Booking.forGuestDeposit(guest, slot, depositAmount, balanceAmount, paymentMethod, accessToken);
        }

        booking = creationSupport.saveAndComplete(booking, slot);
        return new GuestBookingResult(booking, rawToken);
    }
}
