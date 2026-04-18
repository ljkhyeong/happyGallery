package com.personal.happygallery.application.booking;

import com.personal.happygallery.application.booking.port.out.BookingStorePort;
import com.personal.happygallery.application.booking.port.out.SlotReaderPort;
import com.personal.happygallery.application.pass.port.in.PassCreditUseCase;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.error.PaymentMethodNotAllowedException;
import com.personal.happygallery.domain.error.SlotNotAvailableException;
import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.booking.BookingHistoryAction;
import com.personal.happygallery.domain.booking.DepositPaymentMethod;
import com.personal.happygallery.domain.booking.Slot;
import com.personal.happygallery.domain.notification.NotificationEventType;
import com.personal.happygallery.domain.pass.PassPurchase;
import java.time.Clock;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * guest/member 예약 생성의 공통 orchestration.
 *
 * <p>슬롯 검증, 정원 확보, 8회권 크레딧 차감, 예약금 검증, 저장+이력+알림 등
 * 신원 확인 방식과 무관한 공통 단계를 모아 둔다.
 */
@Component
class BookingSlotSupport {

    private final SlotReaderPort slotReaderPort;
    private final SlotBookingSupport slotBookingSupport;
    private final BookingStorePort bookingStorePort;
    private final PassCreditUseCase passCreditPort;
    private final BookingSupport bookingSupport;
    private final Clock clock;

    BookingSlotSupport(SlotReaderPort slotReaderPort,
                       SlotBookingSupport slotBookingSupport,
                       BookingStorePort bookingStorePort,
                       PassCreditUseCase passCreditPort,
                       BookingSupport bookingSupport,
                       Clock clock) {
        this.slotReaderPort = slotReaderPort;
        this.slotBookingSupport = slotBookingSupport;
        this.bookingStorePort = bookingStorePort;
        this.passCreditPort = passCreditPort;
        this.bookingSupport = bookingSupport;
        this.clock = clock;
    }

    /** 슬롯 조회 + 활성 여부 확인 (락 전 빠른 체크). */
    Slot loadActiveSlot(Long slotId) {
        Slot slot = slotReaderPort.findById(slotId)
                .orElseThrow(NotFoundException.supplier("슬롯"));
        if (!slot.isActive()) {
            throw new SlotNotAvailableException();
        }
        return slot;
    }

    /** 비관적 락 + 정원 증가 + 버퍼 비활성화. 중복 체크 이후에 호출한다. */
    @Transactional(propagation = Propagation.MANDATORY)
    void lockSlotCapacity(Long slotId) {
        slotBookingSupport.confirmBooking(slotId);
    }

    /** 비관적 락 + 정원 감소. 취소·변경 시 기존 슬롯을 반납한다. */
    @Transactional(propagation = Propagation.MANDATORY)
    Slot releaseSlotCapacity(Long slotId) {
        return slotBookingSupport.releaseSlotCapacity(slotId);
    }

    /**
     * 8회권 크레딧 차감을 PassCreditUseCase에 위임한다.
     *
     * @param passId       8회권 ID
     * @param ownerUserId  소유자 회원 ID (회원 예약일 때 non-null, 게스트 예약일 때 null)
     * @return 차감된 PassPurchase
     */
    @Transactional(propagation = Propagation.MANDATORY)
    PassPurchase deductPassCredit(Long passId, Long ownerUserId) {
        return passCreditPort.deductCredit(passId, ownerUserId);
    }

    /** 예약금 결제 수단 검증 — 계좌이체 차단. */
    void requireValidDeposit(DepositPaymentMethod paymentMethod) {
        if (paymentMethod == DepositPaymentMethod.BANK_TRANSFER) {
            throw new PaymentMethodNotAllowedException();
        }
    }

    /** 예약 저장 → BOOKED 이력 기록 → 예약 완료 알림. */
    @Transactional(propagation = Propagation.MANDATORY)
    Booking saveAndComplete(Booking booking, Slot slot) {
        booking = bookingStorePort.save(booking);
        bookingSupport.recordHistory(booking, BookingHistoryAction.BOOKED, null, slot, "CUSTOMER", null);
        bookingSupport.notifyBooker(booking, NotificationEventType.BOOKING_CONFIRMED);
        return booking;
    }
}
