package com.personal.happygallery.app.booking;

import com.personal.happygallery.app.booking.port.out.BookingReaderPort;
import com.personal.happygallery.app.booking.port.out.BookingStorePort;
import com.personal.happygallery.app.booking.port.out.SlotReaderPort;
import com.personal.happygallery.app.booking.port.out.SlotStorePort;
import com.personal.happygallery.common.error.ChangeNotAllowedException;
import com.personal.happygallery.common.error.DuplicateBookingException;
import com.personal.happygallery.common.error.ErrorCode;
import com.personal.happygallery.common.error.HappyGalleryException;
import com.personal.happygallery.common.error.NotFoundException;
import com.personal.happygallery.common.error.SlotNotAvailableException;
import com.personal.happygallery.common.time.TimeBoundary;
import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.booking.BookingHistoryAction;
import com.personal.happygallery.domain.booking.BookingStatus;
import com.personal.happygallery.domain.booking.Slot;
import com.personal.happygallery.domain.notification.NotificationEventType;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class BookingRescheduleService {

    private final BookingReaderPort bookingReaderPort;
    private final BookingStorePort bookingStorePort;
    private final SlotReaderPort slotReaderPort;
    private final SlotStorePort slotStorePort;
    private final SlotManagementService slotManagementService;
    private final BookingSupport bookingSupport;
    private final Clock clock;

    public BookingRescheduleService(BookingReaderPort bookingReaderPort,
                                    BookingStorePort bookingStorePort,
                                    SlotReaderPort slotReaderPort,
                                    SlotStorePort slotStorePort,
                                    SlotManagementService slotManagementService,
                                    BookingSupport bookingSupport,
                                    Clock clock) {
        this.bookingReaderPort = bookingReaderPort;
        this.bookingStorePort = bookingStorePort;
        this.slotReaderPort = slotReaderPort;
        this.slotStorePort = slotStorePort;
        this.slotManagementService = slotManagementService;
        this.bookingSupport = bookingSupport;
        this.clock = clock;
    }

    /**
     * 비회원 예약 슬롯을 변경한다.
     *
     * <ol>
     *   <li>access_token으로 예약 조회 및 검증</li>
     *   <li>상태/동일 슬롯/시간 경계 정책 체크</li>
     *   <li>새 슬롯 활성 여부 + 중복 예약 체크</li>
     *   <li>새 슬롯 확정: 비관적 락 + 정원 + 버퍼</li>
     *   <li>기존 슬롯 반납: 비관적 락 + booked_count--</li>
     *   <li>RESCHEDULED 이력 저장</li>
     *   <li>bookings.slot_id 업데이트 (@Version 낙관적 락)</li>
     * </ol>
     */
    public Booking rescheduleBooking(Long bookingId, String accessToken, Long newSlotId) {

        // 1. 예약 로드 (accessToken 검증)
        Booking booking = bookingSupport.findByToken(bookingId, accessToken);

        // 2. 상태 체크 — BOOKED 상태만 변경 가능
        if (booking.getStatus() != BookingStatus.BOOKED) {
            throw new ChangeNotAllowedException();
        }

        // 3. 동일 슬롯 변경 차단
        if (booking.getSlot().getId().equals(newSlotId)) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "현재 예약된 슬롯과 동일합니다.");
        }

        // 4. 시간 경계 정책 — 슬롯 시작 1시간 전까지만 변경 가능
        if (!TimeBoundary.isChangeable(booking.getSlot().getStartAt(), clock)) {
            throw new ChangeNotAllowedException();
        }

        // 5. 새 슬롯 빠른 체크 (락 전 — fast-fail)
        Slot newSlot = slotReaderPort.findById(newSlotId)
                .orElseThrow(() -> new NotFoundException("슬롯"));
        if (!newSlot.isActive()) {
            throw new SlotNotAvailableException();
        }

        // 6. 중복 예약 체크 (현재 booking 자신 제외)
        if (booking.getGuest() != null &&
                bookingReaderPort.existsBySlotIdAndGuestIdAndIdNot(
                        newSlotId, booking.getGuest().getId(), bookingId)) {
            throw new DuplicateBookingException();
        }

        // 7. 새 슬롯 확정 — 비관적 락 + isActive 재확인 + booked_count++ + 버퍼 비활성화
        slotManagementService.confirmBooking(newSlotId);

        // 8. 기존 슬롯 반납 — 비관적 락 + booked_count--
        // 주의: confirmBooking(new) 후 findByIdWithLock(old) 순서 고정
        //       swap 변경 시 deadlock 이론적 가능 (ADR-0006 참고)
        Slot oldSlot = slotReaderPort.findByIdWithLock(booking.getSlot().getId())
                .orElseThrow(() -> new NotFoundException("슬롯"));
        oldSlot.decrementBookedCount();
        slotStorePort.save(oldSlot);

        // 9. 이력 저장 (append-only)
        bookingSupport.recordHistory(booking, BookingHistoryAction.RESCHEDULED,
                oldSlot, newSlot, "CUSTOMER", null);

        // 10. 예약 업데이트 — @Version 충돌 시 OptimisticLockingFailureException → 409 BOOKING_CONFLICT
        booking.reschedule(newSlot);
        Booking saved = bookingStorePort.save(booking);

        // 11. 예약 변경 알림
        bookingSupport.notifyBookingGuest(booking, NotificationEventType.BOOKING_RESCHEDULED);

        return saved;
    }

    /**
     * 회원 예약 슬롯을 변경한다.
     * accessToken 대신 userId 소유권으로 검증한다.
     */
    public Booking rescheduleMemberBooking(Long bookingId, Long userId, Long newSlotId) {

        // 1. 예약 로드 (userId 소유권 검증)
        Booking booking = bookingSupport.findByIdAndUserId(bookingId, userId);

        // 2. 상태 체크 — BOOKED 상태만 변경 가능
        if (booking.getStatus() != BookingStatus.BOOKED) {
            throw new ChangeNotAllowedException();
        }

        // 3. 동일 슬롯 변경 차단
        if (booking.getSlot().getId().equals(newSlotId)) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "현재 예약된 슬롯과 동일합니다.");
        }

        // 4. 시간 경계 정책 — 슬롯 시작 1시간 전까지만 변경 가능
        if (!TimeBoundary.isChangeable(booking.getSlot().getStartAt(), clock)) {
            throw new ChangeNotAllowedException();
        }

        // 5. 새 슬롯 빠른 체크 (락 전 — fast-fail)
        Slot newSlot = slotReaderPort.findById(newSlotId)
                .orElseThrow(() -> new NotFoundException("슬롯"));
        if (!newSlot.isActive()) {
            throw new SlotNotAvailableException();
        }

        // 6. 중복 예약 체크 (현재 booking 자신 제외)
        if (bookingReaderPort.existsBySlotIdAndUserIdAndIdNot(
                newSlotId, userId, bookingId)) {
            throw new DuplicateBookingException();
        }

        // 7. 새 슬롯 확정
        slotManagementService.confirmBooking(newSlotId);

        // 8. 기존 슬롯 반납
        Slot oldSlot = slotReaderPort.findByIdWithLock(booking.getSlot().getId())
                .orElseThrow(() -> new NotFoundException("슬롯"));
        oldSlot.decrementBookedCount();
        slotStorePort.save(oldSlot);

        // 9. 이력 저장
        bookingSupport.recordHistory(booking, BookingHistoryAction.RESCHEDULED,
                oldSlot, newSlot, "CUSTOMER", null);

        // 10. 예약 업데이트
        booking.reschedule(newSlot);
        Booking saved = bookingStorePort.save(booking);

        // 11. 예약 변경 알림
        bookingSupport.notifyBookingUser(booking, NotificationEventType.BOOKING_RESCHEDULED);

        return saved;
    }
}
