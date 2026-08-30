package com.personal.happygallery.application.payment;

import com.personal.happygallery.application.customer.port.out.PhoneVerificationAttemptGuard;
import com.personal.happygallery.application.payment.port.in.PaymentPayload.BookingPayload;
import com.personal.happygallery.application.payment.port.in.PaymentPayload.OrderPayload;
import com.personal.happygallery.application.payment.port.in.PaymentPrepareUseCase;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.payment.PaymentContext;
import com.personal.happygallery.domain.user.KoreanPhoneNumber;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DefaultPaymentPrepareService implements PaymentPrepareUseCase {

    private final PublicPaymentAvailabilityGuard paymentAvailabilityGuard;
    private final PhoneVerificationAttemptGuard phoneVerificationAttemptGuard;
    private final PaymentPrepareTransactionService transactionService;

    public DefaultPaymentPrepareService(
            PublicPaymentAvailabilityGuard paymentAvailabilityGuard,
            PhoneVerificationAttemptGuard phoneVerificationAttemptGuard,
            PaymentPrepareTransactionService transactionService
    ) {
        this.paymentAvailabilityGuard = paymentAvailabilityGuard;
        this.phoneVerificationAttemptGuard = phoneVerificationAttemptGuard;
        this.transactionService = transactionService;
    }

    @Override
    @Transactional(propagation = Propagation.NEVER)
    public PrepareResult prepare(PrepareCommand command) {
        requireValidGuestOrderActor(command);
        paymentAvailabilityGuard.requireAvailable();
        String guestPhone = guestPhone(command);
        if (guestPhone != null) {
            phoneVerificationAttemptGuard.check(KoreanPhoneNumber.required(guestPhone));
        }
        return transactionService.prepare(command);
    }

    private void requireValidGuestOrderActor(PrepareCommand command) {
        if (!command.auth().isMember()
                && command.context() == PaymentContext.ORDER
                && command.payload() instanceof OrderPayload order
                && order.userId() != null) {
            throw new HappyGalleryException(
                    ErrorCode.INVALID_INPUT, "비회원 주문은 회원 정보를 지정할 수 없습니다.");
        }
    }

    private String guestPhone(PrepareCommand command) {
        if (command.auth().isMember()) {
            return null;
        }
        if (command.context() == PaymentContext.ORDER
                && command.payload() instanceof OrderPayload order) {
            return order.phone();
        }
        if (command.context() == PaymentContext.BOOKING
                && command.payload() instanceof BookingPayload booking) {
            return booking.phone();
        }
        return null;
    }
}
