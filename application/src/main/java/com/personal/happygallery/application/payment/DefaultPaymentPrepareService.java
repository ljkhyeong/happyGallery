package com.personal.happygallery.application.payment;

import com.personal.happygallery.application.customer.port.out.PhoneVerificationAttemptGuard;
import com.personal.happygallery.application.payment.port.in.PaymentPayload.BookingPayload;
import com.personal.happygallery.application.payment.port.in.PaymentPayload.OrderPayload;
import com.personal.happygallery.application.payment.port.in.PaymentPrepareUseCase;
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
        paymentAvailabilityGuard.requireAvailable();
        String guestPhone = guestPhone(command);
        if (guestPhone != null) {
            phoneVerificationAttemptGuard.check(KoreanPhoneNumber.required(guestPhone));
        }
        return transactionService.prepare(command);
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
