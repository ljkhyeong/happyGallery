package com.personal.happygallery.policy;

import com.personal.happygallery.common.error.AlreadyRefundedException;
import com.personal.happygallery.common.error.ProductionRefundNotAllowedException;
import com.personal.happygallery.domain.order.OrderStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * [PolicyTest] 주문 상태 전이 가드 검증.
 *
 * <p>이미 환불된 주문({@code REJECTED_REFUNDED}, {@code AUTO_REFUNDED_TIMEOUT},
 * {@code PICKUP_EXPIRED_REFUNDED})에 승인을 시도하면 {@link AlreadyRefundedException}이 발생한다.
 */
@Tag("policy")
class OrderStatusTransitionPolicyTest {

    @DisplayName("PAID_APPROVAL_PENDING 상태는 승인 가능 검증에서 예외가 발생하지 않는다")
    @Test
    void requireApprovable_whenPaidPending_noException() {
        assertThatCode(() -> OrderStatus.PAID_APPROVAL_PENDING.requireApprovable())
                .doesNotThrowAnyException();
    }

    @DisplayName("AUTO_REFUNDED_TIMEOUT 상태는 승인 가능 검증에서 예외가 발생한다")
    @Test
    void requireApprovable_whenAutoRefunded_throws() {
        assertThatThrownBy(() -> OrderStatus.AUTO_REFUNDED_TIMEOUT.requireApprovable())
                .isInstanceOf(AlreadyRefundedException.class);
    }

    @DisplayName("REJECTED_REFUNDED 상태는 승인 가능 검증에서 예외가 발생한다")
    @Test
    void requireApprovable_whenRejectedRefunded_throws() {
        assertThatThrownBy(() -> OrderStatus.REJECTED_REFUNDED.requireApprovable())
                .isInstanceOf(AlreadyRefundedException.class);
    }

    @DisplayName("PICKUP_EXPIRED_REFUNDED 상태는 승인 가능 검증에서 예외가 발생한다")
    @Test
    void requireApprovable_whenPickupExpiredRefunded_throws() {
        assertThatThrownBy(() -> OrderStatus.PICKUP_EXPIRED_REFUNDED.requireApprovable())
                .isInstanceOf(AlreadyRefundedException.class);
    }

    // -----------------------------------------------------------------------
    // requireCancellable() — 제작 중 취소 불가 (§8.3)
    // -----------------------------------------------------------------------

    @DisplayName("IN_PRODUCTION 상태는 취소 가능 검증에서 예외가 발생한다")
    @Test
    void requireCancellable_whenInProduction_throws() {
        assertThatThrownBy(() -> OrderStatus.IN_PRODUCTION.requireCancellable())
                .isInstanceOf(ProductionRefundNotAllowedException.class);
    }

    @DisplayName("DELAY_REQUESTED 상태는 취소 가능 검증에서 예외가 발생한다")
    @Test
    void requireCancellable_whenDelayRequested_throws() {
        assertThatThrownBy(() -> OrderStatus.DELAY_REQUESTED.requireCancellable())
                .isInstanceOf(ProductionRefundNotAllowedException.class);
    }

    @DisplayName("PAID_APPROVAL_PENDING 상태는 취소 가능 검증에서 예외가 발생하지 않는다")
    @Test
    void requireCancellable_whenPaidPending_noException() {
        assertThatCode(() -> OrderStatus.PAID_APPROVAL_PENDING.requireCancellable())
                .doesNotThrowAnyException();
    }

    @DisplayName("APPROVED_FULFILLMENT_PENDING 상태는 취소 가능 검증에서 예외가 발생하지 않는다")
    @Test
    void requireCancellable_whenApprovedFulfillmentPending_noException() {
        assertThatCode(() -> OrderStatus.APPROVED_FULFILLMENT_PENDING.requireCancellable())
                .doesNotThrowAnyException();
    }
}
