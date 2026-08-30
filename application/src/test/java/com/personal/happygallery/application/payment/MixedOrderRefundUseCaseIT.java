package com.personal.happygallery.application.payment;

import com.personal.happygallery.adapter.out.persistence.booking.RefundRepository;
import com.personal.happygallery.adapter.out.persistence.coupon.CouponDefinitionRepository;
import com.personal.happygallery.adapter.out.persistence.coupon.IssuedCouponRepository;
import com.personal.happygallery.adapter.out.persistence.order.OrderRepository;
import com.personal.happygallery.adapter.out.persistence.payment.PaymentAttemptRepository;
import com.personal.happygallery.application.coupon.port.in.CouponRedemptionUseCase;
import com.personal.happygallery.application.customer.port.out.UserStorePort;
import com.personal.happygallery.application.reward.RewardBenefitService;
import com.personal.happygallery.application.reward.port.in.RewardQueryUseCase;
import com.personal.happygallery.application.payment.port.out.PaymentPort;
import com.personal.happygallery.application.payment.port.out.RefundResult;
import com.personal.happygallery.domain.coupon.CouponDefinition;
import com.personal.happygallery.domain.coupon.CouponDiscountType;
import com.personal.happygallery.domain.coupon.IssuedCoupon;
import com.personal.happygallery.domain.coupon.IssuedCouponStatus;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.order.OrderPricingSnapshot;
import com.personal.happygallery.domain.payment.PaymentAttempt;
import com.personal.happygallery.domain.payment.PaymentContext;
import com.personal.happygallery.domain.payment.RefundStatus;
import com.personal.happygallery.domain.reward.RewardLedgerType;
import com.personal.happygallery.domain.user.User;
import com.personal.happygallery.support.TestCleanupSupport;
import com.personal.happygallery.support.UseCaseIT;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.assertj.core.groups.Tuple.tuple;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@UseCaseIT
class MixedOrderRefundUseCaseIT {

    @Autowired RefundExecutionService refundExecutionService;
    @Autowired RefundDispatcher refundDispatcher;
    @Autowired RewardBenefitService rewardBenefitService;
    @Autowired RewardQueryUseCase rewardQueryUseCase;
    @Autowired CouponRedemptionUseCase couponRedemptionUseCase;
    @Autowired UserStorePort userStorePort;
    @Autowired OrderRepository orderRepository;
    @Autowired PaymentAttemptRepository paymentAttemptRepository;
    @Autowired CouponDefinitionRepository definitionRepository;
    @Autowired IssuedCouponRepository issuedCouponRepository;
    @Autowired RefundRepository refundRepository;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired TestCleanupSupport cleanupSupport;
    @Autowired Clock clock;
    @MockitoBean PaymentPort paymentProvider;

    @AfterEach
    void tearDown() {
        cleanupSupport.clearOrderData();
        cleanupSupport.clearUsers();
    }

    @DisplayName("PG와 적립금이 함께 결제된 회원 주문 환불은 PG 취소와 적립금 복원 및 회수를 한 번만 반영한다")
    @Test
    void mixedRefund_cancelsPgAndSettlesRewardIdempotently() {
        LocalDateTime now = LocalDateTime.now(clock);
        User user = userStorePort.save(new User(
                "mixed-refund@example.com", "password-hash", "혼합 환불 회원", "01077779999"));
        Order rewardSourceOrder = orderRepository.saveAndFlush(Order.forMember(
                user.getId(), 100_000L, now, now.plusHours(24)));
        rewardBenefitService.accrue(user.getId(), rewardSourceOrder.getId(), 5_000L, now);

        PaymentAttempt attempt = paymentAttemptRepository.saveAndFlush(
                PaymentAttempt.startForMember(
                        "mixed-refund-attempt", PaymentContext.ORDER, 15_000L, "{}", user.getId()));
        rewardBenefitService.reserve(user.getId(), 5_000L, attempt.getId(), now);

        OrderPricingSnapshot pricing = new OrderPricingSnapshot(
                20_000L, 0L, 0L, 5_000L, 15_000L, null);
        Order order = Order.forMember(user.getId(), pricing, now, now.plusHours(24), null);
        order.recordPaymentKey("mixed-order-payment-key");
        order = orderRepository.saveAndFlush(order);
        rewardBenefitService.consume(attempt.getId(), order.getId(), 5_000L, now);
        rewardBenefitService.accrue(user.getId(), order.getId(), 150L, now);

        when(paymentProvider.refund(any(), anyLong(), any()))
                .thenReturn(RefundResult.success("mixed-refund-transaction-key"));

        Long orderId = order.getId();
        new TransactionTemplate(transactionManager).executeWithoutResult(ignored ->
                refundExecutionService.requestOrderRefund(
                        orderId, 15_000L, 20_000L, 5_000L, 150L, false,
                        "mixed-order-payment-key"));

        await().atMost(Duration.ofSeconds(3)).untilAsserted(() -> {
            var refund = refundRepository.findDirectByOrderId(orderId).orElseThrow();
            var wallet = rewardQueryUseCase.getWallet(user.getId());
            var earned = rewardBenefitService.getEarnedSnapshot(orderId);
            assertSoftly(softly -> {
                softly.assertThat(refund.getStatus()).isEqualTo(RefundStatus.SUCCEEDED);
                softly.assertThat(refund.getAmount()).isEqualTo(15_000L);
                softly.assertThat(refund.getCustomerRefundAmount()).isEqualTo(20_000L);
                softly.assertThat(refund.getRewardRestoreAmount()).isEqualTo(5_000L);
                softly.assertThat(refund.getRewardRevokeAmount()).isEqualTo(150L);
                softly.assertThat(refund.getRefundTransactionKey())
                        .isEqualTo("mixed-refund-transaction-key");
                softly.assertThat(refund.getAttemptCount()).isEqualTo(1);
                softly.assertThat(wallet.availableBalance()).isEqualTo(5_000L);
                softly.assertThat(wallet.reservedBalance()).isZero();
                softly.assertThat(wallet.debtBalance()).isZero();
                softly.assertThat(earned.earnedAmount()).isEqualTo(150L);
                softly.assertThat(earned.revokedAmount()).isEqualTo(150L);
            });
        });

        var completedRefund = refundRepository.findDirectByOrderId(orderId).orElseThrow();
        verify(paymentProvider).refund(
                "mixed-order-payment-key",
                15_000L,
                completedRefund.getIdempotencyKey());

        refundDispatcher.dispatch(completedRefund.getId(), "completed mixed refund replay");

        var replayedRefund = refundRepository.findDirectByOrderId(orderId).orElseThrow();
        var walletAfterReplay = rewardQueryUseCase.getWallet(user.getId());
        assertSoftly(softly -> {
            softly.assertThat(replayedRefund.getStatus()).isEqualTo(RefundStatus.SUCCEEDED);
            softly.assertThat(replayedRefund.getRefundTransactionKey())
                    .isEqualTo("mixed-refund-transaction-key");
            softly.assertThat(replayedRefund.getAttemptCount()).isEqualTo(1);
            softly.assertThat(walletAfterReplay.availableBalance()).isEqualTo(5_000L);
            softly.assertThat(walletAfterReplay.history())
                    .filteredOn(history -> orderId.equals(history.orderId()))
                    .extracting(
                            RewardQueryUseCase.RewardHistory::type,
                            RewardQueryUseCase.RewardHistory::amount)
                    .containsExactlyInAnyOrder(
                            tuple(RewardLedgerType.USE, 5_000L),
                            tuple(RewardLedgerType.EARN, 150L),
                            tuple(RewardLedgerType.RESTORE, 5_000L),
                            tuple(RewardLedgerType.REVOKE, 150L));
        });
        verify(paymentProvider).refund(any(), anyLong(), any());
    }

