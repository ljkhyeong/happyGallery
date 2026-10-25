package com.personal.happygallery.application.booking;

import com.personal.happygallery.application.booking.port.in.BookingRescheduleUseCase;
import com.personal.happygallery.application.booking.port.out.BookingReaderPort;
import com.personal.happygallery.application.booking.port.out.BookingStorePort;
import com.personal.happygallery.application.booking.port.out.SlotSchedulingSnapshot;
import com.personal.happygallery.domain.error.ChangeNotAllowedException;
import com.personal.happygallery.domain.error.DuplicateBookingException;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.time.TimeBoundary;
import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.booking.BookingHistoryAction;
import com.personal.happygallery.domain.booking.BookingHistoryActor;
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

    /** 비회원 예약 슬롯을 변경한다. access_token으로 예약을 조회·검증한다. */
    @Override
    public Booking rescheduleBooking(Long bookingId, String accessToken, Long newSlotId) {
        return reschedule(bookingSupport.findByToken(bookingId, accessToken), newSlotId);
    }

    /** 회원 예약 슬롯을 변경한다. accessToken 대신 userId 소유권으로 검증한다. */
    @Override
    public Booking rescheduleMemberBooking(Long bookingId, Long userId, Long newSlotId) {
        return reschedule(bookingSupport.findByIdAndUserId(bookingId, userId), newSlotId);
    }

    /**
     * <ol>
     *   <li>공통 검증(동일 슬롯 → 변경 마감 → 새 슬롯 가용·클래스 일치)</li>
     *   <li>전화번호 기준 중복 예약 체크</li>
     *   <li>잠금 뒤 새 슬롯 확정 + 기존 슬롯 반납</li>
     *   <li>RESCHEDULED 이력 저장 + 예약 업데이트 + 알림</li>
     * </ol>
     */
    private Booking reschedule(Booking booking, Long newSlotId) {
        requireReschedulable(booking, newSlotId);
        if (bookingReaderPort.existsBookedBySlotIdAndOwnerPhoneHmacAndIdNot(
                newSlotId, booking.getOwnerPhoneHmac(), booking.getId())) {
            throw new DuplicateBookingException();
        }

        Long oldSlotId = booking.getSlot().getId();
        slotCapacitySupport.lockClassesForSlots(List.of(oldSlotId, newSlotId));
        Slot newSlot = slotCapacitySupport.reserveCapacity(newSlotId, booking.getParticipantCount());
        Slot oldSlot = slotCapacitySupport.releaseCapacity(oldSlotId, booking.getParticipantCount());
        booking.reschedule(newSlot);

        bookingSupport.recordHistory(booking, BookingHistoryAction.RESCHEDULED,
                oldSlot, newSlot, BookingHistoryActor.CUSTOMER, null, null);

        Booking saved = bookingStorePort.save(booking);
        bookingSupport.notifyBooker(booking, NotificationEventType.BOOKING_RESCHEDULED);
        return saved;
    }

    private void requireReschedulable(Booking booking, Long newSlotId) {
        if (booking.getSlot().getId().equals(newSlotId)) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "현재 예약된 슬롯과 동일합니다.");
        }

        if (!TimeBoundary.isChangeable(booking.getSlot().getStartAt(), clock)) {
            throw new ChangeNotAllowedException();
        }

        SlotSchedulingSnapshot newSlot = slotCapacitySupport.requireAvailableSlot(newSlotId);
        if (!booking.getBookingClass().getId().equals(newSlot.classId())) {
            throw new HappyGalleryException(
                    ErrorCode.INVALID_INPUT,
                    "같은 클래스의 슬롯으로만 예약을 변경할 수 있습니다.");
        }
    }
}
