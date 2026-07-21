package com.personal.happygallery.application.booking;

import com.personal.happygallery.application.booking.port.in.BookingRescheduleUseCase;
import com.personal.happygallery.application.booking.port.out.BookingReaderPort;
import com.personal.happygallery.application.booking.port.out.BookingStorePort;
import com.personal.happygallery.domain.error.ChangeNotAllowedException;
import com.personal.happygallery.domain.error.DuplicateBookingException;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.time.TimeBoundary;
import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.booking.BookingHistoryAction;
import com.personal.happygallery.domain.booking.Slot;
import com.personal.happygallery.domain.notification.NotificationEventType;
import java.time.Clock;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DefaultBookingRescheduleService implements BookingRescheduleUseCase {

    private final BookingReaderPort bookingReaderPort;
    private final BookingStorePort bookingStorePort;
    private final SlotCapacitySupport slotCapacitySupport;
    private final BookingSupport bookingSupport;
    private final Clock clock;

    public DefaultBookingRescheduleService(BookingReaderPort bookingReaderPort,
                                           BookingStorePort bookingStorePort,
                                           SlotCapacitySupport slotCapacitySupport,
                                           BookingSupport bookingSupport,
                                           Clock clock) {
        this.bookingReaderPort = bookingReaderPort;
        this.bookingStorePort = bookingStorePort;
        this.slotCapacitySupport = slotCapacitySupport;
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
    @Override
    public Booking rescheduleBooking(Long bookingId, String accessToken, Long newSlotId) {
        Booking booking = bookingSupport.findByToken(bookingId, accessToken);
        RescheduleSlots slots = prepareReschedule(booking, newSlotId);
        if (booking.getGuest() != null &&
                bookingReaderPort.existsBookedBySlotIdAndGuestIdAndIdNot(
                        newSlotId, booking.getGuest().getId(), bookingId)) {
            throw new DuplicateBookingException();
        }
        return applyReschedule(booking, slots);
    }

    /**
     * 회원 예약 슬롯을 변경한다.
     * accessToken 대신 userId 소유권으로 검증한다.
     */
    @Override
    public Booking rescheduleMemberBooking(Long bookingId, Long userId, Long newSlotId) {
        Booking booking = bookingSupport.findByIdAndUserId(bookingId, userId);
        RescheduleSlots slots = prepareReschedule(booking, newSlotId);
        if (bookingReaderPort.existsBookedBySlotIdAndUserIdAndIdNot(
                newSlotId, userId, bookingId)) {
            throw new DuplicateBookingException();
        }
        return applyReschedule(booking, slots);
    }

    private RescheduleSlots prepareReschedule(Booking booking, Long newSlotId) {
        if (booking.getSlot().getId().equals(newSlotId)) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "현재 예약된 슬롯과 동일합니다.");
        }

        if (!TimeBoundary.isChangeable(booking.getSlot().getStartAt(), clock)) {
            throw new ChangeNotAllowedException();
        }

        Slot oldSlot = booking.getSlot();
        Slot newSlot = slotCapacitySupport.requireAvailableSlot(newSlotId);
        return new RescheduleSlots(oldSlot, newSlot);
    }

    private Booking applyReschedule(Booking booking, RescheduleSlots slots) {
        slotCapacitySupport.lockClassesForSlots(List.of(slots.oldSlot().getId(), slots.newSlot().getId()));
        booking.reschedule(slots.newSlot());

        Slot newSlot = slotCapacitySupport.reserveCapacity(slots.newSlot().getId());
        Slot oldSlot = slotCapacitySupport.releaseCapacity(slots.oldSlot().getId());

        bookingSupport.recordHistory(booking, BookingHistoryAction.RESCHEDULED,
                oldSlot, newSlot, "CUSTOMER", null);

        Booking saved = bookingStorePort.save(booking);
        bookingSupport.notifyBooker(booking, NotificationEventType.BOOKING_RESCHEDULED);
        return saved;
    }

    private record RescheduleSlots(Slot oldSlot, Slot newSlot) {}
}
