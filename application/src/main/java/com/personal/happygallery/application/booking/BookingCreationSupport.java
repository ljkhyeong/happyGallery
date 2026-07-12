package com.personal.happygallery.application.booking;

import com.personal.happygallery.application.booking.port.out.BookingStorePort;
import com.personal.happygallery.application.pass.PassCreditService;
import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.booking.BookingHistoryAction;
import com.personal.happygallery.domain.booking.DepositPaymentMethod;
import com.personal.happygallery.domain.booking.Slot;
import com.personal.happygallery.domain.error.PaymentMethodNotAllowedException;
import com.personal.happygallery.domain.notification.NotificationEventType;
import com.personal.happygallery.domain.pass.PassPurchase;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원·비회원 예약 생성의 공통 처리.
 *
 * <p>8회권 크레딧, 예약금 결제 수단, 예약 저장과 완료 후 처리를 담당한다.
 * 슬롯 잠금과 정원 변경은 {@link SlotCapacitySupport}가 담당한다.
 */
@Component
class BookingCreationSupport {

    private final BookingStorePort bookingStorePort;
    private final PassCreditService passCreditService;
    private final BookingSupport bookingSupport;

    BookingCreationSupport(BookingStorePort bookingStorePort,
                           PassCreditService passCreditService,
                           BookingSupport bookingSupport) {
        this.bookingStorePort = bookingStorePort;
        this.passCreditService = passCreditService;
        this.bookingSupport = bookingSupport;
    }

    /** 예약 생성 전 8회권 사용 가능 여부를 확인한다. */
    @Transactional(propagation = Propagation.MANDATORY)
    PassPurchase requireUsablePass(Long passId, Long ownerUserId) {
        return passCreditService.requireUsable(passId, ownerUserId);
    }

    /** 예약 저장 후 사용한 8회권 크레딧을 차감한다. */
    @Transactional(propagation = Propagation.MANDATORY)
    PassPurchase deductPassCredit(Long passId, Long ownerUserId, Long bookingId) {
        return passCreditService.deductCredit(passId, ownerUserId, bookingId);
    }

    /** 예약금 결제에서 허용하지 않는 계좌이체를 차단한다. */
    void requireValidDeposit(DepositPaymentMethod paymentMethod) {
        if (paymentMethod == DepositPaymentMethod.BANK_TRANSFER) {
            throw new PaymentMethodNotAllowedException();
        }
    }

    @Transactional(propagation = Propagation.MANDATORY)
    Booking save(Booking booking) {
        return bookingStorePort.save(booking);
    }

    /** BOOKED 이력을 기록하고 예약 완료 알림을 요청한다. */
    @Transactional(propagation = Propagation.MANDATORY)
    Booking complete(Booking booking, Slot slot) {
        bookingSupport.recordHistory(booking, BookingHistoryAction.BOOKED, null, slot, "CUSTOMER", null);
        bookingSupport.notifyBooker(booking, NotificationEventType.BOOKING_CONFIRMED);
        return booking;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    Booking saveAndComplete(Booking booking, Slot slot) {
        return complete(save(booking), slot);
    }
}
