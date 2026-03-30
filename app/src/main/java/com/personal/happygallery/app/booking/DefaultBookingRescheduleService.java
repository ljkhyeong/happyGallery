package com.personal.happygallery.app.booking;

import com.personal.happygallery.app.booking.port.in.BookingRescheduleUseCase;
import com.personal.happygallery.app.booking.port.out.BookingReaderPort;
import com.personal.happygallery.app.booking.port.out.BookingStorePort;
import com.personal.happygallery.app.booking.port.out.SlotReaderPort;
import com.personal.happygallery.domain.error.ChangeNotAllowedException;
import com.personal.happygallery.domain.error.DuplicateBookingException;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.error.SlotNotAvailableException;
import com.personal.happygallery.domain.time.TimeBoundary;
import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.booking.BookingHistoryAction;
import com.personal.happygallery.domain.booking.Slot;
import com.personal.happygallery.domain.notification.NotificationEventType;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DefaultBookingRescheduleService implements BookingRescheduleUseCase {

    private final BookingReaderPort bookingReaderPort;
    private final BookingStorePort bookingStorePort;
    private final SlotReaderPort slotReaderPort;
    private final BookingSlotSupport creationSupport;
    private final BookingSupport bookingSupport;
    private final Clock clock;

    public DefaultBookingRescheduleService(BookingReaderPort bookingReaderPort,
                                    BookingStorePort bookingStorePort,
                                    SlotReaderPort slotReaderPort,
                                    BookingSlotSupport creationSupport,
                                    BookingSupport bookingSupport,
                                    Clock clock) {
        this.bookingReaderPort = bookingReaderPort;
        this.bookingStorePort = bookingStorePort;
        this.slotReaderPort = slotReaderPort;
        this.creationSupport = creationSupport;
        this.bookingSupport = bookingSupport;
        this.clock = clock;
    }

    /**
     * 비회원 예약 슬롯을 변경한다.
     *
     * <ol>
     *   <li>access_token으로 예약 조회 및 검증</li>
     *   <li>공통 검증 + 새 슬롯 확정 + 기존 슬롯 반납</li>
     *   <li>중복 예약 체크 (게스트 기준)</li>
     *   <li>RESCHEDULED 이력 저장 + 예약 업데이트 + 알림</li>
     * </ol>
     */
    public Booking rescheduleBooking(Long bookingId, String accessToken, Long newSlotId) {
        Booking booking = bookingSupport.findByToken(bookingId, accessToken);
        if (booking.getGuest() != null &&
                bookingReaderPort.existsBySlotIdAndGuestIdAndIdNot(
                        newSlotId, booking.getGuest().getId(), bookingId)) {
            throw new DuplicateBookingException();
        }
        return rescheduleInternal(booking, newSlotId);
    }

    /**
     * 회원 예약 슬롯을 변경한다.
     * accessToken 대신 userId 소유권으로 검증한다.
     */
    public Booking rescheduleMemberBooking(Long bookingId, Long userId, Long newSlotId) {
        Booking booking = bookingSupport.findByIdAndUserId(bookingId, userId);
        if (bookingReaderPort.existsBySlotIdAndUserIdAndIdNot(
                newSlotId, userId, bookingId)) {
            throw new DuplicateBookingException();
        }
        return rescheduleInternal(booking, newSlotId);
    }

    private Booking rescheduleInternal(Booking booking, Long newSlotId) {
        // 1. 동일 슬롯 변경 차단
        if (booking.getSlot().getId().equals(newSlotId)) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "현재 예약된 슬롯과 동일합니다.");
        }

        // 2. 시간 경계 정책 — 슬롯 시작 1시간 전까지만 변경 가능
        if (!TimeBoundary.isChangeable(booking.getSlot().getStartAt(), clock)) {
            throw new ChangeNotAllowedException();
        }

        // 3. 새 슬롯 빠른 체크 (락 전 — fast-fail)
        Slot newSlot = slotReaderPort.findById(newSlotId)
                .orElseThrow(() -> new NotFoundException("슬롯"));
        if (!newSlot.isActive()) {
            throw new SlotNotAvailableException();
        }

        // 4. 새 슬롯 확정 — 비관적 락 + isActive 재확인 + booked_count++ + 버퍼 비활성화
        creationSupport.lockSlotCapacity(newSlotId);

        // 5. 기존 슬롯 반납 — 비관적 락 + booked_count--
        // 주의: lockSlotCapacity(new) 후 releaseSlotCapacity(old) 순서 고정
        //       swap 변경 시 deadlock 이론적 가능 (ADR-0006 참고)
        Slot oldSlot = creationSupport.releaseSlotCapacity(booking.getSlot().getId());

        // 6. 이력 저장 (append-only)
        bookingSupport.recordHistory(booking, BookingHistoryAction.RESCHEDULED,
                oldSlot, newSlot, "CUSTOMER", null);

        // 7. 예약 업데이트 — @Version 충돌 시 OptimisticLockingFailureException → 409 BOOKING_CONFLICT
        booking.reschedule(newSlot);
        Booking saved = bookingStorePort.save(booking);

        // 8. 예약 변경 알림
        bookingSupport.notifyBooker(booking, NotificationEventType.BOOKING_RESCHEDULED);

        return saved;
    }
}
