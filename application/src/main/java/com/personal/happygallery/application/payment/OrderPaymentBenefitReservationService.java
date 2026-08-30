package com.personal.happygallery.application.payment;

import com.personal.happygallery.application.coupon.port.in.CouponRedemptionUseCase;
import com.personal.happygallery.application.customer.MemberAccountGuard;
import com.personal.happygallery.application.payment.context.PreparedPaymentPayload;
import com.personal.happygallery.application.payment.context.PreparedPaymentPayload.PreparedOrderPayload;
import com.personal.happygallery.application.reward.RewardBenefitService;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.order.OrderPricingSnapshot;
import com.personal.happygallery.domain.payment.PaymentAttempt;
import com.personal.happygallery.domain.payment.PaymentAttemptStatus;
import com.personal.happygallery.domain.payment.PaymentContext;
import java.time.LocalDateTime;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** ORDER 결제 시도와 쿠폰·적립금 예약 수명주기를 같은 트랜잭션으로 묶는다. */
@Service
class OrderPaymentBenefitReservationService {

    private final CouponRedemptionUseCase couponRedemptionUseCase;
    private final RewardBenefitService rewardBenefitService;
    private final MemberAccountGuard memberAccountGuard;
    private final PaymentConfirmAttemptResolver attemptResolver;

    OrderPaymentBenefitReservationService(
            CouponRedemptionUseCase couponRedemptionUseCase,
            RewardBenefitService rewardBenefitService,
            MemberAccountGuard memberAccountGuard,
            PaymentConfirmAttemptResolver attemptResolver) {
        this.couponRedemptionUseCase = couponRedemptionUseCase;
        this.rewardBenefitService = rewardBenefitService;
        this.memberAccountGuard = memberAccountGuard;
        this.attemptResolver = attemptResolver;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    void reserve(PaymentAttempt attempt,
                 PreparedPaymentPayload payload,
                 LocalDateTime now) {
        BenefitReservation reservation = reservation(attempt, payload);
        if (reservation == null) {
            return;
        }
        memberAccountGuard.requireActiveForUpdate(reservation.userId());
        couponRedemptionUseCase.reserve(
                reservation.pricing().issuedCouponId(), attempt.getId());
        rewardBenefitService.reserve(
                reservation.userId(),
                reservation.pricing().rewardUsedAmount(),
                attempt.getId(),
                now);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    void release(PaymentAttempt attempt,
                 PreparedPaymentPayload payload,
                 LocalDateTime now) {
        requireReleaseAllowed(attempt.getStatus());
        BenefitReservation reservation = reservation(attempt, payload);
        if (reservation == null) {
            return;
        }
        memberAccountGuard.requireActiveForUpdate(reservation.userId());
        couponRedemptionUseCase.release(
                reservation.pricing().issuedCouponId(), attempt.getId());
        rewardBenefitService.release(attempt.getId(), now);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    void release(PaymentAttempt attempt, LocalDateTime now) {
        PreparedPaymentPayload payload = readPayloadForRelease(attempt);
        if (payload != null) {
            release(attempt, payload, now);
        }
    }

    PreparedPaymentPayload readPayloadForRelease(PaymentAttempt attempt) {
        if (attempt.getContext() != PaymentContext.ORDER || attempt.getPayloadEnc() == null) {
            return null;
        }
        return attemptResolver.readPayload(attempt);
    }

    private static BenefitReservation reservation(
            PaymentAttempt attempt, PreparedPaymentPayload payload) {
        if (attempt.getContext() != PaymentContext.ORDER
                || !(payload instanceof PreparedOrderPayload orderPayload)
                || orderPayload.pricing() == null
                || (orderPayload.pricing().issuedCouponId() == null
                    && orderPayload.pricing().rewardUsedAmount() == 0L)) {
            return null;
        }
        if (orderPayload.userId() == null
                || !Objects.equals(orderPayload.userId(), attempt.getOwnerUserId())) {
            throw new HappyGalleryException(
                    ErrorCode.INVALID_INPUT, "결제 혜택의 회원 정보가 결제 시도와 일치하지 않습니다.");
        }
        return new BenefitReservation(orderPayload.userId(), orderPayload.pricing());
    }

    private static void requireReleaseAllowed(PaymentAttemptStatus status) {
        if (status != PaymentAttemptStatus.FAILED
                && status != PaymentAttemptStatus.COMPENSATED
                && status != PaymentAttemptStatus.CANCELED) {
            throw new HappyGalleryException(
                    ErrorCode.INVALID_INPUT, "종결된 결제에서만 혜택 예약을 해제할 수 있습니다.");
        }
    }

    private record BenefitReservation(Long userId, OrderPricingSnapshot pricing) {}
}
