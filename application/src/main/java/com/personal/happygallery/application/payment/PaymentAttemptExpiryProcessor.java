package com.personal.happygallery.application.payment;

import com.personal.happygallery.application.payment.port.out.PaymentAttemptReaderPort;
import com.personal.happygallery.domain.payment.PaymentAttempt;
import com.personal.happygallery.domain.payment.PaymentAttemptStatus;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
class PaymentAttemptExpiryProcessor {

    private final PaymentAttemptReaderPort attemptReader;
    private final OrderPaymentBenefitReservationService benefitReservationService;
    private final Clock clock;

    PaymentAttemptExpiryProcessor(PaymentAttemptReaderPort attemptReader,
                                  OrderPaymentBenefitReservationService benefitReservationService,
                                  Clock clock) {
        this.attemptReader = attemptReader;
        this.benefitReservationService = benefitReservationService;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    boolean expire(Long attemptId, LocalDateTime cutoff) {
        PaymentAttempt attempt = attemptReader.findByIdForUpdate(attemptId).orElse(null);
        if (attempt == null) {
            return false;
        }
        return expireIfDue(attempt, cutoff, LocalDateTime.now(clock));
    }

    /**
     * 잠근 PENDING 결제가 만료 경계를 지났으면 취소하고 ORDER 혜택 예약을 해제한다.
     * 호출자의 트랜잭션 안에서 실행하며, PENDING이 아니면 payload를 읽지 않고 false를 돌려준다.
     */
    boolean expireIfDue(PaymentAttempt attempt, LocalDateTime cutoff, LocalDateTime now) {
        if (attempt.getStatus() != PaymentAttemptStatus.PENDING) {
            return false;
        }
        return benefitReservationService.releaseAfter(
                attempt, () -> attempt.expirePendingBefore(cutoff), now);
    }
}
