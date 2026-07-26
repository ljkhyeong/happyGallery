package com.personal.happygallery.application.payment.context.booking;

import com.personal.happygallery.application.booking.port.out.SlotReaderPort;
import com.personal.happygallery.application.payment.GuestPaymentVerificationService;
import com.personal.happygallery.application.payment.context.PaymentPreparer;
import com.personal.happygallery.application.payment.context.PreparedPaymentPayload.PreparedBookingPayload;
import com.personal.happygallery.application.payment.port.in.AuthContext;
import com.personal.happygallery.application.payment.port.in.PaymentPayload;
import com.personal.happygallery.application.payment.port.in.PaymentPayload.BookingPayload;
import com.personal.happygallery.domain.booking.DepositCalculator;
import com.personal.happygallery.domain.booking.DepositCalculator.BookingAmounts;
import com.personal.happygallery.domain.booking.DepositPaymentMethod;
import com.personal.happygallery.domain.booking.Slot;
import com.personal.happygallery.domain.booking.SlotCapacity;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.error.PaymentMethodNotAllowedException;
import com.personal.happygallery.domain.payment.PaymentContext;
import com.personal.happygallery.domain.user.KoreanPhoneNumber;
import com.personal.happygallery.domain.user.PersonalName;
import org.springframework.stereotype.Component;

@Component
public class BookingPreparer implements PaymentPreparer {

    private final SlotReaderPort slotReader;
    private final GuestPaymentVerificationService guestPaymentVerification;

    public BookingPreparer(
            SlotReaderPort slotReader,
            GuestPaymentVerificationService guestPaymentVerification) {
        this.slotReader = slotReader;
        this.guestPaymentVerification = guestPaymentVerification;
    }

    @Override
    public PaymentContext context() {
        return PaymentContext.BOOKING;
    }

    @Override
    public PreparedPayment prepare(String paymentOrderId, PaymentPayload payload, AuthContext auth) {
        if (!(payload instanceof BookingPayload bp)) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "예약 결제 payload가 아닙니다.");
        }
        if (bp.slotId() == null) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "예약 슬롯이 지정되지 않았습니다.");
        }
        SlotCapacity.requireValidParticipantCount(bp.participantCount());

        if (bp.passId() != null) {
            if (!auth.isMember() || !auth.userId().equals(bp.userId())) {
                throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "8회권 사용 예약은 회원 인증이 필요합니다.");
            }
            if (bp.participantCount() != 1) {
                throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "8회권 예약은 1명만 예약할 수 있습니다.");
            }
            return new PreparedPayment(0L, preparedForMember(bp, 0L, 0L));
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
        SlotCapacity.checkAvailable(slot.getBookedCount(), bp.participantCount());
        BookingAmounts amounts = DepositCalculator.calculate(slot, bp.participantCount());
        long depositAmount = amounts.depositAmount();
        long balanceAmount = amounts.balanceAmount();
        if (auth.isMember()) {
            return new PreparedPayment(
                    depositAmount, preparedForMember(bp, depositAmount, balanceAmount));
        }
        String phone = KoreanPhoneNumber.required(bp.phone());
        String name = PersonalName.required(bp.name());
        String guestVerificationProof = guestPaymentVerification.consumeAndIssue(
                PaymentContext.BOOKING, paymentOrderId, phone, bp.verificationCode());
        PreparedBookingPayload prepared = new PreparedBookingPayload(
                null,
                phone,
                guestVerificationProof,
                name,
                bp.slotId(),
                null,
                bp.paymentMethod(),
                depositAmount,
                balanceAmount,
                bp.participantCount());
        return new PreparedPayment(depositAmount, prepared);
    }

    private PreparedBookingPayload preparedForMember(
            BookingPayload payload, long depositAmount, long balanceAmount) {
        return new PreparedBookingPayload(
                payload.userId(),
                null,
                null,
                null,
                payload.slotId(),
                payload.passId(),
                payload.paymentMethod(),
                depositAmount,
                balanceAmount,
                payload.participantCount());
    }
}
