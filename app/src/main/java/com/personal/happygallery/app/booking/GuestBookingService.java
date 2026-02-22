package com.personal.happygallery.app.booking;

import com.personal.happygallery.common.error.DuplicateBookingException;
import com.personal.happygallery.common.error.NotFoundException;
import com.personal.happygallery.common.error.PhoneVerificationFailedException;
import com.personal.happygallery.common.error.SlotNotAvailableException;
import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.booking.BookingHistory;
import com.personal.happygallery.domain.booking.BookingHistoryAction;
import com.personal.happygallery.domain.booking.Guest;
import com.personal.happygallery.domain.booking.PhoneVerification;
import com.personal.happygallery.domain.booking.Slot;
import com.personal.happygallery.infra.booking.BookingHistoryRepository;
import com.personal.happygallery.infra.booking.BookingRepository;
import com.personal.happygallery.infra.booking.GuestRepository;
import com.personal.happygallery.infra.booking.PhoneVerificationRepository;
import com.personal.happygallery.infra.booking.SlotRepository;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class GuestBookingService {

    /** 인증 코드 유효 시간 (5분) */
    private static final int VERIFICATION_EXPIRE_MINUTES = 5;

    private final PhoneVerificationRepository phoneVerificationRepository;
    private final GuestRepository guestRepository;
    private final SlotRepository slotRepository;
    private final BookingRepository bookingRepository;
    private final BookingHistoryRepository bookingHistoryRepository;
    private final SlotManagementService slotManagementService;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public GuestBookingService(PhoneVerificationRepository phoneVerificationRepository,
                               GuestRepository guestRepository,
                               SlotRepository slotRepository,
                               BookingRepository bookingRepository,
                               BookingHistoryRepository bookingHistoryRepository,
                               SlotManagementService slotManagementService,
                               Clock clock) {
        this.phoneVerificationRepository = phoneVerificationRepository;
        this.guestRepository = guestRepository;
        this.slotRepository = slotRepository;
        this.bookingRepository = bookingRepository;
        this.bookingHistoryRepository = bookingHistoryRepository;
        this.slotManagementService = slotManagementService;
        this.clock = clock;
    }

    /**
     * 휴대폰 인증 코드를 생성·저장한다.
     * MVP: 실제 SMS 발송 없음. 반환된 code를 클라이언트가 직접 사용.
     *
     * @return 저장된 PhoneVerification (id + code 포함)
     */
    public PhoneVerification sendVerificationCode(String phone) {
        String code = String.format("%06d", random.nextInt(1_000_000));
        LocalDateTime expiresAt = LocalDateTime.now(clock)
                .plusMinutes(VERIFICATION_EXPIRE_MINUTES);
        PhoneVerification pv = new PhoneVerification(phone, code, expiresAt);
        return phoneVerificationRepository.save(pv);
    }

    /**
     * 게스트 예약을 생성한다.
     *
     * <ol>
     *   <li>인증 코드 검증 + 소모</li>
     *   <li>Guest upsert (동일 전화번호 재사용)</li>
     *   <li>슬롯 활성 여부 확인</li>
     *   <li>동일 슬롯 중복 예약 확인</li>
     *   <li>{@code confirmBooking} — 비관적 락 + 정원 + 버퍼 (동일 트랜잭션)</li>
     *   <li>Booking 저장</li>
     * </ol>
     */
    public Booking createGuestBooking(String phone, String code, String name,
                                      Long slotId, long depositAmount) {
        // 1. 인증 코드 검증 + 소모
        PhoneVerification pv = phoneVerificationRepository
                .findByPhoneAndCodeAndVerifiedFalseAndExpiresAtAfter(
                        phone, code, LocalDateTime.now(clock))
                .orElseThrow(PhoneVerificationFailedException::new);
        pv.markVerified();

        // 2. Guest upsert by phone
        Guest guest = guestRepository.findByPhone(phone)
                .orElseGet(() -> guestRepository.save(new Guest(name, phone)));
        guest.markPhoneVerified();

        // 3. 슬롯 활성 여부 확인 (락 전 빠른 체크)
        Slot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new NotFoundException("슬롯"));
        if (!slot.isActive()) {
            throw new SlotNotAvailableException();
        }

        // 4. 중복 예약 확인
        if (bookingRepository.existsBySlotIdAndGuestId(slotId, guest.getId())) {
            throw new DuplicateBookingException();
        }

        // 5. confirmBooking — 비관적 락 + 정원 증가 + 버퍼 비활성화 (동일 트랜잭션 내)
        slotManagementService.confirmBooking(slotId);

        // 6. Booking 생성
        long balanceAmount = slot.getBookingClass().getPrice() - depositAmount;
        String accessToken = UUID.randomUUID().toString().replace("-", "");
        Booking booking = new Booking(guest, slot, depositAmount, balanceAmount, accessToken);
        booking = bookingRepository.save(booking);

        // 7. 초기 이력 저장 (BOOKED)
        bookingHistoryRepository.save(
                new BookingHistory(booking, BookingHistoryAction.BOOKED, null, slot, "CUSTOMER", null));

        return booking;
    }
}
