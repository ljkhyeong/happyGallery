package com.personal.happygallery.application.booking;

import com.personal.happygallery.application.booking.port.out.BookingReaderPort;
import com.personal.happygallery.application.booking.port.out.BookingStorePort;
import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.booking.BookingHistoryAction;
import com.personal.happygallery.domain.booking.DepositPaymentMethod;
import com.personal.happygallery.domain.booking.Slot;
import com.personal.happygallery.domain.error.DuplicateBookingException;
import com.personal.happygallery.domain.error.PaymentMethodNotAllowedException;
import com.personal.happygallery.domain.notification.NotificationEventType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원·비회원 예약 생성의 공통 처리.
 *
 * <p>슬롯 확보, 예약금 결제 수단 검증, 예약 저장과 완료 후 처리를 담당한다.
 */
@Component
class BookingCreationSupport {

    private final BookingStorePort bookingStorePort;
    private final BookingReaderPort bookingReaderPort;
    private final SlotCapacitySupport slotCapacitySupport;
    private final BookingSupport bookingSupport;

    BookingCreationSupport(BookingStorePort bookingStorePort,
                           BookingReaderPort bookingReaderPort,
                           SlotCapacitySupport slotCapacitySupport,
                           BookingSupport bookingSupport) {
        this.bookingStorePort = bookingStorePort;
        this.bookingReaderPort = bookingReaderPort;
        this.slotCapacitySupport = slotCapacitySupport;
        this.bookingSupport = bookingSupport;
    }

    /** 빠른 가용성 확인 뒤 중복 예약을 차단하고 잠금 상태에서 정원을 확보한다. */
    Slot reserveSlot(String ownerPhoneHmac, Long slotId, int participantCount) {
        slotCapacitySupport.requireAvailableSlot(slotId);
        if (bookingReaderPort.existsBookedBySlotIdAndOwnerPhoneHmac(
                slotId, ownerPhoneHmac)) {
            throw new DuplicateBookingException();
        }
        return slotCapacitySupport.reserveCapacity(slotId, participantCount);
    }

    /** 예약금 결제에서 허용하지 않는 계좌이체를 차단한다. */
    void requireValidDeposit(DepositPaymentMethod paymentMethod) {
        if (paymentMethod == DepositPaymentMethod.BANK_TRANSFER) {
            throw new PaymentMethodNotAllowedException();
        }
    }

    /** BOOKED 이력을 기록하고 예약 완료 알림을 요청한다. */
    @Transactional(propagation = Propagation.MANDATORY)
    Booking complete(Booking booking, Slot slot) {
        bookingSupport.recordHistory(booking, BookingHistoryAction.BOOKED, null, slot, "CUSTOMER", null, null);
        bookingSupport.notifyBooker(booking, NotificationEventType.BOOKING_CONFIRMED);
        return booking;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    Booking saveAndComplete(Booking booking, Slot slot) {
        return complete(bookingStorePort.save(booking), slot);
    }
}
