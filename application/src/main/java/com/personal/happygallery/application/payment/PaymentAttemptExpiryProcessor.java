package com.personal.happygallery.application.payment;

import com.personal.happygallery.application.payment.context.PreparedPaymentPayload;
import com.personal.happygallery.application.payment.port.out.PaymentAttemptReaderPort;
import com.personal.happygallery.application.payment.port.out.PaymentAttemptStorePort;
import com.personal.happygallery.domain.payment.PaymentAttempt;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
class PaymentAttemptExpiryProcessor {

    private final PaymentAttemptReaderPort attemptReader;
    private final PaymentAttemptStorePort attemptStore;
    private final OrderPaymentBenefitReservationService benefitReservationService;
    private final Clock clock;

    PaymentAttemptExpiryProcessor(PaymentAttemptReaderPort attemptReader,
                                  PaymentAttemptStorePort attemptStore,
                                  OrderPaymentBenefitReservationService benefitReservationService,
                                  Clock clock) {
        this.attemptReader = attemptReader;
        this.attemptStore = attemptStore;
        this.benefitReservationService = benefitReservationService;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    boolean expire(Long attemptId, LocalDateTime cutoff) {
        PaymentAttempt attempt = attemptReader.findByIdForUpdate(attemptId).orElse(null);
        if (attempt == null) {
            return false;
        }
        PreparedPaymentPayload payload = benefitReservationService.readPayloadForRelease(attempt);
        if (!attempt.expirePendingBefore(cutoff)) {
            return false;
        }
        attemptStore.save(attempt);
        if (payload != null) {
            benefitReservationService.release(
                    attempt, payload, LocalDateTime.now(clock));
        }
        return true;
    }
}
