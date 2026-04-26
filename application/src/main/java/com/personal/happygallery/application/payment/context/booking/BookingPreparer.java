package com.personal.happygallery.application.payment.context.booking;

import com.personal.happygallery.application.booking.port.out.SlotReaderPort;
import com.personal.happygallery.application.payment.context.PaymentPreparer;
import com.personal.happygallery.application.payment.port.in.AuthContext;
import com.personal.happygallery.application.payment.port.in.PaymentPayload;
import com.personal.happygallery.application.payment.port.in.PaymentPayload.BookingPayload;
import com.personal.happygallery.domain.booking.DepositCalculator;
import com.personal.happygallery.domain.booking.DepositPaymentMethod;
import com.personal.happygallery.domain.booking.Slot;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.error.PaymentMethodNotAllowedException;
import com.personal.happygallery.domain.payment.PaymentContext;
import org.springframework.stereotype.Component;

@Component
public class BookingPreparer implements PaymentPreparer {

    private final SlotReaderPort slotReader;

    public BookingPreparer(SlotReaderPort slotReader) {
        this.slotReader = slotReader;
    }

    @Override
    public PaymentContext context() {
        return PaymentContext.BOOKING;
    }

    @Override
    public long calculateAmount(PaymentPayload payload, AuthContext auth) {
        if (!(payload instanceof BookingPayload bp)) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "예약 결제 payload가 아닙니다.");
        }
        if (bp.slotId() == null) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "예약 슬롯이 지정되지 않았습니다.");
        }

        if (bp.passId() != null) {
            if (!auth.isMember() || !auth.userId().equals(bp.userId())) {
                throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "8회권 사용 예약은 회원 인증이 필요합니다.");
            }
            return 0L;
        }

        if (bp.paymentMethod() == null) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "예약금 결제 수단을 선택하세요.");
        }
        if (bp.paymentMethod() == DepositPaymentMethod.BANK_TRANSFER) {
            throw new PaymentMethodNotAllowedException();
        }

        if (auth.isMember()) {
            if (bp.userId() == null || !bp.userId().equals(auth.userId())) {
                throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "회원 정보가 인증과 일치하지 않습니다.");
            }
        } else {
            if (bp.phone() == null || bp.verificationCode() == null || bp.name() == null) {
                throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "비회원 예약은 휴대폰 인증이 필요합니다.");
            }
        }

        Slot slot = slotReader.findById(bp.slotId())
                .orElseThrow(NotFoundException.supplier("슬롯"));
        return DepositCalculator.of(slot);
    }
}