    @DisplayName("PG 0원 회원 주문 환불은 외부 호출 없이 성공하고 적립금과 쿠폰을 복원한다")
    @Test
    void localOnlyRefund_restoresRewardAndCouponWithoutPgCall() {
        LocalDateTime now = LocalDateTime.now(clock);
        User user = userStorePort.save(new User(
                "local-refund@example.com", "password-hash", "로컬 환불 회원", "01077778888"));
        Order earningOrder = orderRepository.saveAndFlush(Order.forMember(
                user.getId(), 10_000L, now, now.plusHours(24)));
        rewardBenefitService.accrue(user.getId(), earningOrder.getId(), 5_000L, now);

        CouponDefinition definition = definitionRepository.saveAndFlush(new CouponDefinition(
                "로컬 환불 쿠폰",
                CouponDiscountType.FIXED,
                5_000L,
                0L,
                null,
                now.minusDays(1),
                now.plusDays(30),
                true,
                false));
        IssuedCoupon issuedCoupon = issuedCouponRepository.saveAndFlush(
                new IssuedCoupon(definition.getId(), user.getId(), now));
        PaymentAttempt attempt = paymentAttemptRepository.saveAndFlush(
                PaymentAttempt.startForMember(
                        "local-refund-attempt", PaymentContext.ORDER, 1L, "{}", user.getId()));
        couponRedemptionUseCase.reserve(issuedCoupon.getId(), attempt.getId());
        rewardBenefitService.reserve(user.getId(), 5_000L, attempt.getId(), now);

        OrderPricingSnapshot pricing = new OrderPricingSnapshot(
                10_000L, 0L, 5_000L, 5_000L, 0L, issuedCoupon.getId());
        Order order = orderRepository.saveAndFlush(Order.forMember(
                user.getId(), pricing, now, now.plusHours(24), null));
        couponRedemptionUseCase.redeem(issuedCoupon.getId(), attempt.getId(), order.getId());
        rewardBenefitService.consume(attempt.getId(), order.getId(), 5_000L, now);

        new TransactionTemplate(transactionManager).executeWithoutResult(ignored ->
                refundExecutionService.requestOrderRefund(
                        order.getId(), 0L, 5_000L, 5_000L, 0L, true, null));

        await().atMost(Duration.ofSeconds(3)).untilAsserted(() -> {
            var refund = refundRepository.findDirectByOrderId(order.getId()).orElseThrow();
            var coupon = issuedCouponRepository.findById(issuedCoupon.getId()).orElseThrow();
            var wallet = rewardQueryUseCase.getWallet(user.getId());
            assertSoftly(softly -> {
                softly.assertThat(refund.getStatus()).isEqualTo(RefundStatus.SUCCEEDED);
                softly.assertThat(refund.getAmount()).isZero();
                softly.assertThat(refund.getCustomerRefundAmount()).isEqualTo(5_000L);
                softly.assertThat(refund.getRefundTransactionKey()).isNull();
                softly.assertThat(coupon.getStatus()).isEqualTo(IssuedCouponStatus.AVAILABLE);
                softly.assertThat(wallet.availableBalance()).isEqualTo(5_000L);
            });
        });
        verify(paymentProvider, never()).refund(any(), anyLong(), any());
    }
}
