package com.personal.happygallery.application.booking;

import com.personal.happygallery.application.booking.port.in.GuestBookingUseCase;
import com.personal.happygallery.application.booking.port.out.BookingReaderPort;
import com.personal.happygallery.application.customer.VerifiedGuestResolver;
import com.personal.happygallery.application.customer.port.out.PhoneVerificationSender;
import com.personal.happygallery.application.token.GuestTokenService;
import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.booking.DepositPaymentMethod;
import com.personal.happygallery.domain.booking.Guest;
import com.personal.happygallery.domain.booking.PhoneVerification;
import com.personal.happygallery.domain.booking.Slot;
import com.personal.happygallery.domain.error.DuplicateBookingException;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DefaultGuestBookingService implements GuestBookingUseCase {

    private static final Logger log = LoggerFactory.getLogger(DefaultGuestBookingService.class);

    /** 인증 코드 유효 시간 (5분) */
    private static final int VERIFICATION_EXPIRE_MINUTES = 5;

    private final VerifiedGuestResolver verifiedGuestResolver;
    private final PhoneVerificationIssueTransactionService phoneVerificationIssueTransaction;
    private final PhoneVerificationSender phoneVerificationSender;
    private final BookingReaderPort bookingReaderPort;
    private final SlotCapacitySupport slotCapacitySupport;
    private final BookingCreationSupport creationSupport;
    private final GuestTokenService guestTokenService;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public DefaultGuestBookingService(VerifiedGuestResolver verifiedGuestResolver,
                                      PhoneVerificationIssueTransactionService phoneVerificationIssueTransaction,
                                      PhoneVerificationSender phoneVerificationSender,
                                      BookingReaderPort bookingReaderPort,
                                      SlotCapacitySupport slotCapacitySupport,
                                      BookingCreationSupport creationSupport,
                                      GuestTokenService guestTokenService,
                                      Clock clock) {
        this.verifiedGuestResolver = verifiedGuestResolver;
        this.phoneVerificationIssueTransaction = phoneVerificationIssueTransaction;
        this.phoneVerificationSender = phoneVerificationSender;
        this.bookingReaderPort = bookingReaderPort;
        this.slotCapacitySupport = slotCapacitySupport;
        this.creationSupport = creationSupport;
        this.guestTokenService = guestTokenService;
        this.clock = clock;
    }

    /** 휴대폰 인증 코드를 생성·저장하고 전용 SMS 경계로 발송한다. */
    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public PhoneVerification sendVerificationCode(String phone) {
        String code = "%06d".formatted(random.nextInt(1_000_000));
        LocalDateTime expiresAt = LocalDateTime.now(clock)
                .plusMinutes(VERIFICATION_EXPIRE_MINUTES);
        PhoneVerification pv = new PhoneVerification(phone, code, expiresAt);
        pv = phoneVerificationIssueTransaction.save(pv);
        if (!phoneVerificationSender.send(pv.getPhone(), code)) {
            throw new HappyGalleryException(
                    ErrorCode.SERVICE_UNAVAILABLE,
                    "인증 코드를 발송하지 못했습니다. 잠시 후 다시 시도해주세요.");
        }
        pv = phoneVerificationIssueTransaction.completeDelivery(pv.getId(), pv.getPhone());
        log.info("[phone-verification] created [verificationId={}]", pv.getId());
        return pv;
    }

    /** 게스트 예약을 생성한다. 비회원은 예약금 결제만 허용한다. */
    @Override
    public GuestBookingResult createGuestBooking(CreateGuestBookingCommand command) {
        // 1. 인증 코드 검증 + Guest upsert
        Guest guest = verifiedGuestResolver.resolveVerifiedGuest(
                command.phone(), command.code(), command.name());

        // 2. 슬롯 예약 가능 여부 확인 (락 전 빠른 체크)
        slotCapacitySupport.requireAvailableSlot(command.slotId());

        // 3. 중복 예약 확인
        if (bookingReaderPort.existsBookedBySlotIdAndGuestId(command.slotId(), guest.getId())) {
            throw new DuplicateBookingException();
        }

        // 4. 비관적 락 + 정원 증가 + 첫 예약이면 뒤쪽 버퍼 차단
        Slot slot = slotCapacitySupport.reserveCapacity(command.slotId());

        GuestTokenService.IssuedToken issued = guestTokenService.issue();
        String rawToken = issued.rawToken();
        String accessToken = issued.tokenHash();

        creationSupport.requireValidDeposit(command.paymentMethod());
        Booking booking = Booking.forGuestDeposit(
                guest,
                slot,
                command.depositAmount(),
                command.balanceAmount(),
                command.paymentMethod(),
                accessToken);

        booking = creationSupport.saveAndComplete(booking, slot);
        return new GuestBookingResult(booking, rawToken);
    }
}
