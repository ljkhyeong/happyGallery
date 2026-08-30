package com.personal.happygallery.application.payment;

import com.personal.happygallery.application.payment.context.PreparedPaymentPayload;
import com.personal.happygallery.application.payment.port.in.AuthContext;
import com.personal.happygallery.application.payment.port.in.PaymentAbandonUseCase;
import com.personal.happygallery.application.payment.port.out.PaymentAttemptReaderPort;
import com.personal.happygallery.application.payment.port.out.PaymentAttemptStorePort;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.payment.PaymentAttempt;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DefaultPaymentAbandonService implements PaymentAbandonUseCase {

    private final PaymentAttemptReaderPort attemptReader;
    private final PaymentAttemptStorePort attemptStore;
    private final PaymentAttemptAccessVerifier accessVerifier;
    private final OrderPaymentBenefitReservationService benefits;
    private final Clock clock;

    public DefaultPaymentAbandonService(PaymentAttemptReaderPort attemptReader,
                                        PaymentAttemptStorePort attemptStore,
                                        PaymentAttemptAccessVerifier accessVerifier,
                                        OrderPaymentBenefitReservationService benefits,
                                        Clock clock) {
        this.attemptReader = attemptReader;
        this.attemptStore = attemptStore;
        this.accessVerifier = accessVerifier;
        this.benefits = benefits;
        this.clock = clock;
    }

    @Override
    public void abandon(String orderId, AuthContext auth, String statusToken) {
        PaymentAttempt attempt = attemptReader.findByOrderIdExternalForUpdate(orderId)
                .orElseThrow(() -> new NotFoundException("결제"));
        accessVerifier.requireCustomerAccess(attempt, auth, statusToken);
        PreparedPaymentPayload payload = benefits.readPayloadForRelease(attempt);
        if (!attempt.abandonPending()) {
            return;
        }
        attemptStore.save(attempt);
        if (payload != null) {
            benefits.release(attempt, payload, LocalDateTime.now(clock));
        }
    }
}
